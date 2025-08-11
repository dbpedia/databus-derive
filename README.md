# Databus-derive-maven-plugin

Extension to the [databus-maven-plugin](https://github.com/dbpedia/databus-maven-plugin), 
used to download and derive new datasets already released on the [databus](https://databus.dbpedia.org).
`mvn databus-derive:clone` downloads all data into the build directory `target/databus/derive/downloads`, `mvn clean` deletes the downloaded data again.
Later the plugin will feature better data housekeeping option, such as persisting some downloads in `${user.home}/.m2/databus` and caching often used conversions. 


**working beta**
* we are using it to derive the monthly [DBpedia releases](http://dev.dbpedia.org/Download_DBpedia) from the MARVIN pre-releases (parse and remove syntax errors)
* Code overlaps with the [Databus Client](https://github.com/dbpedia/databus-client). Merging and refactoring is required. 


Current caveats:
* putting a version that does not exist into `<version>` will result in a `java.util.NoSuchElementException: QueryIterPlainWrapper` 
* using <version> is much less flexible than just giving the SPARQL query
* can only parse RDF-NTriples as `.bz2` other data is simply copied
* Per default parsing data and cloning (e.g. create new versions for the databus-maven-plugin) are deactivated.
You can turn them on with `-DskipParsing=false -DskipCloning=false`, but beware the plugin needs three times the space of the data (easier to debug). 

## Setup 
Add the snapshot plugin repository, not required if you already use the databus-maven-plugin
```xml
    <pluginRepositories>
        <pluginRepository>
            <id>archiva.internal</id>
            <name>Internal Release Repository</name>
            <url>http://databus.dbpedia.org:8081/repository/internal</url>
        </pluginRepository>
        <pluginRepository>
            <id>archiva.snapshots</id>
            <name>Internal Snapshot Repository</name>
            <url>http://databus.dbpedia.org:8081/repository/snapshots</url>
            <snapshots>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>
```

Add the following plugin entry to the `BaseBuild(<build>)` element in your maven pom.

```xml  
  <build>
  ...
    <plugins>
      <plugin>
        <groupId>org.dbpedia.databus</groupId>
        <artifactId>databus-derive-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
          <execution>
            <goals>
              <goal>clone</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <versions>
            <version>https://databus.dbpedia.org/dbpedia/enrichment/mappingbased-literals/2019.03.01</version>
          </versions>
          <skipParsing>true</skipParsing>
          <skipCloning>true</skipCloning>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

Note that you can also use a variable for configuration. This configuration downloads the whole [DBpedia/mappings](https://databus.dbpedia.org/dbpedia/mappings) group.
```xml
 <properties>
	<!-- used for derive plugin, can be anything really -->
        <databus.deriveversion>2019.08.01</databus.deriveversion>
 </properties>

 <configuration>
          <versions>
            <version>https://databus.dbpedia.org/marvin/mappings/geo-coordinates-mappingbased/${databus.deriveversion}</version>
            <version>https://databus.dbpedia.org/marvin/mappings/instance-types/${databus.deriveversion}</version>
            <version>https://databus.dbpedia.org/marvin/mappings/mappingbased-literals/${databus.deriveversion}</version>
            <version>https://databus.dbpedia.org/marvin/mappings/mappingbased-objects/${databus.deriveversion}</version>
            <version>https://databus.dbpedia.org/marvin/mappings/mappingbased-objects-uncleaned/${databus.deriveversion}</version>
            <version>https://databus.dbpedia.org/marvin/mappings/specific-mappingbased-properties/${databus.marvinversion}</version>
          </versions>
          <skipParsing>true</skipParsing>
          <skipCloning>true</skipCloning>
 </configuration>
```

Maven manual install
```
git clone https://github.com/dbpedia/databus-derive.git
cd databus-derive
mvn clean install
```


## Usage

Execute the plugin.

```
# normal
export MAVEN_OPTS="-Xmx24000m" 
# for 64Cores ~ 64GB to parse 5GB bz2
export MAVEN_OPTS="-Xmx64000m" 

mvn databus-derive:clone
```

## Standalone Execution

It is also possible to just create cleaned triples and pareslogs.
```
mvn scala:run -Dlauncher=flatRdfParser -DaddArgs="$flat-rdf-in|-o|$flat-rdf-out|-r|$report-out"
```
or
```
./flatRdfParser <flat-rdf-in> <flat-rdf-out> <report-out>
```
for help add `--help`

## Example

```
git clone https://github.com/dbpedia/databus-derive.git
cd databus-derive/ && mvn clean install
wget http://dbpedia-mappings.tib.eu/release/mappings/mappingbased-literals/2019.10.01/mappingbased-literals_lang\=de.ttl.bz2
./flatRdfParser mappingbased-literals_lang=de.ttl.bz2 --discard-warnings > mappingbased-literals_lang=de_cleaned.ttl
```

# Run NT-Parser with Python

## Locally

see [src/main/python/README.md](src/main/python/README.md)
## With Docker
Put your files in the ntfiles folder or mount a volume manually
```bash
mvn clean package
docker build -t ntparser .
docker run \
  ntparser \
  --input ntfiles/test.nt.bz2 \
  --triples output-triples.nt \
  --report output-report.txt \
  --chunk 5000 \
  --reportFormat TEXT \
  --removeWarnings

```