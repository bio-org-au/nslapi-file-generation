package au.org.biodiversity.nslapi.jobs

import au.org.biodiversity.nslapi.services.ApiAccessService
import au.org.biodiversity.nslapi.util.Performance
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.zip.*
import java.text.SimpleDateFormat

/**
 * A class to generate the output files for output delivered as files
 * for nslapi endpoints
 */
@Singleton
@Slf4j
class GenerateFile {
    @Inject
    @Client("/")
    private HttpClient httpClient

    @Inject
    ApiAccessService apiAccessService
    @Property(name = 'nslapi.app.environ')
    String envPropertyName

    @Scheduled(initialDelay = '2s')
    void execute() {
        try {
            String skosFileDir = '/tmp/skos/'
            String envLetter = envPropertyName[0]
            SimpleDateFormat sdf = new SimpleDateFormat("Y-MM-dd")
            Date now = new Date()
            String zipFilePath = skosFileDir + 'BDR_name_label_' + sdf.format(now).toString().replace('-', '') + ".zip"
            log.debug("ZIP file name will be: ${zipFilePath}")
            List shards = ['apni', 'algae', 'fungi', 'lichen', 'moss', 'afd']
            // String shard = 'apni'
            shards.each {
                String graphSchema = envLetter + it
                String treeVersionId = null
                log.debug("Building skos output for ${graphSchema}")
                Date start = new Date()
                Performance.printTime(start, 1)
                HttpRequest request = apiAccessService.buildRequest(graphSchema)
                HttpResponse<Map> response = httpClient.toBlocking().exchange(request, Map)
                // stop here is there are errors
                if (response.body().containsKey('errors')) {
                    log.debug("\n  ERROR RUNNING JOB\n  ${response.body().errors}")
                    return
                }
                Performance.printTime(start, 2)
                Map stats = [
                        "bdr_context": response?.body()?.data[graphSchema + '_bdr_context_v'].size(),
                        "bdr_sdo": response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_sdo'][0].size(),
                        'bdr_tree_schema': response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_tree_schema'][0].size(),
                        "bdr_schema": response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_schema'][0].size(),
                        "bdr_labels": response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_labels'].size(),
                        "bdr_top_concept": response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_top_concept'][0].size(),
                        "bdr_concepts": response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_concepts'][0].size(),
                        "bdr_alt_labels": response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_alt_labels'][0].size(),
                        "bdr_unplaced": response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_unplaced'][0].size()
                ]
                log.debug(stats.toString())
                Map interimOutput = [:]
                List graphOutput = []

                graphOutput.add( response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_sdo'][0][0])
                graphOutput.add( response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_tree_schema'][0][0])
                graphOutput.add( response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_schema'][0][0])
                graphOutput.add( response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_top_concept'][0][0])
                treeVersionId = response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_schema'][0][0]['_id'].toString().split(':')[1]

                response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_concepts'][0].each {
                    graphOutput.add it
                }
                response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_alt_labels'][0]?.each {
                    graphOutput.add it
                }
                response?.body()['data'][graphSchema + '_bdr_graph_v']['bdr_unplaced'][0].each {
                    graphOutput.add it
                }
                interimOutput << ["@context": response?.body()?.data[graphSchema + '_bdr_context_v'][0]]
                interimOutput << ["@graph": graphOutput]
                Performance.printTime(start, 3)
                String returnOutput = new JsonBuilder(interimOutput).toString()
                        .replace('__', ':')
                        .replace('_type', "@type")
                        .replace('_id', '@id')
                Performance.printTime(start, 5)
                new File( skosFileDir + 'BDR_name_label_' + it + '_' + treeVersionId + '.jsonld').withWriter('utf-8') {
                    writer -> writer.writeLine(returnOutput)
                }
            }
            log.debug("Validating the output files")
            Integer numOfFiles = 0
            new File(skosFileDir).eachFile() {
                numOfFiles ++
            }
            if (shards.size() == numOfFiles) {
                log.debug("Creating zip file at ${zipFilePath}")
                ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(zipFilePath))
                new File(skosFileDir).eachFile() { file ->
                    //check if file
                    if (file.isFile() && file.name.endsWith('.jsonld')){
                        zipFile.putNextEntry(new ZipEntry(file.name))
                        def buffer = new byte[file.size()]
                        file.withInputStream {
                            zipFile.write(buffer, 0, it.read(buffer))
                        }
                        zipFile.closeEntry()
                    }
                }
                zipFile.close()
                log.debug("Job completed")
                System.exit(0)

            } else {
                log.error("There was a problem with the output. Only ${numOfFiles} of ${shards.size()} files created")
                System.exit(1)
            }
        } catch (Exception e) {
            log.error("Exception occured with message: ${e.message}")
            System.exit(2)
        }

    }
}