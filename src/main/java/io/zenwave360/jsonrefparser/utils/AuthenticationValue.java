package io.zenwave360.jsonrefparser.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AuthenticationValue {

    public static enum AuthenticationType {
        QUERY,
        HEADER;
    }

    private String key;
    private String value;
    private AuthenticationType type = AuthenticationType.HEADER;

    private Predicate<URL> urlMatcher;

    public AuthenticationValue() {
    }

    public AuthenticationValue(String key, String value, AuthenticationType type, Predicate<URL> urlMatcher) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.urlMatcher = urlMatcher;
    }

    public AuthenticationValue withQueryParams(String key, String value) {
        return new AuthenticationValue(key, value, AuthenticationType.QUERY, null);
    }

    public static AuthenticationValue withHeader(String header) {
        String[] split = StringUtils.split(header, ":");
        String key = StringUtils.trim(split[0]);
        String value = StringUtils.trim(split[1]);
        return new AuthenticationValue(key, value, AuthenticationType.HEADER, null);
    }

    public static List<AuthenticationValue> headersFromCsv(String csv) {
        return Arrays.asList(csv.split(",|;|\n|\t"))
                .stream().map(header -> withHeader(header))
                .collect(Collectors.toList());
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public AuthenticationType getType() {
        return this.type;
    }

    public Predicate<URL> getUrlMatcher() {
        return this.urlMatcher;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(AuthenticationType type) {
        this.type = type;
    }

    public void setUrlMatcher(Predicate<URL> urlMatcher) {
        this.urlMatcher = urlMatcher;
    }
}