package io.zenwave360.jsonrefparser;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.internal.JsonContext;
import io.zenwave360.jsonrefparser.parser.ExtendedJsonContext;
import io.zenwave360.jsonrefparser.parser.Parser;
import io.zenwave360.jsonrefparser.resolver.FileResolver;
import io.zenwave360.jsonrefparser.resolver.HttpResolver;
import io.zenwave360.jsonrefparser.resolver.RefFormat;
import io.zenwave360.jsonrefparser.resolver.Resolver;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.math.NumberUtils.isDigits;

public class $RefParser {

    private static final Logger log = LoggerFactory.getLogger($RefParser.class);

    public final URL url;

    public final $Refs refs;
    public final Map<RefFormat, Resolver> resolvers = new HashMap<>();
    {
        resolvers.put(RefFormat.RELATIVE, new FileResolver());
        resolvers.put(RefFormat.URL, new HttpResolver());
    }
    private Map<String, ExtendedJsonContext> cache = new HashMap<>();
    private List<AuthenticationValue> authenticationValues = new ArrayList<>();

    private $RefParserOptions options;

    public $RefParser(File file) throws IOException {
        this.url = file.toURI().toURL();
        refs = new $Refs(Parser.parse(file), url);
    }


    public $RefParser(String json) {
        refs = new $Refs(Parser.parse(json));
        url = null;
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
        mergeAllOf(refs.jsonContext, refs.schema(), new String[0], url);
        return this;
    }

    private void mergeAllOf(ExtendedJsonContext jsonContext, Object value, String[] paths, URL currentFileURL) {
        if(paths.length > 0 && "allOf".equals(paths[paths.length -1])) {
            List allOf = (List) value;
            Map<String, Object> mergedAllOfObject = new HashMap<>();
            for (int i = 0; i < allOf.size(); i++) {
                if(allOf.get(i) instanceof Map) {
                    Map<String, Object> item = (Map<String, Object>) allOf.get(i);
                    mergedAllOfObject.putAll(item);
                } else {
                    throw new RuntimeException("Could not understand allOf: " + allOf.get(i));
                }
            }
            String[] jsonPaths = Arrays.copyOf(paths, paths.length -1);
            String jsonPath = jsonPath(jsonPaths);
            try {
                jsonContext.set(jsonPath, mergedAllOfObject);
            } catch (Exception e){
                log.error("Error setting jsonPath:{} in file:{}", jsonPath, currentFileURL.toExternalForm(), e);
                throw e;
            }
        } else if(value instanceof Map) {
            // visit
            ((Map<String, Object>) value).entrySet().forEach(e -> {
                mergeAllOf(jsonContext, e.getValue(), ArrayUtils.add(paths, e.getKey()), currentFileURL);
            });
        } else if(value instanceof List) {
            // visit
            List list = (List) value;
            for (int i = 0; i < list.size(); i++) {
                mergeAllOf(jsonContext, list.get(i), ArrayUtils.add(paths, i + ""), currentFileURL);
            }
        }
    }

    private void dereference(ExtendedJsonContext jsonContext, Object value, String[] paths, URL currentFileURL) {
         if(paths.length > 0 && "$ref".equals(paths[paths.length -1])) {
            $Ref $ref = $Ref.of((String) value, currentFileURL);
            // check if is circular
            if(jsonPointer(paths).startsWith($ref.getPath() + "") && ($ref.getUrl() == null || $ref.getUrl().equals(currentFileURL))) {
                if(options != null && $RefParserOptions.OnCircular.FAIL == options.onCircular) {
                    throw new RuntimeException("Failing: Circular references not allowed " + $ref);
                }
                if(options != null && $RefParserOptions.OnCircular.SKIP == options.onCircular) {
                    return;
                }
                this.refs.circular = true;
            }

            // do dereference
            Object resolved = dereference($ref, jsonContext);
            this.refs.saveOriginalRef($ref, resolved);
            String[] jsonPaths = Arrays.copyOf(paths, paths.length -1);
            String jsonPath = jsonPath(jsonPaths);
            try {
                jsonContext.set(jsonPath, resolved);
            }catch (Exception e){
                log.error("Error setting jsonPath: {} in {}", jsonPath,  currentFileURL.toExternalForm(), e);
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

    private Object dereference($Ref $ref, ExtendedJsonContext jsonContext) {
        this.refs.addRef($ref.getRef());
        // resolve external file
        if($ref.getRefFormat().isAnExternalRefFormat()) {
            this.refs.addPath($ref.getUrl());
            String refUrl = $ref.getUrl().toExternalForm();
            if(cache.containsKey(refUrl)) {
                jsonContext = cache.get(refUrl);
            } else {
                Resolver resolver = resolvers.get($ref.getRefFormat());
                String resolved = resolver.resolve($ref);
                jsonContext = Parser.parse(resolved);
                cache.put(refUrl, jsonContext);
                this.refs.addJsonContext($ref.getUrl(), jsonContext);
                if(jsonContext.json() instanceof Map || jsonContext.json() instanceof List) {
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
        return "#/" + StringUtils.join(paths, "/");
    }
}
