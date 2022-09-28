package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;

public enum RefFormat {
    FILE,
    URL,
    CLASSPATH,
    RELATIVE,
    INTERNAL;

    public static RefFormat of(String ref) {
        if(ref.startsWith("file:")) {
            return FILE;
        } else if (ref.startsWith("classpath:")) {
            return CLASSPATH;
        } else if (ref.startsWith("http") || ref.startsWith("https")) {
            return URL;
        } else if (ref.startsWith($Ref.REFERENCE_SEPARATOR)) {
            return INTERNAL;
        }
        return RELATIVE;
    }

    public boolean isAnExternalRefFormat() {
        return this == RefFormat.URL || this == RefFormat.RELATIVE || this == RefFormat.CLASSPATH;
    }
}
