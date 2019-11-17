package com.p4square.groupsindexer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.p4square.ccbapi.CCBAPI;
import com.p4square.ccbapi.CCBAPIClient;
import com.p4square.ccbapi.model.GetGroupProfilesRequest;
import com.p4square.ccbapi.model.GetGroupProfilesResponse;
import com.p4square.ccbapi.model.GroupProfile;
import com.p4square.ccbapi.model.InteractionType;
import com.p4square.groupsindexer.model.GroupSearchDocument;
import com.p4square.groupsindexer.model.GroupSearchDocumentAdapter;
import com.p4square.groupsindexer.model.GroupsCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * SyncGroups is a scheduled lambda which syncs groups data from CCB.
 *
 * Required (custom) environment variables:
 * <ul>
 *  <li>CCBAPIURL</li>
 *  <li>CCBAPIUser</li>
 *  <li>CCBAPIPassword</li>
 *  <li>OUTPUT_BUCKET</li>
 * </ul>
 *
 */
public class SyncGroups implements RequestHandler<ScheduledEvent, String> {

    private static final Logger LOG = LogManager.getLogger(SyncGroups.class);
    private static final GroupSearchDocumentAdapter ADAPTER = new GroupSearchDocumentAdapter();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private final String outputBucket;
    private final String baseUrl;

    private final CCBAPI ccbClient;
    private final AmazonS3 s3Client;

    private final SearchFieldsCache searchFieldsCache;

    public SyncGroups() throws Exception {
        // Setup CCB Client
        final String CCBAPIURL = System.getenv("CCBAPIURL");
        final String CCBAPIUser = System.getenv("CCBAPIUser");
        final String CCBAPIPassword = System.getenv("CCBAPIPassword");
        ccbClient = new CCBAPIClient(new URI(CCBAPIURL), CCBAPIUser, CCBAPIPassword);
        searchFieldsCache = new SearchFieldsCache(ccbClient);

        // Setup S3 Client
        outputBucket = System.getenv("OUTPUT_BUCKET");
        s3Client = AmazonS3ClientBuilder.defaultClient();

        // Prefix to prepend to image urls.
        baseUrl = System.getenv("BASE_URL");
    }

    @Override
    public String handleRequest(ScheduledEvent s3Event, Context context) {
        try {
            final GroupsCollection groupsCollection = new GroupsCollection();
            groupsCollection.setLastUpdated(Instant.now());
            groupsCollection.setSearchFields(searchFieldsCache.getSearchFields());

            final GetGroupProfilesResponse response = ccbClient.getGroupProfiles(
                    new GetGroupProfilesRequest()
                            .withIncludeImageUrl(true)
                            .withIncludeParticipants(false));

            final List<GroupSearchDocument> groups = new ArrayList<>();

            for (GroupProfile profile : response.getGroups()) {
                if (!profile.isActive() ||
                        !profile.isPublicSearchListed() ||
                        profile.getInteractionType() != InteractionType.MEMBERS_INTERACT) {
                    LOG.info("Skipping inactive/unlisted group " + profile.getName());
                    continue;
                }

                if (!profile.getGroupType().getName().contains("Community")) {
                    LOG.info("Skipping non-Community group " + profile.getName());
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
                        s3Client.putObject(outputBucket, imageKey, in, null);
                        document.setImageUrl(baseUrl + "/" + imageKey);
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

                groups.add(document);
            }

            // Save the groups data
            groupsCollection.setGroups(groups);
            s3Client.putObject(outputBucket, "data/groups-data.json", MAPPER.writeValueAsString(groupsCollection));

            LOG.info("Updated search index. Found " + response.getGroups().size() + " groups.");
            return "ok";

        } catch (IOException e) {
            LOG.error("Unexpected Exception: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
