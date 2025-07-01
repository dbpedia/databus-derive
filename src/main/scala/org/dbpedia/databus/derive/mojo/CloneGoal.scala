package org.dbpedia.databus.derive.mojo

import java.{io, util}
import better.files
import better.files.File
import org.apache.commons.io.FileUtils
import org.apache.jena.iri.IRIFactory
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations._
import org.dbpedia.databus.derive.cli.NTripleParserCLI.parseFile
import org.dbpedia.databus.derive.download.DatabusDownloader
import org.dbpedia.databus.derive.io.findFilePathsInDirectory
import org.dbpedia.databus.derive.io.rdf.ReportFormat

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
import scala.language.postfixOps
import scala.sys.process
import scala.sys.process.Process

/** @author Marvin Hofer
  *
  *         mvn databus-derive:clone-parse
  *
  *         MAVEN Goal: retrieve and parse DBpedia databus dataset's by version
  */
@Mojo(name = "clone", threadSafe = true)
class CloneGoal extends AbstractMojo {

  private val endpoint: String = "https://databus.dbpedia.org/repo/sparql"

  @Parameter(defaultValue = "${session.executionRootDirectory}", readonly = true)
  val sessionRoot: java.io.File = null

  @Parameter(defaultValue = "${project.basedir} ", readonly = true)
  val baseDirectory: java.io.File = null

  @Parameter(defaultValue = "${project.groupId}", readonly = true)
  val groupId: String = null

  @Parameter(defaultValue = "${project.artifactId}", readonly = true)
  val artifactId: String = null

  @Parameter(defaultValue = "${project.version}", readonly = true)
  val version: String = null

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  val buildDirectory: java.io.File = null

  /**
    * List of dataset version IRIs
    *
    * pom.xml > build > plugins > plugin > configuration > versions > version*
    */
  @Parameter
  val versions: util.ArrayList[String] = new util.ArrayList[String]

  @Parameter(
    property = "databus.derive.downloadDirectory",
    defaultValue = "${project.build.directory}/databus/derive/downloads"
  )
  val downloadDirectory: java.io.File = null

  @Parameter(
    property = "databus.derive.reportDirectory",
    defaultValue = "${project.build.directory}/databus/derive/reports"
  )
  val reportDirectory: java.io.File = null


  @Parameter(
    property = "databus.derive.packageDirectory",
    defaultValue = "${project.basedir}"
  )
  val packageDirectory: java.io.File = null

  /**
    * derived variables
    */
  lazy val downloadDirectoryBF: File = File(downloadDirectory.getAbsolutePath)
  lazy val finalBuildDirectory = new io.File(buildDirectory, "databus/derive/final")
  lazy val finalBuildDirectoryBF: File = File(finalBuildDirectory.getAbsolutePath)
  lazy val reportDirectoryBF: File = File(reportDirectory.getAbsolutePath)


  @Parameter(
    property = "databus.derive.skipCloning",
    defaultValue = "true"
  )
  val skipCloning: Boolean = true


  @Parameter(
    property = "databus.derive.skipParsing",
    defaultValue = "true"
  )
  val skipParsing: Boolean = true

  @Parameter(
    property = "databus.derive.deleteDownloadCache",
    defaultValue = "false"
  )
  val deleteDownloadCache: Boolean = false

  @Parameter(
    property = "databus.derive.deleteDownloadCache",
    defaultValue = "${project.build.directory}/databus/derive/.spark_local_dir"
  )
  val sparkLocalDir: java.io.File = null

  @Parameter(
    property = "databus.derive.removeFoundWarnings",
    defaultValue = "true"
  )
  val removeWarnings: Boolean = true

  @Parameter(
    property = "databus.derive.parFiles",
    defaultValue = "4"
  )
  val parFiles: Int = 4

  @Parameter(
    property = "databus.derive.parChunks",
    defaultValue = "8"
  )
  val parChunks: Int = 8

  @Parameter(
    property = "databus.derive.chunkSize",
    defaultValue = "10000"
  )
  val chunkSize: Int = 50000

  override def execute(): Unit = {

    if (artifactId == "group-metadata") {

      versions.asScala.foreach(versionStr => {

        System.err.println(s"[INFO] Looking for version: $versionStr")
        val versionIRI = IRIFactory.iriImplementation().construct(versionStr)

        DatabusDownloader.cloneVersionToDirectory(
          version = versionIRI,
          directory = files.File(downloadDirectory.getAbsolutePath),
          skipFilesIfExists = true,
          endpoint
        )

        //        PomUtils.copyAllAndChangeGroup(downloadDirectory, finalBuildDirectory, groupId, version)
      })


      if (skipParsing) {
        FileUtils.copyDirectory(downloadDirectory, finalBuildDirectory)
      }
      else {
        parseDownloadsToReportAndFinal()
      }

      //      collectReports(reportDirectory, finalBuildDirectory)

      //      compression is now done for each file
      //      compressOutputWithBash(finalBuildDirectory)

      if(!skipCloning) {
        FileUtils.copyDirectory(finalBuildDirectory, packageDirectory)
      }
    }
  }

  def collectReports(sourceDirectory: io.File, targetDirectory: io.File): Unit = {

    sourceDirectory.listFiles().foreach(

      artifact => {

        artifact.listFiles().foreach(

          version => {

            val newArtifact = new io.File(targetDirectory, s"${artifact.getName}/${version.getName}")

            val cmd = {
              Seq(
                "bash",
                "-c",
                s"cat $$(find ${version.getAbsolutePath} -name '*_debug.txt.bz2') " +
                  s"> ${newArtifact.getAbsolutePath}/${artifact.getName}_debug.txt.bz2 ")
            }

            System.err.println(s"[INFO] ${cmd.mkString(" ")}")

            Process(cmd).!
          }
        )
      }
    )

  }


  def compressOutputWithBashDeprecated(directory: io.File): Int = {

    val cmd = Seq("bash", "-c", s"lbzip2 $$(find ${directory.getAbsolutePath} -regextype posix-egrep -regex '.*\\.(ttl|nt)')")

    System.err.println(s"[INFO] ${cmd.mkString(" ")}")

    Process(cmd).!
  }

  /**
    * works on class variables
    */
  def parseDownloadsToReportAndFinal(): Unit = {

    if (!downloadDirectoryBF.isDirectory) {
      System.err.println(s"[ERROR] $downloadDirectoryBF is no directory")
    }
    else {

      /*
      reportFormat, removeWarnings
       */

      val rFilter = """(.*\.nt.*)|(.*\.ttl.*)""".r

      val pool = findFilePathsInDirectory(downloadDirectoryBF.toJava, Array[String]("*/*/*")).par
      pool.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(parFiles))

      pool.foreach(subPath => {

        val artifact = subPath.split("/")(0)
        val version = subPath.split("/")(1)

        val downFile = downloadDirectoryBF / subPath
        val finalFile = finalBuildDirectoryBF / artifact / version /
          s"${downFile.nameWithoutExtension}.ttl"

        finalFile.parent.createDirectoryIfNotExists()

        val reportFile = reportDirectoryBF / artifact / version /
          s"${downFile.nameWithoutExtension}_debug.txt"

        reportFile.parent.createDirectoryIfNotExists()

        if (rFilter.pattern.matcher(downFile.name).matches) {

          System.err.println(s"[INFO] Parsing ${downFile.name}")
          val finalFileOutputStream : java.io.OutputStream = finalFile.newOutputStream
          val reportFileOutputStream : java.io.OutputStream  =  reportFile.newOutputStream

          parseFile(
            downFile,
            finalFileOutputStream,
            reportFileOutputStream,
            parChunks,
            chunkSize,
            ReportFormat.TEXT,
            removeWarnings = true
          )

          finalFileOutputStream.close()
          reportFileOutputStream.close()

          lbzip2File(finalFile)
          lbzip2File(reportFile)

        }
        else {

          System.err.println(s"[INFO] Skip parsing ${downFile.name}")
          FileUtils.moveFile(downFile.toJava, finalFile.toJava)
        }
      })
    }
  }

  /**
    * compresses file to .bz2
    * removes original
    *
    * @param file uncompressed File
    */
  def lbzip2File(file: File): Unit = {

    val sortMemory = "20%"
    val sortParallel = "8"

    val cmd: Seq[String] = Seq(
      "bash",
      "-c",
      s"LC_ALL=C sort -S $sortMemory -u --parallel=$sortParallel ${file.pathAsString} " +
        s"| lbzip2 > ${file.pathAsString}.bz2 " +
        s"&& rm ${file.pathAsString}"
    )

    System.err.println(s"[INFO] ${cmd.mkString(" ")}")

    val process = Process(cmd).run()
    val exitValue = process.exitValue()
    process.destroy()
   

  }

  val pluginVersion = "1.3-SNAPSHOT"

  var logoPrinted = false

  //NOTE: NEEDS TO BE COMPATIBLE WITH TURTLE COMMENTS
  val logo : String =
    s"""|
        |
        |######⎄s
        |#     #   ##   #####   ##   #####  #    #  ####
        |#     #  #  #    #    #  #  #    # #    # #
        |#     # #    #   #   #    # #####  #    #  ####
        |#     # ######   #   ###### #    # #    #      #
        |#     # #    #   #   #    # #    # #    # #    #
        |######  #    #   #   #    # #####   ####   ####
        |
        |# Plugin version $pluginVersion - https://github.com/dbpedia/databus-maven-plugin
        |
        |""".stripMargin

  //  def printLogoOnce(mavenlog: Log) = {
  //    if (!logoPrinted) {
  //      mavenlog.info(logo)
  //    }
  //    logoPrinted = true
  //  }
}
