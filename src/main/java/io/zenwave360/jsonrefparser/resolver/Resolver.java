package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;
import io.zenwave360.jsonrefparser.AuthenticationValue;

import java.net.URL;

public interface Resolver {

    String resolve($Ref $ref) throws MissingResourceException;

    class MissingResourceException extends RuntimeException {
        public MissingResourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    default <T extends Resolver> T withAuthentication(AuthenticationValue authenticationValue) {
        return (T) this;
    }

    default String getBaseURL(URL url) {
        if (url != null) {
            String urlStr = url.toExternalForm();
            return (urlStr.substring(0, urlStr.lastIndexOf('/')));
        }
        return null;
    }
}
