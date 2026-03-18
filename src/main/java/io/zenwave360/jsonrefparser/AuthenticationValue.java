package io.zenwave360.jsonrefparser;

import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AuthenticationValue {

    private static final Predicate<URL> ANY_MATCH = (url) -> true;

    public static enum AuthenticationType {
        QUERY,
        HEADER;
    }

    private String key;
    private String value;
    private AuthenticationType type = AuthenticationType.HEADER;

    private List<String> urlPatterns = Arrays.asList("*");
    private Predicate<URL> urlMatcher = ANY_MATCH;

    public AuthenticationValue() {
    }

    public AuthenticationValue(String key, String value, AuthenticationType type, Predicate<URL> urlMatcher) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.urlMatcher = urlMatcher;
    }

    public AuthenticationValue withQueryParam(String key, String value) {
        this.key = key;
        this.value = value;
        this.type = AuthenticationType.QUERY;
        return this;
    }

    public AuthenticationValue withHeader(String header) {
        String[] split = StringUtils.split(header, ":");
        this.key = StringUtils.trim(split[0]);
        this.value = StringUtils.trim(split[1]);
        this.type = AuthenticationType.HEADER;
        return this;
    }

    public AuthenticationValue withHeader(String key, String value) {
        this.key = key;
        this.value = value;
        this.type = AuthenticationType.HEADER;
        return this;
    }

    public AuthenticationValue withUrlMatcher(Predicate<URL> urlMatcher) {
        this.urlMatcher = urlMatcher;
        return this;
    }

    public AuthenticationValue withUrlPattern(String urlPattern) {
        this.urlPatterns = List.of(urlPattern);
        return this;
    }

    public boolean matches(URL url) {
        if(urlMatcher != null) {
            return urlMatcher.test(url);
        } else if(urlPatterns != null) {
            return urlPatterns.stream().anyMatch(url.toString()::matches);
        }
        return ANY_MATCH.test(url);
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

    public void setUrlPattern(String urlPattern) {
        this.urlPatterns = List.of(urlPattern);
    }

    public void setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public String toString() {
        return "AuthenticationValue{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", urlPatterns=" + urlPatterns +
                ", urlMatcher=" + urlMatcher +
                '}';
    }
}
