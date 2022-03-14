package org.dbpedia.databus.derive.io.compress

import java.io.{BufferedInputStream, File, FileInputStream, FileOutputStream}
import java.net.URL

import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.language.postfixOps
import scala.sys.process._

class DecompressTestSuite extends FunSuite with BeforeAndAfterAll {

  val downloadURL =
    new URL("http://dbpedia-generic.tib.eu/release/generic/geo-coordinates/2019.08.01/geo-coordinates_lang=en.ttl.bz2")

  val testFile: File = new File("decompress-test.bz2")
  val testSink: File = new File("decompress-test")

  override def beforeAll(): Unit = {

    downloadURL #> testFile !

    val inputStream = {
      CompressIO.decompressStreamAuto(
        new BufferedInputStream(
          new FileInputStream(
            testFile
          )
        )
      )
    }

    val outputStream = new FileOutputStream(testSink)

    IOUtils.copy(inputStream, outputStream)

    inputStream.close()
    outputStream.close()
  }

  test("bzip2 decompression byte size") {

    assert(testSink.length() > testFile.length(), "Decompressed file should be greater then the compressed file")

  }

  test("bzip2 decompression line count") {

    val linesIn = Process(Seq("bash", "-c", s"bzcat ${testFile.getAbsolutePath} | wc -l")).!!
    val linesOut = Process(Seq("bash", "-c", s"cat ${testSink.getAbsolutePath} | wc -l")).!!

    assert(linesOut == linesIn, "Decompressed file should have same line count as the compressed file")
  }

  override def afterAll() {
    testFile.delete()
    testSink.delete()
  }
}