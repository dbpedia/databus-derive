package org.dbpedia.databus.utils.parser

import org.dbpedia.utils.parser.NTParser

import java.nio.charset.StandardCharsets
import org.scalatest.FunSuite

import java.nio.file.{Files, Path}

class NTParserTests extends FunSuite {

  val valid: String =
    """<http://a> <http://b> "1" .
      |<http://a> <http://b> "2" .
      |<http://a> <http://b> "3" .
      |<http://a> <http://b> "4" .
      |<http://a> <http://b> "5" .""".stripMargin

  val tempValidFile: Path = Files.createTempFile("nt_valid", ".nt")
  Files.write(tempValidFile, valid.getBytes(StandardCharsets.UTF_8))

  val warn: String =
    """<http://a> <http://b> "1" .
      |<http://a> <http://b> "2" .
      |<http://a> <http://b Space> "3" .
      |<http://a> <http://b> "4" .
      |<http://a> <http://b> "5" .""".stripMargin

  val tempWarnFile: Path = Files.createTempFile("nt_warn", ".nt")
  Files.write(tempWarnFile, warn.getBytes(StandardCharsets.UTF_8))

  val error: String =
    """<http://a> <http://b> "1" .
      |<http://a> <http://b> .
      |INVALID LINE
      |<http://a> <http://b> "5" .
      |<http://a> <http://b> "unterminated""".stripMargin

  val tempErrorFile: Path = Files.createTempFile("nt_error", ".nt")
  Files.write(tempErrorFile, error.getBytes(StandardCharsets.UTF_8))

  val errorExp: String =
    """<http://a> <http://b> "1" .
      |<http://a> <http://b> "5" .""".stripMargin


  test("parse 5 valid N-Triples with no issues") {
    val tempOutputFile: Path = Files.createTempFile("triples_out", ".nt")
    val tempReportFile: Path = Files.createTempFile("report_out", ".txt")
    NTParser.parse(
      tempValidFile.toString,
      tempOutputFile.toString,
      tempReportFile.toString,
      reportFormat = "TEXT",
      removeWarnings = false
    )
    val triples = new String(Files.readAllBytes(tempOutputFile), StandardCharsets.UTF_8)
    val report = new String(Files.readAllBytes(tempReportFile), StandardCharsets.UTF_8)

    println("=== Triples ===")
    println(triples)

    println("=== Report ===")
    println(report)


    assertResult(valid.trim){
      triples.trim
    }
    assert(report.trim.isEmpty)
  }

  test("parse 5 N-Triples with warnings") {
    val tempOutputFile: Path = Files.createTempFile("triples_out", ".nt")
    val tempReportFile: Path = Files.createTempFile("report_out", ".txt")
    NTParser.parse(
      tempWarnFile.toString,
      tempOutputFile.toString,
      tempReportFile.toString,
      reportFormat = "TEXT",
      removeWarnings = false
    )
    val triples = new String(Files.readAllBytes(tempOutputFile), StandardCharsets.UTF_8)
    val report = new String(Files.readAllBytes(tempReportFile), StandardCharsets.UTF_8)

    println("=== Triples ===")
    println(triples)

    println("=== Report ===")
    println(report)


    assert(triples.lines.count==warn.lines.count)
    assert(report.contains("WRN"))
  }

  test("parse 5 N-Triples with errors") {
    val tempOutputFile: Path = Files.createTempFile("triples_out", ".nt")
    val tempReportFile: Path = Files.createTempFile("report_out", ".txt")
    NTParser.parse(
      tempErrorFile.toString,
      tempOutputFile.toString,
      tempReportFile.toString,
      reportFormat = "TEXT",
      removeWarnings = false
    )
    val triples = new String(Files.readAllBytes(tempOutputFile), StandardCharsets.UTF_8)
    val report = new String(Files.readAllBytes(tempReportFile), StandardCharsets.UTF_8)

    println("=== Triples ===")
    println(triples)

    println("=== Report ===")
    println(report)

    assertResult(errorExp.trim){
      triples.trim
    }
    assert(report.contains("ERR"))
  }
}
