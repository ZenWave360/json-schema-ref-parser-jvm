package io.zenwave360.jsonrefparser;

import com.jayway.jsonpath.internal.JsonContext;
import io.zenwave360.jsonrefparser.parser.Parser;
import io.zenwave360.jsonrefparser.resolver.FileResolver;
import io.zenwave360.jsonrefparser.resolver.HttpResolver;
import io.zenwave360.jsonrefparser.resolver.RefFormat;
import io.zenwave360.jsonrefparser.resolver.Resolver;
import io.zenwave360.jsonrefparser.utils.AuthenticationValue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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

    public final URL url;

    public final $Refs refs;
    public final Map<RefFormat, Resolver> resolvers = new HashMap<>();
    {
        resolvers.put(RefFormat.RELATIVE, new FileResolver());
        resolvers.put(RefFormat.URL, new HttpResolver());
    }
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

    public $Refs dereference() {
        this.refs.addPath(url);
        visit(refs.jsonContext, refs.schema(), new String[0], url);
        return refs;
    }

    private void visit(JsonContext jsonContext, Object value, String[] paths, URL currentFileURL) {
        if(value instanceof Map) {
            final Map<String, Object> map = (Map) value;
            map.entrySet().forEach(e -> {
                visit(jsonContext, e.getValue(), ArrayUtils.add(paths, e.getKey()), currentFileURL);
            });
        } else if(value instanceof List) {
            List list = (List) value;
            for (int i = 0; i < list.size(); i++) {
                visit(jsonContext, list.get(i), ArrayUtils.add(paths, i + ""), currentFileURL);
            }
        } else if(value instanceof String){
            if("$ref".equals(paths[paths.length -1])) {
                $Ref $ref = $Ref.of((String) value, currentFileURL);
                if(jsonPointer(paths).startsWith($ref.getPath() + "") && ($ref.getUrl() == null || $ref.getUrl().equals(currentFileURL))) {
                    if(options != null && $RefParserOptions.OnCircular.FAIL == options.onCircular) {
                        throw new RuntimeException("Failing: Circular references not allowed " + $ref);
                    }
                    if(options != null && $RefParserOptions.OnCircular.SKIP == options.onCircular) {
                        return;
                    }
                    this.refs.circular = true;
                }

                Object resolved = resolve($ref);
                this.refs.saveOriginalRef($ref, resolved);
                String[] jsonPaths = Arrays.copyOf(paths, paths.length -1);
                String jsonPath = jsonPath(jsonPaths);
                try {
                    jsonContext.set(jsonPath, resolved);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private Map<String, JsonContext> cache = new HashMap<>();

    private Object resolve($Ref $ref) {
        this.refs.addRef($ref.getRef());
        JsonContext jsonContext = this.refs.jsonContext;
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
                if(jsonContext.json() instanceof Map || jsonContext.json() instanceof List) {
                    visit(jsonContext, jsonContext.json(), new String[0], $ref.getUrl());
                }
            }

        }
        // resolve internal path
        if(StringUtils.isNotBlank($ref.getPath())) {
            String jsonPaths[] = $ref.getPath().replace($Ref.REFERENCE_SEPARATOR, "").split("/");
            Object resolved = jsonContext.read(jsonPath(jsonPaths));
            return resolved;
        }
        return jsonContext.json();
    }

    private String jsonPath(String[] paths) {
        return "$" + Arrays.stream(paths).map(path -> isDigits(path)? "[" + path + "]" : "['" + path + "']").collect(Collectors.joining());
    }

    private String jsonPointer(String[] paths) {
        return "#/" + StringUtils.join(paths, "/");
    }
}
