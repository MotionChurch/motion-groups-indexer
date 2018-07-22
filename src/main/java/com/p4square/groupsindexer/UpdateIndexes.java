package com.p4square.groupsindexer;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p4square.ccbapi.CCBAPI;
import com.p4square.ccbapi.CCBAPIClient;
import com.p4square.ccbapi.model.*;
import com.p4square.groupsindexer.model.GroupSearchDocument;
import com.p4square.groupsindexer.model.GroupSearchDocumentAdapter;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * UpdateIndexes is a scheduled lambda which populates the groups search index.
 *
 * Required (custom) environment variables:
 * <ul>
 *  <li>CCBAPIURL</li>
 *  <li>CCBAPIUser</li>
 *  <li>CCBAPIPassword</li>
 *  <li>ES_URL</li>
 *  <li>IMAGE_BUCKET</li>
 * </ul>
 *
 */
public class UpdateIndexes implements RequestHandler<ScheduledEvent, String> {

    private static final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

    private static final Logger LOG = LogManager.getLogger(UpdateIndexes.class);
    private static final GroupSearchDocumentAdapter ADAPTER = new GroupSearchDocumentAdapter();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String imageBucket;

    private final CCBAPI ccbClient;
    private final RestHighLevelClient esClient;
    private final AmazonS3 s3Client;

    public UpdateIndexes() throws Exception {
        // Setup CCB Client
        final String CCBAPIURL = System.getenv("CCBAPIURL");
        final String CCBAPIUser = System.getenv("CCBAPIUser");
        final String CCBAPIPassword = System.getenv("CCBAPIPassword");
        ccbClient = new CCBAPIClient(new URI(CCBAPIURL), CCBAPIUser, CCBAPIPassword);

        // Setup ElasticSearch client
        final String ES_URL = System.getenv("ES_URL");
        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName("es");
        signer.setRegionName(System.getenv("AWS_DEFAULT_REGION"));
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(signer.getServiceName(), signer, credentialsProvider);

        esClient = new RestHighLevelClient(RestClient
                        .builder(HttpHost.create(ES_URL))
                        .setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)));

        // Setup S3 Client
        imageBucket = System.getenv("IMAGE_BUCKET");
        s3Client = AmazonS3ClientBuilder.defaultClient();
    }

    @Override
    public String handleRequest(ScheduledEvent s3Event, Context context) {
        try {
            GetGroupProfilesResponse response = ccbClient.getGroupProfiles(
                    new GetGroupProfilesRequest()
                            .withIncludeImageUrl(true)
                            .withIncludeParticipants(false));

            final BulkRequest indexRequest = new BulkRequest();

            for (GroupProfile profile : response.getGroups()) {
                if (!profile.isActive() ||
                        !profile.isPublicSearchListed() ||
                        profile.getInteractionType() != InteractionType.MEMBERS_INTERACT) {
                    LOG.info("Skipping inactive/unlisted group " + profile.getName());
                    continue;
                }

                // Transform GroupProfile to Search Document.
                final GroupSearchDocument document = ADAPTER.apply(profile);

                // Save GroupProfile image.
                document.setImageUrl(null);
                if (profile.getImageUrl() != null && !profile.getImageUrl().isEmpty()) {
                    final String imageKey = "group-images/group-" + profile.getId();
                    InputStream in = null;
                    try {
                        final URL imageUrl = new URL(profile.getImageUrl());
                        in = imageUrl.openStream();
                        s3Client.putObject(imageBucket, imageKey, in, null);
                        document.setImageUrl(imageKey);
                    } catch (Exception e) {
                        LOG.error("Failed to upload image for group " + profile.getId(), e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }

                // Add request to batch.
                indexRequest.add(Requests
                        .indexRequest("groups")
                        .type("group")
                        .id(String.valueOf(document.getId()))
                        .source(MAPPER.writeValueAsString(document), XContentType.JSON));
            }

            BulkResponse esResponse = esClient.bulk(indexRequest);

            if (esResponse.hasFailures()) {
                throw new RuntimeException(esResponse.buildFailureMessage());
            }

            LOG.info("Updated search index. Found " + response.getGroups().size() + " groups.");
            return "ok";

        } catch (IOException e) {
            LOG.error("Unexpected Exception: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
