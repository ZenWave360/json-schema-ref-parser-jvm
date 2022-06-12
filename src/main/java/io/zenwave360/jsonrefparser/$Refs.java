package io.zenwave360.jsonrefparser;

import com.fasterxml.jackson.core.JsonLocation;
import io.zenwave360.jsonrefparser.parser.ExtendedJsonContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class $Refs {

    public boolean circular;

    private final List<String> paths = new ArrayList<>();
    private final List<String> refs = new ArrayList<>();

    public ExtendedJsonContext jsonContext;

    private Map<URL, ExtendedJsonContext> jsonContextMap = new HashMap<>();

    public final URL file;
    public final URL rootDir;

    // References are kept on a list (instead of a Map) because Map.hashCode() is calculated recursively and may be circular references.
    private List<Pair<$Ref, Object>> originalRefsList = new ArrayList<>();

    public $Refs(ExtendedJsonContext jsonContext) {
        this.jsonContext = jsonContext;
        this.file = null;
        try {
            this.rootDir = new File(".").toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public $Refs(ExtendedJsonContext jsonContext, URL file) {
        this.jsonContext = jsonContext;
        this.file = file;
        String parentFile = FilenameUtils.getFullPath(file.toExternalForm()) + (file.getQuery() != null? "?" + file.getQuery() : "");
        try {
            this.rootDir = new URL(parentFile);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Pair<$Ref, Object>> getOriginalRefsList() {
        return originalRefsList;
    }

    public $Ref getOriginalRef(Object obj) {
        return originalRefsList.stream()
                .filter(pair -> pair.getValue() == obj)
                .map(pair -> pair.getKey())
                .findFirst().orElse(null);
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

    public void addJsonContext(URL url, ExtendedJsonContext jsonContext) {
        jsonContextMap.put(url, jsonContext);
    }

    public void saveOriginalRef($Ref originalRef, Object resolved) {
//        if(this.circular) {
//            return;
//        }
        if(!originalRef.equals(resolved)) {
            this.originalRefsList.add(Pair.of(originalRef, resolved));
        }
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
    public Pair<JsonLocation, JsonLocation> getJsonLocationRange(URL fileURL, String jsonPath) {
        if(jsonContextMap.containsKey(fileURL)) {
            return jsonContextMap.get(fileURL).getJsonLocationsMap().get(jsonPath);
        }
        return null;
    }

    public Object get(String $ref) {
        return jsonContext.read($ref);
    }

    public <T> T get(String $ref, Class<T> type) {
        return jsonContext.read($ref, type);
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
