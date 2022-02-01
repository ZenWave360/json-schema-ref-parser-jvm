package io.zenwave360.jsonrefparser.resolver;

public enum RefFormat {
    URL,
    RELATIVE,
    INTERNAL;

    public boolean isAnExternalRefFormat() {
        return this == RefFormat.URL || this == RefFormat.RELATIVE;
    }
}
