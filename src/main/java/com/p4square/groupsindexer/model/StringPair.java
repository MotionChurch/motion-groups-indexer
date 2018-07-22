package com.p4square.groupsindexer.model;

/**
 * A key/value pair of Strings.
 */
public class StringPair {
    private String key;
    private String value;

    public static StringPair of(String key, String value) {
        final StringPair pair = new StringPair();
        pair.setKey(key);
        pair.setValue(value);
        return pair;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
