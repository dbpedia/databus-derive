package org.dbpedia.utils.parser

import java.io._
import java.math.BigInteger
import java.net.URLEncoder
import java.security.MessageDigest
import net.sansa_stack.rdf.benchmark.io.ReadableByteChannelFromIterator
import net.sansa_stack.rdf.common.io.riot.tokens.TokenizerTextForgiving
import org.apache.jena.atlas.io.PeekReader
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.{Node, NodeFactory, Triple}
import org.apache.jena.irix.IRIxResolver
import org.apache.jena.riot.system._
import org.apache.jena.riot.{RDFDataMgr, RIOT}
import org.dbpedia.databus.derive.io.rdf.LangNTriplesSkipBad

import java.nio.charset.StandardCharsets
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

object NTParser {

  protected val ByteInputBufferSize: Int = 32 * 1024 //64 * 1024

  def parse(
             tripleInput: InputStream,
             tripleOutput: OutputStream,
             reportOutput: OutputStream,
             chunk: Int = 10000,
             reportFormat: ReportFormat.Value = ReportFormat.TEXT,
             removeWarnings: Boolean = false
           ): Unit = {

    val reader = new BufferedReader(new InputStreamReader(tripleInput, StandardCharsets.UTF_8))
    val linesBuffer = new scala.collection.mutable.ArrayBuffer[String](chunk)
    var line: String = null

    while ({ line = reader.readLine(); line != null }) {
      linesBuffer.append(line)
      if (linesBuffer.size >= chunk) {
        processChunk(linesBuffer, tripleOutput, reportOutput, reportFormat, removeWarnings)
        linesBuffer.clear()
      }
    }

    if (linesBuffer.nonEmpty) {
      processChunk(linesBuffer, tripleOutput, reportOutput, reportFormat, removeWarnings)
      linesBuffer.clear()
    }

    reader.close()
    tripleOutput.flush()
    reportOutput.flush()

  }


  private def processChunk(
                    lines: Seq[String],
                    tripleOutput: OutputStream,
                    reportOutput: OutputStream,
                    reportFormat: ReportFormat.Value,
                    removeWarnings: Boolean): Unit = {

    val errorHandler = reportFormat match {
      case ReportFormat.TEXT =>
        new BufferedTextReportsEH(lines.toArray, ListBuffer[String](), mutable.HashSet[Long]())
      case ReportFormat.RDF =>
        new BufferedRDFReportsEH(lines.toArray, ListBuffer[Triple](), mutable.HashSet[Long]())
    }

    val parserProfile = new ParserProfileStd(
      RiotLib.factoryRDF,
      errorHandler,
      IRIxResolver.create("http://example.org/").build(),
      PrefixMapFactory.create,
      RIOT.getContext.copy,
      true,
      true
    )

    val tokenizer = new TokenizerTextForgiving(
      PeekReader.makeUTF8(
        ReadableByteChannelFromIterator.toInputStream(lines.iterator.asJava)
      )
    )

    tokenizer.setErrorHandler(errorHandler)

    val jenaTriples = new LangNTriplesSkipBad(tokenizer, parserProfile, null).filter { wrappedTriple =>
      !removeWarnings || !errorHandler.getViolatedRowsBuffer.contains(wrappedTriple.getRow)
    }


    val tripleOS = new ByteArrayOutputStream()
    RDFDataMgr.writeTriples(tripleOS, jenaTriples)
    tripleOutput.write(tripleOS.toByteArray)

    errorHandler match {
      case textReports: BufferedTextReportsEH =>
        if (textReports.getReportBuffer.nonEmpty) {
          val reportBytes = textReports.getReportBuffer.mkString("", "\n", "\n").getBytes(StandardCharsets.UTF_8)
          reportOutput.write(reportBytes)
        }

      case rdfReports: BufferedRDFReportsEH =>
        if (rdfReports.getReportBuffer.nonEmpty) {
          val reportOS = new ByteArrayOutputStream()
          RDFDataMgr.writeTriples(reportOS, rdfReports.getReportBuffer.toIterator.asJava)
          reportOutput.write(reportOS.toByteArray)
        }
    }

  }

}

sealed trait CPR
case class TripleBytes(bytes: Array[Byte]) extends CPR
case class ReportBytes(bytes: Array[Byte]) extends CPR

object ReportFormat extends Enumeration {
  val TEXT,RDF = Value
}

case class RowNr(nr: Long)

abstract class BufferedErrorHandler[T,T2]( reportBuffer: mutable.Iterable[T],
                                           violatedRowsBuffer: mutable.Iterable[T2] ) {

  def getReportBuffer: mutable.Iterable[T] = reportBuffer
  def getViolatedRowsBuffer: mutable.Iterable[T2] = violatedRowsBuffer
}

class BufferedTextReportsEH( rawLines: Array[String],
                             reportBuffer: ListBuffer[String],
                             violatedRowsBuffer: mutable.HashSet[Long] )

  extends BufferedErrorHandler[String,Long]( reportBuffer, violatedRowsBuffer ) with ErrorHandler{

  override def warning(message: String, line: Long, col: Long): Unit = {
    violatedRowsBuffer.add(line)
    reportBuffer.append(s"${rawLines(line.toInt-1)} # WRN@$col $message")
  }

  override def error(message: String, line: Long, col: Long): Unit = {
    val safeLine = rawLines.lift((line - 1).toInt).getOrElse(s"[Line $line not available]")
    reportBuffer.append(s"$safeLine # ERR@$col $message")
  }


  override def fatal(message: String, line: Long, col: Long): Unit = {
    reportBuffer.append(s"${rawLines(line.toInt-1)} # FTL@$col $message")
  }
}

class BufferedRDFReportsEH( rawLines: Array[String],
                            reportBuffer: ListBuffer[Triple],
                            violatedRowsBuffer: mutable.HashSet[Long] )

  extends BufferedErrorHandler[Triple,Long]( reportBuffer, violatedRowsBuffer ) with ErrorHandler{

  def base: String = "http://dbpedia.org/debug/"
  def rIri: Regex = """<.*>""".r
  def rLit: Regex = """\".*\"""".r

  override def warning(message: String, line: Long, col: Long): Unit = {
    violatedRowsBuffer.add(line)
    construct("e",message,line,col)
  }

  override def error(message: String, line: Long, col: Long): Unit = {
    construct("e",message,line,col)
  }

  override def fatal(message: String, line: Long, col: Long): Unit = {
    construct("e",message,line,col)
  }

  def construct(level: String, message: String, line: Long, col: Long): Unit = {

    val lIdx = line.toInt-1
    val resource = NodeFactory.createURI(s"${base}entity/${sha256FromString(rawLines(lIdx))}")

    appendReportBuffer(
      resource,NodeFactory.createURI(s"${base}vocab/raw"),
      NodeFactory.createLiteral(rawLines(lIdx)))

    appendReportBuffer(
      resource,NodeFactory.createURI(s"${base}vocab/pos"),
      NodeFactory.createLiteral(col.toString,XSDDatatype.XSDnonNegativeInteger))

    val m = rLit.replaceAllIn(rIri.replaceAllIn(message.split(" ",2)(1),""),"")

    appendReportBuffer(
      resource,NodeFactory.createURI(s"${base}vocab/code"),
      NodeFactory.createURI(s"${base}code/${URLEncoder.encode(m,"UTF-8")}"))
  }

  def appendReportBuffer(s: Node,p: Node,o: Node): Unit = {
    reportBuffer.append(new org.apache.jena.graph.Triple(s,p,o))
  }

  def sha256FromString(string: String): String = {
    String.format(
      "%032x", new BigInteger(
        1, MessageDigest.getInstance("SHA-256").
          digest(string.getBytes("UTF-8"))))
  }
}