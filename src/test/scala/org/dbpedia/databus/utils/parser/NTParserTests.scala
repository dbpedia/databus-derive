package org.dbpedia.databus.utils.parser

import better.files.File
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.dbpedia.utils.parser.{NTParser, ReportFormat}

import java.io._
import java.nio.charset.StandardCharsets
import org.scalatest.FunSuite

class NTParserTests extends FunSuite {

  val valid: String =
    """
      |<http://a> <http://b> "1" .
      |<http://a> <http://b> "2" .
      |<http://a> <http://b> "3" .
      |<http://a> <http://b> "4" .
      |<http://a> <http://b> "5" .
      |""".stripMargin

  val warn: String =
    """
      |<http://a> <http://b> "1" .
      |<http://a> <http://b> "2" .
      |<http://a> <http://b Space> "3" .
      |<http://a> <http://b> "4" .
      |<http://a> <http://b> "5" .
      |""".stripMargin

  val error: String =
    """
      |<http://a> <http://b> "1" .
      |<http://a> <http://b> .
      |INVALID LINE
      |<http://a> <http://b> "5" .
      |<http://a> <http://b> "unterminated
      |""".stripMargin

  val errorExp: String =
    """
      |<http://a> <http://b> "1" .
      |<http://a> <http://b> "5" .
      |""".stripMargin


  def captureOutput[T](block: (ByteArrayOutputStream, ByteArrayOutputStream) => T): (T, String, String) = {
    val tripleOut = new ByteArrayOutputStream()
    val reportOut = new ByteArrayOutputStream()

    val result = block(tripleOut, reportOut)
    (
      result,
      tripleOut.toString(StandardCharsets.UTF_8.name()),
      reportOut.toString(StandardCharsets.UTF_8.name())
    )

  }

  test("parse 5 valid N-Triples with no issues") {
    val in = new ByteArrayInputStream(valid.getBytes(StandardCharsets.UTF_8))
    val (res, triples, report) = captureOutput { (tripleOut, reportOut) =>
      NTParser.parse(
        tripleInput = in,
        tripleOutput = tripleOut,
        reportOutput = reportOut,
        reportFormat = ReportFormat.TEXT,
        removeWarnings = false
      )
    }

    println("=== Triples ===")
    println(triples)

    println("=== Report ===")
    println(report)


    assertResult(triples.trim){
      valid.trim
    }
    assert(report.trim.isEmpty)
  }

  test("parse 5 N-Triples with warnings") {
    val in = new ByteArrayInputStream(warn.getBytes(StandardCharsets.UTF_8))
    val (_, triples, report) = captureOutput { (tripleOut, reportOut) =>
      NTParser.parse(
        tripleInput = in,
        tripleOutput = tripleOut,
        reportOutput = reportOut,
        reportFormat = ReportFormat.TEXT,
        removeWarnings = false
      )
    }

    println("=== Triples ===")
    println(triples)

    println("=== Report ===")
    println(report)

    assertResult(triples.trim){
      warn.trim
    }
    assert(report.contains("WRN"))
  }

  test("parse 5 N-Triples with errors") {
    val in = new ByteArrayInputStream(error.getBytes(StandardCharsets.UTF_8))
    val (_, triples, report) = captureOutput { (tripleOut, reportOut) =>
      NTParser.parse(
        tripleInput = in,
        tripleOutput = tripleOut,
        reportOutput = reportOut,
        reportFormat = ReportFormat.TEXT,
        removeWarnings = false
      )
    }

    println("=== Triples ===")
    println(triples)

    println("=== Report ===")
    println(report)

    assertResult(triples.trim){
      errorExp.trim
    }
    assert(report.contains("ERR"))
  }
}
