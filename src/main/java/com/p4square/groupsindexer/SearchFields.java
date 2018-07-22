package com.p4square.groupsindexer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p4square.ccbapi.CCBAPI;
import com.p4square.ccbapi.CCBAPIClient;
import com.p4square.ccbapi.model.*;
import com.p4square.groupsindexer.model.ErrorResponse;
import com.p4square.groupsindexer.model.SearchField;
import com.p4square.groupsindexer.model.StringPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchFields is an API Gateway Proxy which returns the searchable dropdown fields and their choices.
 *
 * Required (custom) environment variables:
 * <ul>
 *  <li>CCBAPIURL</li>
 *  <li>CCBAPIUser</li>
 *  <li>CCBAPIPassword</li>
 * </ul>
 *
 */
public class SearchFields implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final long REFRESH_INTERVAL_MS = 15 * 60 * 1000;

    private static final Logger LOG = LogManager.getLogger(GroupsSearch.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CCBAPI ccbClient;

    private List<SearchField> cachedFields;
    private long lastRefresh;

    public SearchFields() throws Exception {
        // Setup CCB Client
        final String CCBAPIURL = System.getenv("CCBAPIURL");
        final String CCBAPIUser = System.getenv("CCBAPIUser");
        final String CCBAPIPassword = System.getenv("CCBAPIPassword");
        ccbClient = new CCBAPIClient(new URI(CCBAPIURL), CCBAPIUser, CCBAPIPassword);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        final APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            final List<SearchField> fields = getFields();
            if (fields == null) {
                response.setStatusCode(500);
                response.setBody(MAPPER.writeValueAsString(new ErrorResponse("Unable to get search fields.")));
                return response;
            }

            response.setStatusCode(200);
            response.setBody(MAPPER.writeValueAsString(fields));

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

    private synchronized List<SearchField> getFields() {
        if (System.currentTimeMillis() - lastRefresh < REFRESH_INTERVAL_MS) {
            LOG.debug("Using cached CCB fields");
            return cachedFields;
        }

        try {
            LOG.info("Fetching fields from CCB");

            cachedFields = new ArrayList<>();
            final GetCustomFieldLabelsResponse labels = ccbClient.getCustomFieldLabels();

            cachedFields.add(new SearchField("groupTypeId", "Group Type", getValues(LookupTableType.GROUP_TYPE)));
            // TODO fields.add(new SearchField("campusId", "Campus", ...));
            cachedFields.add(new SearchField("meetingDayId", "Day", getValues(LookupTableType.MEET_DAY)));


            for (final CustomField field : labels.getCustomFields()) {
                final LookupTableType type = getTypeFromString(field.getName());
                if (type != null) {
                    cachedFields.add(new SearchField(getSearchFieldIdForType(type), field.getLabel(), getValues(type)));
                }
            }

            cachedFields.add(new SearchField("childcare", "Childcare",
                    Arrays.asList(StringPair.of("true", "Yes"), StringPair.of("false", "No"))));

            lastRefresh = System.currentTimeMillis();
            return cachedFields;

        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
    }

    private LookupTableType getTypeFromString(String name) {
        switch (name) {
            case "udf_grp_pulldown_1":
                return LookupTableType.UDF_GRP_PULLDOWN_1;
            case "udf_grp_pulldown_2":
                return LookupTableType.UDF_GRP_PULLDOWN_2;
            case "udf_grp_pulldown_3":
                return LookupTableType.UDF_GRP_PULLDOWN_3;
            default:
                return null;
        }
    }

    private String getSearchFieldIdForType(LookupTableType type) {
        switch (type) {
            case UDF_GRP_PULLDOWN_1:
                return "udf_1";
            case UDF_GRP_PULLDOWN_2:
                return "udf_2";
            case UDF_GRP_PULLDOWN_3:
                return "udf_3";
            default:
                throw new IllegalArgumentException();
        }
    }

    private List<StringPair> getValues(LookupTableType type) throws IOException {
        final GetLookupTableRequest lookupTableRequest = new GetLookupTableRequest().withType(type);
        final GetLookupTableResponse lookupTable = ccbClient.getLookupTable(lookupTableRequest);

        return lookupTable.getItems()
                .stream()
                .map(entry -> StringPair.of(String.valueOf(entry.getId()), entry.getName()))
                .collect(Collectors.toList());
    }

}
