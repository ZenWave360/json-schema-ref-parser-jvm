package io.zenwave360.jsonrefparser;

import com.fasterxml.jackson.core.JsonLocation;
import com.jayway.jsonpath.PathNotFoundException;
import io.zenwave360.jsonrefparser.parser.ExtendedJsonContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class $Refs {

    public boolean circular;

    private final List<String> paths = new ArrayList<>();
    private final List<String> refs = new ArrayList<>();

    public ExtendedJsonContext jsonContext;

    private Map<URI, ExtendedJsonContext> jsonContextMap = new HashMap<>();

    public final URI file;
    public final URI rootDir;

    // References are kept on a list (instead of a Map) because Map.hashCode() is calculated recursively and may be circular references.
    private List<Pair<$Ref, Object>> originalRefsList = new ArrayList<>();
    private List<Pair<$Ref, Object>> replacedRefsList = new ArrayList<>();
    private List<Pair<Map, List>> originalAllOfList = new ArrayList<>();

    public $Refs(ExtendedJsonContext jsonContext) {
        this.jsonContext = jsonContext;
        this.file = null;
        this.rootDir = new File(".").toURI();
    }

    public $Refs(ExtendedJsonContext jsonContext, URI file) {
        this.jsonContext = jsonContext;
        this.file = file;
        FilenameUtils.getFullPath(file.toString());
        String parentFile = FilenameUtils.getFullPath(file.toString()) + (file.getQuery() != null? "?" + file.getQuery() : "");
        this.rootDir = URI.create(parentFile);
    }

    public List<Pair<$Ref, Object>> getOriginalRefsList() {
        return originalRefsList;
    }

    public List<Pair<$Ref, Object>> getReplacedRefsList() {
        return replacedRefsList;
    }

    public $Ref getOriginalRef(Object obj) {
        Object originalAllOf = getOriginalAllOf(obj);
        return originalRefsList.stream()
                .filter(pair -> isOriginalRef(obj, pair.getValue(), originalAllOf))
                .map(pair -> pair.getKey())
                .findFirst().orElse(null);
    }

    public Object getObjectForRef($Ref ref) {
        return originalRefsList.stream()
                .filter(pair -> pair.getKey().toString().equals(ref.toString()))
                .map(Pair::getValue).findFirst().orElse(null);
    }

    public List<Map<String, Object>> getOriginalAllOf(Object resolvedAllOf) {
        return originalAllOfList.stream().filter(pair -> pair.getKey() == resolvedAllOf).map(Pair::getValue).findFirst().orElse(null);
    }

    protected boolean isOriginalRef(Object value, Object savedValue, Object originalAllOf) {
        return value == savedValue || (originalAllOf != null && savedValue instanceof Map && ((Map<?, ?>) savedValue).get("allOf") == originalAllOf);
    }

    private $Ref getOriginalRefForAllOf(Object resolvedAllOf) {
        if(resolvedAllOf instanceof Map) {
            Object originalAllOf = getOriginalAllOf((Map<String, Object>) resolvedAllOf);
            if(originalAllOf != null) {

            }
        }
        return null;
    }

    public void addRef(String ref) {
        if(ref != null && !this.refs.contains(ref)) {
            this.refs.add(ref);
        }
    }

    public void addPath(URL url) {
        if(url != null && !this.paths.contains(url.toExternalForm())) {
            this.paths.add(url.toExternalForm());
        }
    }

    public void addPath(URI uri) {
        if(uri != null && !this.paths.contains(uri.toString())) {
            this.paths.add(uri.toString());
        }
    }

    public void addJsonContext(URI url, ExtendedJsonContext jsonContext) {
        jsonContextMap.put(url, jsonContext);
    }

    public void saveOriginalRef($Ref originalRef, Object resolved) {
        if(!originalRef.equals(resolved)) {
            this.originalRefsList.add(Pair.of(originalRef, resolved));
        }
    }

    public void saveReplacedRef($Ref originalRef, Object resolved) {
        this.replacedRefsList.add(Pair.of(originalRef, resolved));
    }

    public void saveOriginalAllOf(Map<String, Object> resolvedAllOf, List originalAllOf) {
        originalAllOfList.add(Pair.of(resolvedAllOf, originalAllOf));
    }

    public Map<String, Object> schema() {
        return (Map<String, Object>) jsonContext.json();
    }

    /**
     * Returns the paths/URLs of all the files in your schema (including the main schema file).
     *
     * @param types (optional) Optionally only return certain types of paths ("file", "http", etc.)
     * @return paths/URLs of all files in your schema
     */
    public List<String> paths(String... types) {
        if(types == null || types.length == 0) {
            return paths;
        }

        // TODO filter for more than one
        return this.paths.stream().filter(path -> path.startsWith(types[0])).collect(Collectors.toList());
    }

    public List<String> refs(String... types) {
        if(types == null || types.length == 0) {
            return refs;
        }
        // TODO filter for more than one
        return this.refs.stream().filter(ref -> ref.startsWith(types[0])).collect(Collectors.toList());
    }

    public Pair<JsonLocation, JsonLocation> getJsonLocationRange(String jsonPath) {
        return jsonContext.getJsonLocationsMap().get(jsonPath);
    }
    public Pair<JsonLocation, JsonLocation> getJsonLocationRange(URI fileURI, String jsonPath) {
        if(jsonContextMap.containsKey(fileURI)) {
            return jsonContextMap.get(fileURI).getJsonLocationsMap().get(jsonPath);
        }
        return null;
    }

    public Object get(String $ref) {
        try {
            return jsonContext.read($ref);
        } catch (UndeclaredThrowableException e) {
            if(e.getUndeclaredThrowable().getCause() instanceof PathNotFoundException) {
                return null;
            }
            throw e;
        }
    }

    public <T> T get(String $ref, Class<T> type) {
        try {
            return jsonContext.read($ref, type);
        } catch (UndeclaredThrowableException e) {
            if(e.getUndeclaredThrowable().getCause() instanceof PathNotFoundException) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Sets the value at the given path in the schema. If the property, or any of its parents, don't exist, they will be created.
     *
     * @param $ref The JSON Reference path, optionally with a JSON Pointer in the hash
     * @param value The value to assign. Can be anything (object, string, number, etc.)
     */
    public void set(String $ref, Object value) {
        jsonContext.set($ref, value);
    }
}
