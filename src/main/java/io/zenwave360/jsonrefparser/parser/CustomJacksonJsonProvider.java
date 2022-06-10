package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Allows accessing array properties using map-like json-paths (i.e "$['servers]['1']" will return the same as "$['servers][1]")
 */
class CustomJacksonJsonProvider extends JacksonJsonProvider {

    private Logger log = LoggerFactory.getLogger(CustomJacksonJsonProvider.class);

    public CustomJacksonJsonProvider(ObjectMapper objectMapper, ObjectReader objectReader) {
        super(objectMapper, objectReader);
    }

    public boolean isArray(Object obj) {
        log.trace("isArray {}", obj != null? obj.getClass() : null);
        return (obj instanceof List) || (obj instanceof Map && ((Map<String, ?>) obj).keySet().stream().allMatch(key -> NumberUtils.isDigits(key)));
    }

    public Object getArrayIndex(Object obj, int idx) {
        return obj instanceof Map? ((Map) obj).get(idx + "") : ((List) obj).get(idx);
    }

    @Override
    public boolean isMap(Object obj) {
        log.trace("isMap {}", obj != null? obj.getClass() : null);
        return (obj instanceof Map) || (obj instanceof List);
    }

    public Collection<String> getPropertyKeys(Object obj) {
        if (obj instanceof List) {
            return IntStream.range(0, ((List<?>) obj).size()).boxed().map(i -> String.valueOf(i)).collect(Collectors.toList());
        } else {
            return ((Map) obj).keySet();
        }
    }

    @Override
    public Object getMapValue(Object obj, String key) {
        log.trace("getMapValue {} {}", obj.getClass(), key);
        if (obj instanceof List) {
            Integer index = parseIndex(key);
            return index != null? getArrayIndex(obj, index) : JsonProvider.UNDEFINED;
        }
        return super.getMapValue(obj, key);
    }

    public void setProperty(Object obj, Object key, Object value) {
        log.trace("setProperty {} {} {}", obj.getClass(), key, value.getClass());
        if (obj instanceof Map)
            ((Map) obj).put(key.toString(), value);
        else {
            setArrayIndex(obj, parseIndex(key.toString()), value);
        }
    }

    public void removeProperty(Object obj, Object key) {
        log.trace("removeProperty {} {}", obj.getClass(), key);
        if (obj instanceof Map) {
            ((Map) obj).remove(key.toString());
        } else {
            ((List) obj).remove(parseIndex(key.toString()));
        }
    }

    private Integer parseIndex(String key) {
        key = key.replaceAll("['\"]", "");
        return NumberUtils.isDigits(key)? Integer.parseInt(key) : null;
    }
}
