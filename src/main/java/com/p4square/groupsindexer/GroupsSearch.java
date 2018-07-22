package com.p4square.groupsindexer;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p4square.groupsindexer.model.ErrorResponse;
import com.p4square.groupsindexer.model.GroupSearchDocument;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GroupsSearch is an API Gateway Proxy Lambda which executes a search and returns the results.
 *
 * Required (custom) environment variables:
 *
 * <ul>
 *  <li>ES_URL</li>
 *  <li>IMAGE_URL_PREFIX</li>
 * </ul>
 */
public class GroupsSearch implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

    private static final Logger LOG = LogManager.getLogger(GroupsSearch.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestHighLevelClient esClient;
    private final String imageUrlPrefix;

    public GroupsSearch() throws Exception {
        // Prefix to prepend to image urls.
        imageUrlPrefix = System.getenv("IMAGE_URL_PREFIX");

        // Setup ElasticSearch client
        final String ES_URL = System.getenv("ES_URL");
        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName("es");
        signer.setRegionName(System.getenv("AWS_DEFAULT_REGION"));
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(signer.getServiceName(), signer, credentialsProvider);

        esClient = new RestHighLevelClient(RestClient
                .builder(HttpHost.create(ES_URL))
                .setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        final APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            final Map<String, String> params = event.getQueryStringParameters();
            if (params == null) {
                response.setStatusCode(400);
                response.setBody(MAPPER.writeValueAsString(new ErrorResponse("Request must contain a query.")));
                return response;
            }

            final BoolQueryBuilder query = QueryBuilders.boolQuery();

            if (params.containsKey("q")) {
                query.must(QueryBuilders.simpleQueryStringQuery(params.get("q")));
            }

            if (params.containsKey("groupTypeId")) {
                query.filter(QueryBuilders.termQuery("groupType.id", params.get("groupTypeId")));
            }

            if (params.containsKey("campusId")) {
                query.filter(QueryBuilders.termQuery("campus.id", params.get("campusId")));
            }

            if (params.containsKey("meetingDayId")) {
                query.filter(QueryBuilders.termQuery("meetingDay.id", params.get("meetingDayId")));
            }

            if (params.containsKey("childcare")) {
                query.filter(QueryBuilders.termQuery("childcare", Boolean.parseBoolean(params.get("childcare"))));
            }

            params.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().startsWith("udf_"))
                    .map(entry -> QueryBuilders.termQuery("udf." + entry.getKey() + ".id", entry.getValue()))
                    .forEach(query::filter);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(query);
            searchSourceBuilder.size(20);

            SearchRequest searchRequest = new SearchRequest("groups");
            searchRequest.types("group");
            searchRequest.source(searchSourceBuilder);

            SearchResponse searchResponse = esClient.search(searchRequest);

            List<GroupSearchDocument> docs = new ArrayList<>();
            for (final SearchHit hit : searchResponse.getHits().getHits()) {
                GroupSearchDocument doc = MAPPER.readValue(hit.getSourceAsString(), GroupSearchDocument.class);
                // Sanitize the output
                doc.setLeaderEmail(null);
                if (doc.getImageUrl() != null) {
                    doc.setImageUrl(imageUrlPrefix + "/" + doc.getImageUrl());
                }
                docs.add(doc);
            }

            response.setStatusCode(200);
            response.setBody(MAPPER.writeValueAsString(docs));

        } catch (Exception e) {
            LOG.error(e.getMessage());
            response.setStatusCode(500);
            try {
                response.setBody(MAPPER.writeValueAsString(new ErrorResponse(e.getMessage())));
            } catch (JsonProcessingException _) {
                // Unexpected.
            }
        }

        return response;
    }
}
