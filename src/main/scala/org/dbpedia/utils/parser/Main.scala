package org.dbpedia.utils.parser

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

import java.io._

object Main {

  def main(args: Array[String]): Unit = {

    val inputPath = "ntfiles/test.nt.bz2"
    val outputTriplesPath = "output-triples.nt"
    val outputReportPath = "output-report.txt"

    try {
      println(s"Parsing RDF data from $inputPath...")

      org.dbpedia.utils.parser.NTParser.parse(
        inputPath,
        outputTriplesPath,
        outputReportPath
      )

      println(s"Parsing complete. Results written to:")
      println(s"  Triples: $outputTriplesPath")
      println(s"  Report:  $outputReportPath")

    } catch {
      case ex: Exception =>
        System.err.println("Error while parsing RDF-Data:")
        ex.printStackTrace()
    }
  }
}
