package com.p4square.groupsindexer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * A list of groups and some metadata.
 */
public class GroupsCollection {
    @JsonProperty("last-updated")
    private Instant lastUpdated;

    @JsonProperty("groups")
    private List<GroupSearchDocument> groups;

    @JsonProperty("search-fields")
    private List<SearchField> searchFields;

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<GroupSearchDocument> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupSearchDocument> groups) {
        this.groups = groups;
    }

    public List<SearchField> getSearchFields() {
        return searchFields;
    }

    public void setSearchFields(List<SearchField> searchFields) {
        this.searchFields = searchFields;
    }
}
