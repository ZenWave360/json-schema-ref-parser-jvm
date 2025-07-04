package io.zenwave360.jsonrefparser;

import io.zenwave360.jsonrefparser.parser.ExtendedJsonContext;
import io.zenwave360.jsonrefparser.parser.Parser;
import io.zenwave360.jsonrefparser.resolver.ClasspathResolver;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class $RefParser {

    private static final Logger log = LoggerFactory.getLogger($RefParser.class);

    public final File file;
    public final URI uri;
    public final String json;

    private ClassLoader resourceClassLoader;

    public $Refs refs;
    public final Map<RefFormat, Resolver> resolvers = new HashMap<>();
    {
        resolvers.put(RefFormat.FILE, new FileResolver());
        resolvers.put(RefFormat.URL, new HttpResolver());
        resolvers.put(RefFormat.CLASSPATH, new ClasspathResolver());
    }
    private Map<String, ExtendedJsonContext> urlJsonContextCache = new HashMap<>();
    private List<AuthenticationValue> authenticationValues = new ArrayList<>();

    private $RefParserOptions options;

    public $RefParser(File file) throws MalformedURLException {
        this.file = file;
        this.uri = file.toURI();
        this.json = null;
    }

    public $RefParser(URI uri) {
        if(uri.toString().startsWith("classpath:") && !uri.toString().startsWith("classpath:/")) {
            // gracefully handle classpath: without the slash
            uri = URI.create(uri.toString().replace("classpath:", "classpath:/"));
        }
        if(uri.getScheme() == null || uri.getScheme().length() == 1) { // file path (or windows drive letter)
            this.file = new File(uri.toString());
            this.uri = file.toURI();
        } else {
            this.uri = uri;
            this.file = null;
        }
        this.json = null;
    }

    public $RefParser(String json) {
        this.json = json;
        this.file = null;
        this.uri = null;
    }

    public $RefParser(String json, URI uri) {
        this.file = null;
        this.uri = uri;
        this.json = json;
    }


    public $RefParser parse() throws IOException {
        if(file != null) {
            refs = new $Refs(Parser.parse(file), uri);
        } else if (uri != null) {
            if (uri.getScheme() != null && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                var text = this.resolvers.get(RefFormat.URL).resolve($Ref.of(uri.toURL().toExternalForm(), uri));
                refs = new $Refs(Parser.parse(text), uri);
            } else {
                refs = new $Refs(Parser.parse(uri), uri);
            }
        } else {
            refs = new $Refs(Parser.parse(json));
        }
        return this;
    }

    public $RefParser withResourceClassLoader(ClassLoader resourceClassLoader) {
        this.resourceClassLoader = resourceClassLoader;
        Parser.withResourceClassLoader(resourceClassLoader);
        var classpathResolver = this.resolvers.get(RefFormat.CLASSPATH);
        if(classpathResolver instanceof ClasspathResolver) {
            ((ClasspathResolver) classpathResolver).withResourceClassLoader(resourceClassLoader);
        }
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
        this.refs.addPath(uri);
        this.visited.clear();
        dereference(refs.jsonContext, refs.schema(), new String[0], uri);
        return this;
    }

    public $RefParser mergeAllOf() {
        this.refs.addPath(uri);
        this.visited.clear();
        mergeAllOf(refs.schema(), new String[0], uri);
        return this;
    }

    private void mergeAllOf(Object value, String[] paths, URI currentFileURL) {
//        var visitedNodeRef = String.format("%s%s", currentFileURL, jsonPointer(paths));
//        log.trace("{}visiting {}", indent(), visitedNodeRef);
//        if(visited.contains(value)) {
//            log.trace("{}skipping visited {}", indent(), visitedNodeRef);
//            return;
//        }
//        visited.add(value);
        if(paths.length > 0 && "allOf".equals(paths[paths.length -1])) {
            List allOf = (List) value;
            String[] jsonPaths = Arrays.copyOf(paths, paths.length -1);
            String jsonPath = jsonPath(jsonPaths);
            Map<String, Object> originalAllOfRoot = refs.jsonContext.read(jsonPath);

            AllOfObject allOfObject = new AllOfObject();
            merge(allOfObject, originalAllOfRoot);
            for (int i = 0; i < allOf.size(); i++) {
                if(allOf.get(i) instanceof Map) {
                    Map<String, Object> item = (Map<String, Object>) allOf.get(i);
                    merge(allOfObject, item);
                } else {
                    throw new RuntimeException("Could not understand allOf: " + allOf.get(i));
                }
            }

            try {
                var mergedAllOfObject = allOfObject.buildAllOfObject();
                $Ref originalRef = refs.getOriginalRef(originalAllOfRoot);
                if (originalRef != null) {
                    refs.saveOriginalRef(originalRef, mergedAllOfObject);
                }
                refs.jsonContext.set(jsonPath, mergedAllOfObject);
                refs.saveOriginalAllOf(mergedAllOfObject, allOf);
            } catch (Exception e){
                log.error("Error setting jsonPath:{} in file:{}", jsonPath, currentFileURL, e);
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

    private void merge(AllOfObject allOfObject, List<Map<String, Object>> items) {
        for (Map<String, Object> innerItem : items) {
            merge(allOfObject, innerItem);
        }
    }

    private void merge(AllOfObject allOfObject, Map<String, Object> item) {
        if(item.keySet().size() == 1 && item.containsKey("allOf")) {
            List<Map<String, Object>> items = (List) item.get("allOf");
            merge(allOfObject, items);
        } else {
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                if(entry.getKey().equals("allOf")) {
                    merge(allOfObject, (List) item.get("allOf"));
                } else {
                    allOfObject.allOf.put(entry.getKey(), entry.getValue());
                }
            }
            if(item.containsKey("properties")) {
                allOfObject.properties.putAll((Map) item.get("properties"));
            }
            if(item.containsKey("required")) {
                allOfObject.required.addAll((List) item.get("required"));
            }
        }
    }

    private static class AllOfObject {
        Map<String, Object> allOf = new LinkedHashMap<>();
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();


        Map<String, Object> buildAllOfObject() {
            Map<String, Object> allOfObject = new LinkedHashMap<>(allOf);
            if(!required.isEmpty()) {
                allOfObject.put("required", required);
            }
            if(!properties.isEmpty()) {
                allOfObject.put("properties", properties);
            }
            return allOfObject;
        }
    }

    private List<Object> visited = new ArrayList<>();
    private List<String> indent = new ArrayList<>();
    private String indent() {
        return StringUtils.join(indent, "");
    }
    private void dereference(ExtendedJsonContext jsonContext, Object value, String[] paths, URI currentFileURL) {
        var visitedNodeRef = String.format("%s%s", currentFileURL, jsonPointer(paths));
        log.trace("{}visiting {}", indent(), visitedNodeRef);
        if(visited.contains(visitedNodeRef)) {
            log.trace("{}skipping visited {}", indent(), visitedNodeRef);
            return;
        }
        visited.add(visitedNodeRef);
        if(paths.length > 0 && "$ref".equals(paths[paths.length -1])) {
            $Ref $ref = $Ref.of((String) value, currentFileURL);
            boolean isCircular = (jsonPointer(paths) + "/").startsWith($ref.getPath() + "/") && ($ref.getURI() == null || $ref.getURI().equals(currentFileURL));
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
            Object resolved = null;
            try {
                resolved = dereference($ref, jsonContext, currentFileURL);
            } catch (Resolver.MissingResourceException e) {
                if(options != null && $RefParserOptions.OnMissing.SKIP == options.onMissing) {
                    log.warn("Skipping missing reference {}", $ref);
                    return;
                }
                throw e;
            }
            indent.remove(indent.size() -1);
            // dereference resolved
            var resolvedRefURL = ObjectUtils.firstNonNull($ref.getURI(), currentFileURL);
            var resolvedNodePaths = jsonPointerToPaths($ref.getPath());
            var resolvedNodeRef =  String.format("%s%s", resolvedRefURL, $ref.getPath());
            indent.add(" => ");
            log.trace("{}dereferencing resolved {}", indent(), resolvedNodeRef);
            dereference(jsonContext, resolved, resolvedNodePaths, resolvedRefURL);
            indent.remove(indent.size() -1);

            try {
                log.trace("{}setting resolved value at {} {}", indent(), innerJsonPath, currentFileURL);
                resolved = dereference($ref, jsonContext, currentFileURL);
                this.refs.saveOriginalRef($ref, resolved);
                // jsonContext.set(innerJsonPath, resolved);
                replaceWith$Ref(jsonContext, innerJsonPath, resolved);
            }catch (Exception e){
                log.error("Error setting jsonPath: {} in {}", innerJsonPath,  currentFileURL, e);
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

    private void replaceWith$Ref(ExtendedJsonContext jsonContext, String jsonPath, Object resolved) {
        Map<String, Object> original = jsonContext.read(jsonPath);
        Map<String, Object> replacement = new LinkedHashMap<>();
        replacement.putAll(original);
        replacement.remove("$ref");
        replacement.putAll((Map) resolved);
        jsonContext.set(jsonPath, replacement);
    }

    private Object dereference($Ref $ref, ExtendedJsonContext jsonContext, URI currentFileURL)  {
        this.refs.addRef($ref.getRef());
        // resolve external file
        if($ref.getRefFormat().isAnExternalRefFormat()) {
            this.refs.addPath($ref.getURI());
            String refUrl = $ref.getURI().toString();
            if(urlJsonContextCache.containsKey(refUrl)) {
                jsonContext = urlJsonContextCache.get(refUrl);
            } else {
                Resolver resolver = getResolver($ref.getRefFormat(), $ref.getURI());
                String resolved = resolver.resolve($ref);
                jsonContext = Parser.parse(resolved);
                urlJsonContextCache.put(refUrl, jsonContext);
                this.refs.addJsonContext($ref.getURI(), jsonContext);
                if(jsonContext.json() instanceof Map || jsonContext.json() instanceof List) {
                    // log.trace("dereferencing resolved {}", $ref);
                    dereference(jsonContext, jsonContext.json(), new String[0], $ref.getURI());
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

    protected Resolver getResolver(RefFormat refFormat, URI currentURL) {
        if(refFormat == RefFormat.RELATIVE) {
            return getResolver(RefFormat.of(currentURL.toString()), currentURL);
        }
        return resolvers.get(refFormat);
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
