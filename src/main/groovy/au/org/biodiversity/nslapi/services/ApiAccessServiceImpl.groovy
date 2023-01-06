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

package au.org.biodiversity.nslapi.services

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton

/*
    Implementation of the ApiAccessService
* */

@Singleton
@CompileStatic
@Slf4j
@SuppressWarnings('GrMethodMayBeStatic')
class ApiAccessServiceImpl implements ApiAccessService {
    @Property(name = "nslapi.graphql.url")
    String graphEndpoint

    @Property(name = "nslapi.graphql.adminSecret")
    String graphqlAdminSecret

    @Property(name = "nslapi.queries.bdr-skos")
    String bdrSkosQuery

    /**
     * Create a GraphQL or web service API query to use in the request
     * TODO: Build a class to generate queries on the fly and keep history
     *
     * @param dataset
     * @param name
     * @return HttpRequest
     */
    String buildGraphQuery(String name, String dataset) {
        String checkSciOrCan = '{ "query" : "query checkName($name_to_match: String!, $dataset_name: String!) { api_names(where: {_and: {_or: [{scientificName: {_ilike: $name_to_match}}, {canonicalName: {_ilike: $name_to_match}}], datasetName: {_ilike: $dataset_name}}}) { scientificName resultNameMatchStatus:taxonomicStatus nameType taxonRank scientificNameID canonicalName scientificNameAuthorship nomIlleg nomInval nomenclaturalCode nomenclaturalStatus nameSource:datasetName taxonRankSortOrder ccAttributionIRI hasUsage { taxonID nameType taxonomicStatus proParte taxonRank taxonConceptID nameAccordingToID ccAttributionIRI hasAcceptedNameUsage { taxonID scientificName taxonRank scientificNameID canonicalName scientificNameAuthorship taxonConceptID parentNameUsageID nameType taxonomicStatus proParte kingdom family taxonSource:datasetName nameAccordingTo nameAccordingToID taxonRemarks taxonDistribution higherClassification license ccAttributionIRI } } } }", "variables": { "name_to_match": "' + name + '", "dataset_name": "' + dataset + '" } }'
        checkSciOrCan
    }

    /**
     * Generate a GraphQL query and return it
     *
     * @param requestType
     * @param name
     * @return HttpRequest
     */
    String generateGraphQuery(String name, String dataset){
        String queryDeclaration = '"query checkName($name_to_match: String!, $data_set: String) { '
        String queryArguments = 'api_names(where: {_and: {_or: [{scientificName: {_ilike: $name_to_match}}, {canonicalName: {_ilike: $name_to_match}}], datasetName: {_ilike: $data_set}}}) { '
        String queryProperties = 'scientificName resultNameMatchStatus:taxonomicStatus nameType taxonRank scientificNameID ' +
                'scientificName scientificNameID taxonomicStatus nameType taxonRank canonicalName scientificNameAuthorship nomIlleg ' +
                'nomInval nomenclaturalCode ccAttributionIRI datasetName nomenclaturalStatus hasUsage { taxonomicStatus proParte ' +
                'hasAcceptedName { taxonRank scientificName scientificNameID nomenclaturalStatus nameType ' +
                'canonicalName scientificNameAuthorship kingdom family nameAccordingTo taxonID ' +
                'taxonConceptID higherClassification taxonDistribution datasetName ccAttributionIRI ' +
                'hasParent { parentScientificName:scientificName parentScientificNameID:scientificNameID ' +
                'parentNameUsageID:taxonID } } } } }"'
        String queryVariables = ', "variables": { "name_to_match": "' + name + '", "data_set": "' + dataset + '" } '
        String generatedQuery = '{"query" : ' +
                "${queryDeclaration} " +
                "$queryArguments" +
                "$queryProperties" +
                "$queryVariables" +
                '}'
        generatedQuery
    }

    /**
     * Create a HttpRequest when a GraphQL query string is supplied
     * as a property
     *
     * @param query
     * @return HttpRequest
     */
    HttpRequest buildRequest( String scheme) {
        Map contextMap = ["apni": "apc apni",
                          "afd": "afd afdi",
                          "moss": "abl abni",
                          "algae": "aal aani",
                          "fungi": "afl afni",
                          "lichen": "all alni"]
        HttpRequest request = null
        // Currently only used bye bdr skos query
        // TODO: make is general purpose
        String graphQuery = bdrSkosQuery.replace('SHARDSCHEMENAME', scheme).replace('SHARDCONTEXTVALUE', contextMap[scheme[1..scheme.size()-1]])

        log.debug "graphQuery: ${graphQuery}"
        if (graphQuery) {
            request =  HttpRequest.POST(graphEndpoint, graphQuery)
                    .header('x-hasura-admin-secret', graphqlAdminSecret)
                    .header('Content-Type', 'application/json')
        }
        request
    }
}