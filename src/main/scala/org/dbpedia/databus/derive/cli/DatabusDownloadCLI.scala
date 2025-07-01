package org.dbpedia.databus.derive.cli

import better.files.File
import org.apache.jena.iri.IRIFactory
import org.dbpedia.databus.derive.download.DatabusDownloader
import scopt.{OptionParser, Read}

import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool

object DatabusDownloadCLI {

  case class DatabusDownloadConfig(versions: Seq[String] = null, downloadDirectory: File= null,
                                   parDownlaods: Int = 1, skipIfExists: Boolean = false)

  implicit def betterFileRead: Read[File] = Read.reads(File(_))

  def main(args: Array[String]): Unit = {

    val optionParser: OptionParser[DatabusDownloadConfig] = {

      new OptionParser[DatabusDownloadConfig]("databusDownloader") {

        head("Databus Version Downlaoder", "0.1")

        arg[Seq[String]]("<dataid:version1>,...").required().action((v, p) => p.copy(versions = v))
          .text("comma-separated dataid:version IRIs")

        opt[File]('o', "output").maxOccurs(1).action((o, p) =>  p.copy(downloadDirectory = o))
          .text("repository directory to download files")

        opt[Int]('p', "parDownoads").maxOccurs(1).action((x, p) =>  p.copy(parDownlaods = x))
          .text("parallel versions to process simultaneously")

        opt[Unit]( "skipIfExists").maxOccurs(1).action((x, p) =>  p.copy(skipIfExists = true))
          .text("skip files if already exist")
      }
    }

    optionParser.parse(args,DatabusDownloadConfig()) match {

      case Some(config) =>

        println("--------------------------")
        println(" DatabusVersionDownloader ")
        println("--------------------------")


        val pool = config.versions.par

        pool.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(config.parDownlaods))

        pool.foreach( version =>

          DatabusDownloader.cloneVersionToDirectory(
            version = IRIFactory.iriImplementation().construct(version),
            directory = config.downloadDirectory,
            skipFilesIfExists = config.skipIfExists
          )
        )

      case _ => optionParser.showTryHelp()
    }
  }
}
