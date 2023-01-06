/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL API project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

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
            String envLetter = envPropertyName?.toList()?.first()
            log.warn "envLetter: ${envLetter}"
            SimpleDateFormat sdf = new SimpleDateFormat("YMMdd")
            Date now = new Date()
            String zipFilePath = skosFileDir + 'BDR_name_label_' + sdf.format(now).toString() + ".zip"
            log.debug("ZIP file name will be: ${zipFilePath}")
            List shards = ['apni', 'algae', 'fungi', 'lichen', 'moss', 'afd']
            // String shard = 'apni'
            shards.each {
                String graphSchema = envLetter + it
                String treeVersionId = null
                log.debug(/ *** Building skos output for "${graphSchema}" ***/)
                Date start = new Date()
                Performance.printTime(start, 1)
                HttpRequest request = apiAccessService.buildRequest(graphSchema)
                HttpResponse<Map> response = httpClient.toBlocking().exchange(request, Map)
                // stop here is there are errors
                if (response.body().containsKey('errors')) {
                    log.error("ERROR MESSAGE:  ${response.body().errors.extensions.internal.error}")
                    return
                }
                Performance.printTime(start, 2)
                Map resBodyData = response?.body()['data']
                Map stats = [
                        "bdr_context": resBodyData[graphSchema + '_bdr_context_v'].size(),
                        "bdr_sdo": resBodyData[graphSchema + '_bdr_graph_v']['bdr_sdo']?.first()?.size(),
                        'bdr_tree_schema': resBodyData[graphSchema + '_bdr_graph_v']['bdr_tree_schema']?.first()?.size(),
                        "bdr_schema": resBodyData[graphSchema + '_bdr_graph_v']['bdr_schema']?.first()?.size(),
                        "bdr_labels": resBodyData[graphSchema + '_bdr_graph_v']['bdr_labels'].size(),
                        "bdr_top_concept": resBodyData[graphSchema + '_bdr_graph_v']['bdr_top_concept']?.first()?.size(),
                        "bdr_concepts": resBodyData[graphSchema + '_bdr_graph_v']['bdr_concepts']?.first()?.size(),
                        "bdr_alt_labels": resBodyData[graphSchema + '_bdr_graph_v']['bdr_alt_labels']?.first()?.size(),
                        "bdr_unplaced": resBodyData[graphSchema + '_bdr_graph_v']['bdr_unplaced']?.first()?.size()
                ]
                log.debug(stats.toString())
                Map interimOutput = [:]
                List graphOutput = []

                graphOutput.add( resBodyData[graphSchema + '_bdr_graph_v']['bdr_sdo'][0][0])
                graphOutput.add( resBodyData[graphSchema + '_bdr_graph_v']['bdr_tree_schema'][0][0])
                graphOutput.add( resBodyData[graphSchema + '_bdr_graph_v']['bdr_schema'][0][0])
                graphOutput.add( resBodyData[graphSchema + '_bdr_graph_v']['bdr_top_concept'][0][0])
                treeVersionId = resBodyData[graphSchema + '_bdr_graph_v']['bdr_schema'][0][0]['_id'].toString().split(':')[1]

                resBodyData[graphSchema + '_bdr_graph_v']['bdr_concepts']?.first()?.each {
                    graphOutput.add it
                }
                resBodyData[graphSchema + '_bdr_graph_v']['bdr_alt_labels']?.first()?.each {
                    graphOutput.add it
                }
                resBodyData[graphSchema + '_bdr_graph_v']['bdr_unplaced']?.first()?.each {
                    graphOutput.add it
                }
                interimOutput << ["@context": response?.body()?.data[graphSchema + '_bdr_context_v']?.first()]
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