package org.dbpedia.databus.derive.download

import java.net.URL
import better.files.File
import org.apache.jena.iri.IRI
import org.apache.jena.query.{Query, QueryExecutionFactory, QueryFactory}
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP
import org.apache.spark.sql.execution.QueryExecution
import org.dbpedia.databus.sparql.DataidQueries

object DatabusDownloader {

  private val endpoint: String = "https://databus.dbpedia.org/repo/sparql"

  def cloneVersionToDirectory(version: IRI, directory: File,
                              skipFilesIfExists: Boolean = false, endpoint: String = endpoint): Unit = {

    val versionParts = version.toString.split("/")
    val artifact = versionParts(versionParts.length - 2)
    val hasVersion = versionParts.last

    System.err.println(s"[INFO] download $version")
    val query: Query = QueryFactory.create(DataidQueries.queryVersionDownloadUrls(version.toString))
    val queryExec = QueryExecutionHTTP.service(endpoint, query)
    val resultSet = queryExec.execSelect()
    val querySolution = resultSet.next()

    val dataidUrl = querySolution.getResource("dataset").getURI

    //println(s"$version -> ${directory/artifact}/$hasVersion")

    /**
      * Download original pom
      */
    //todo 
    /*FileDownloader.downloadUrlToDirectory(
      url = new URL(dataidUrl.split("/").dropRight(2).mkString("/") + "/pom.xml"),
      directory = directory / artifact,
      createDirectory = true,
      skipIfExists = skipFilesIfExists
    )*/

    /**
      * Download distributions
      */
    val artifactVersionDir = directory / s"$artifact/$hasVersion"

    FileDownloader.downloadUrlToDirectory(
      url = new URL(querySolution.getResource("downloadUrl").getURI),
      directory = artifactVersionDir,
      createDirectory = true,
      skipIfExists = skipFilesIfExists
    )
    while (resultSet.hasNext) {
      FileDownloader.downloadUrlToDirectory(
        url = new URL(resultSet.next().getResource("downloadUrl").getURI),
        directory = artifactVersionDir,
        skipIfExists = skipFilesIfExists
      )
    }
    //TODO maybe use RDFConnection for auto-close in lambda
    queryExec.close()
  }
}
//    val query =
//      """
//        |PREFIX dct:    <http://purl.org/dc/terms/>
//        |PREFIX dcat:   <http://www.w3.org/ns/dcat#>
//        |PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>
//        |
//        |SELECT ?dataset ?artifact ?hasVersion (GROUP_CONCAT(DISTINCT ?downloadUrl; SEPARATOR=";") AS ?downloadUrls) {
//        |
//        |  ?dataset a dataid:Dataset;
//        |           dataid:version <https://databus.dbpedia.org/dbpedia/databus/databus-data/2019.07.21> ;
//        |           dct:hasVersion ?hasVersion ;
//        |           dataid:artifact ?artifact ;
//        |           dcat:distribution/dcat:downloadURL ?downloadUrl .
//        |
//        |} GROUP BY ?dataset ?artifact ?hasVersion
//      """.stripMargin
