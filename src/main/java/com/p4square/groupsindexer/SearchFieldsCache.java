package com.p4square.groupsindexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p4square.ccbapi.CCBAPI;
import com.p4square.ccbapi.model.*;
import com.p4square.groupsindexer.model.SearchField;
import com.p4square.groupsindexer.model.StringPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SearchFieldsCache {
    private static final long REFRESH_INTERVAL_MS = 15 * 60 * 1000;

    private static final Logger LOG = LogManager.getLogger(SearchFieldsCache.class);

    private final CCBAPI ccbClient;

    private List<SearchField> cachedFields;
    private long lastRefresh;

    public SearchFieldsCache(CCBAPI ccbClient) {
        this.ccbClient = ccbClient;
    }

    public synchronized List<SearchField> getSearchFields() {
        if (System.currentTimeMillis() - lastRefresh < REFRESH_INTERVAL_MS) {
            LOG.debug("Using cached CCB fields");
            return cachedFields;
        }

        try {
            LOG.info("Fetching fields from CCB");

            cachedFields = new ArrayList<>();
            final GetCustomFieldLabelsResponse labels = ccbClient.getCustomFieldLabels();

            cachedFields.add(new SearchField("groupType", "Group Type", getValues(LookupTableType.GROUP_TYPE)));
            // TODO fields.add(new SearchField("campusId", "Campus", ...));
            cachedFields.add(new SearchField("meetingDay", "Day", getValues(LookupTableType.MEET_DAY)));


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
