package com.p4square.groupsindexer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * SearchField describes a pulldown field and its options.
 */
public class SearchField {

    @JsonProperty("id")
    private String id;

    @JsonProperty("label")
    private String label;

    @JsonProperty("values")
    private List<StringPair> values;

    public SearchField() { }

    public SearchField(String id, String label, List<StringPair> values) {
        this.id = id;
        this.label = label;
        this.values = values;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<StringPair> getValues() {
        return values;
    }

    public void setValues(List<StringPair> values) {
        this.values = values;
    }
}
