package io.zenwave360.jsonrefparser;

import io.zenwave360.jsonrefparser.parser.ExtendedJsonContext;
import io.zenwave360.jsonrefparser.parser.Parser;
import io.zenwave360.jsonrefparser.resolver.FileResolver;
import io.zenwave360.jsonrefparser.resolver.HttpResolver;
import io.zenwave360.jsonrefparser.resolver.RefFormat;
import io.zenwave360.jsonrefparser.resolver.Resolver;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class $RefParser {

    private static final Logger log = LoggerFactory.getLogger($RefParser.class);

    public final File file;
    public final URL url;
    public final String json;

    public $Refs refs;
    public final Map<RefFormat, Resolver> resolvers = new HashMap<>();
    {
        resolvers.put(RefFormat.RELATIVE, new FileResolver());
        resolvers.put(RefFormat.URL, new HttpResolver());
    }
    private Map<String, ExtendedJsonContext> urlJsonContextCache = new HashMap<>();
    private List<AuthenticationValue> authenticationValues = new ArrayList<>();

    private $RefParserOptions options;

    public $RefParser(File file) throws MalformedURLException {
        this.file = file;
        this.url = file.toURI().toURL();
        this.json = null;
    }

    public $RefParser(String json) {
        this.json = json;
        this.file = null;
        this.url = null;
    }

    public $RefParser(String json, URL url) {
        this.file = null;
        this.url = url;
        this.json = json;
    }

    public $RefParser parse() throws IOException {
        refs = file != null? new $Refs(Parser.parse(file), url) : new $Refs(Parser.parse(json), url);
        return this;
    }

    public $RefParser withAuthentication(AuthenticationValue authenticationValue) {
        this.authenticationValues.add(authenticationValue);
        this.resolvers.values().forEach(resolver -> resolver.withAuthentication(authenticationValue));
        return this;
    }

    public $RefParser withResolver(RefFormat refFormat, Resolver resolver) {
        this.resolvers.put(refFormat, resolver);
        return this;
    }

    public $RefParser withOptions($RefParserOptions options) {
        this.options = options;
        return this;
    }

    public $Refs getRefs() {
        return refs;
    }

    public $RefParser dereference() {
        this.refs.addPath(url);
        dereference(refs.jsonContext, refs.schema(), new String[0], url);
        return this;
    }

    public $RefParser mergeAllOf() {
        this.refs.addPath(url);
        mergeAllOf(refs.schema(), new String[0], url);
        return this;
    }

    private void mergeAllOf(Object value, String[] paths, URL currentFileURL) {
        if(paths.length > 0 && "allOf".equals(paths[paths.length -1])) {
            List allOf = (List) value;
            List<String> required = new ArrayList<>();
            Map<String, Object> properties = new LinkedHashMap<>();
            Map<String, Object> mergedAllOfObject = new LinkedHashMap<>();
            for (int i = 0; i < allOf.size(); i++) {
                if(allOf.get(i) instanceof Map) {
                    Map<String, Object> item = (Map<String, Object>) allOf.get(i);
                    mergedAllOfObject.putAll(item);
                    if(item.containsKey("properties")) {
                        properties.putAll((Map) item.get("properties"));
                    }
                    if(item.containsKey("required")) {
                        required.addAll((List) item.get("required"));
                    }
                } else {
                    throw new RuntimeException("Could not understand allOf: " + allOf.get(i));
                }
            }
            if(!required.isEmpty()) {
                mergedAllOfObject.put("required", required);
            }
            if(!properties.isEmpty()) {
                mergedAllOfObject.put("properties", properties);
            }
            String[] jsonPaths = Arrays.copyOf(paths, paths.length -1);
            String jsonPath = jsonPath(jsonPaths);
            try {
                refs.jsonContext.set(jsonPath, mergedAllOfObject);
                refs.saveOriginalAllOf(mergedAllOfObject, allOf);
            } catch (Exception e){
                log.error("Error setting jsonPath:{} in file:{}", jsonPath, currentFileURL.toExternalForm(), e);
                throw e;
            }
        } else if(value instanceof Map) {
            // visit
            ((Map<String, Object>) value).entrySet().forEach(e -> {
                mergeAllOf(e.getValue(), ArrayUtils.add(paths, e.getKey()), currentFileURL);
            });
        } else if(value instanceof List) {
            // visit
            List list = (List) value;
            for (int i = 0; i < list.size(); i++) {
                mergeAllOf(list.get(i), ArrayUtils.add(paths, i + ""), currentFileURL);
            }
        }
    }

    private Set<String> visited = new HashSet<>();
    private List<String> indent = new ArrayList<>();
    private String indent() {
        return StringUtils.join(indent, "");
    }
    private void dereference(ExtendedJsonContext jsonContext, Object value, String[] paths, URL currentFileURL) {
        var visitedNodeRef = String.format("%s%s", currentFileURL.toExternalForm(), jsonPointer(paths));
        log.trace("{}visiting {}", indent(), visitedNodeRef);
        if(visited.contains(visitedNodeRef)) {
            log.trace("{}skipping visited {}", indent(), visitedNodeRef);
            return;
        }
        visited.add(visitedNodeRef);
        if(paths.length > 0 && "$ref".equals(paths[paths.length -1])) {
            $Ref $ref = $Ref.of((String) value, currentFileURL);
            boolean isCircular = (jsonPointer(paths) + "/").startsWith($ref.getPath() + "/") && ($ref.getUrl() == null || $ref.getUrl().equals(currentFileURL));
            if(isCircular) {
                if(options != null && $RefParserOptions.OnCircular.FAIL == options.onCircular) {
                    throw new RuntimeException("Failing: Circular references not allowed " + $ref);
                }
                if(options != null && $RefParserOptions.OnCircular.SKIP == options.onCircular) {
                    return;
                }
                this.refs.circular = true;
                boolean isSelfReferencing = (jsonPointer(paths)).equals($ref.getPath() + "/$ref");
                if(isSelfReferencing) {
                    log.debug("{}Skipping self referencing reference [TODO: implement this] {}", indent(), $ref);
                }
            }


            // do dereference
            String[] innerJsonPaths = Arrays.copyOf(paths, paths.length -1);
            String innerJsonPath = jsonPath(innerJsonPaths);
            indent.add("->  ");
            log.trace("{}resolving {} for {}", indent(), $ref, visitedNodeRef);
            Object resolved = dereference($ref, jsonContext, currentFileURL);
            indent.remove(indent.size() -1);
            // dereference resolved
            var resolvedRefURL = ObjectUtils.firstNonNull($ref.getUrl(), currentFileURL);
            var resolvedNodePaths = jsonPointerToPaths($ref.getPath());
            var resolvedNodeRef =  String.format("%s%s", resolvedRefURL.toExternalForm(), $ref.getPath());
            indent.add(" => ");
            log.trace("{}dereferencing resolved {}", indent(), resolvedNodeRef);
            dereference(jsonContext, resolved, resolvedNodePaths, resolvedRefURL);
            indent.remove(indent.size() -1);

            try {
                log.trace("{}setting resolved value at {} {}", indent(), innerJsonPath, currentFileURL);
                resolved = dereference($ref, jsonContext, currentFileURL);
                this.refs.saveOriginalRef($ref, resolved);
                jsonContext.set(innerJsonPath, resolved);
            }catch (Exception e){
                log.error("Error setting jsonPath: {} in {}", innerJsonPath,  currentFileURL.toExternalForm(), e);
                throw e;
            }
        } else if(value instanceof Map) {
             // visit
             ((Map<String, Object>) value).entrySet().forEach(e -> {
                dereference(jsonContext, e.getValue(), ArrayUtils.add(paths, e.getKey()), currentFileURL);
            });
        } else if(value instanceof List) {
             // visit
             List list = (List) value;
             for (int i = 0; i < list.size(); i++) {
                 dereference(jsonContext, list.get(i), ArrayUtils.add(paths, i + ""), currentFileURL);
             }
        }
    }

    private Object dereference($Ref $ref, ExtendedJsonContext jsonContext, URL currentFileURL)  {
        this.refs.addRef($ref.getRef());
        // resolve external file
        if($ref.getRefFormat().isAnExternalRefFormat()) {
            this.refs.addPath($ref.getUrl());
            String refUrl = $ref.getUrl().toExternalForm();
            if(urlJsonContextCache.containsKey(refUrl)) {
                jsonContext = urlJsonContextCache.get(refUrl);
            } else {
                Resolver resolver = resolvers.get($ref.getRefFormat());
                String resolved = resolver.resolve($ref);
                jsonContext = Parser.parse(resolved);
                urlJsonContextCache.put(refUrl, jsonContext);
                this.refs.addJsonContext($ref.getUrl(), jsonContext);
                if(jsonContext.json() instanceof Map || jsonContext.json() instanceof List) {
                    // log.trace("dereferencing resolved {}", $ref);
                    dereference(jsonContext, jsonContext.json(), new String[0], $ref.getUrl());
                }
            }
        }
        // resolve internal path
        if(StringUtils.isNotBlank($ref.getPath())) {
            String jsonPaths[] = $ref.getPath().replace($Ref.REFERENCE_SEPARATOR, "").split("/");
            String jsonPath = jsonPath(jsonPaths);
            try {
                Object resolved = jsonContext.read(jsonPath);
                return resolved;
            } catch (Exception e) {
                log.error("Error reading internal path: {}", $ref, e);
                throw e;
            }
        }
        return jsonContext.json();
    }

    private String jsonPath(String[] paths) {
        return "$" + Arrays.stream(paths).map(path -> "['" + path + "']").collect(Collectors.joining());
    }

    private String jsonPointer(String[] paths) {
        return "#/" + Arrays.stream(paths)
                .map(path -> path
                    .replace("~", "~0")
                    .replace("/", "~1")
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\""))
                .collect(Collectors.joining("/"));
    }

    private String[] jsonPointerToPaths(String jsonPointer) {
        if(jsonPointer == null) {
            return new String[0];
        }
        return Arrays.stream(jsonPointer.replaceFirst("^#/", "")
                .split("/")).map(path -> path
                .replace("~0", "~")
                .replace("~1", "/")
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")).collect(Collectors.toList()).toArray(new String[0]);
    }
}
