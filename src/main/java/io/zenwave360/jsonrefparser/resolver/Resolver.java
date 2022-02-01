package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;

import java.net.URL;

public interface Resolver {

    String resolve($Ref $ref);

    default String getBaseURL(URL url) {
        if (url != null) {
            String urlStr = url.toExternalForm();
            return (urlStr.substring(0, urlStr.lastIndexOf('/')));
        }
        return null;
    }
}
