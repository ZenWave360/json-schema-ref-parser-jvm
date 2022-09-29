package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import io.zenwave360.jsonrefparser.resolver.ClasspathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class Parser {

    private static final Logger log = LoggerFactory.getLogger(Parser.class);

    private static ClassLoader resourceClassLoader = Parser.class.getClassLoader();
    public static void withResourceClassLoader(ClassLoader resourceClassLoader) {
        if(resourceClassLoader != null) {
            Parser.resourceClassLoader = resourceClassLoader;
        }
    }

    public static ExtendedJsonContext parse(URI uri) throws IOException {
        if("classpath".contentEquals(uri.getScheme())) {
            return parse(resourceClassLoader.getResourceAsStream(uri.getPath().replaceFirst("^/", "")));
        }
        // TODO: it does not support yet parsing http/https files directly
        return parse(new FileInputStream(new File(uri)));
    }

    public static ExtendedJsonContext parse(File file) throws IOException {
        return parse(new FileInputStream(file));
    }

    public static ExtendedJsonContext parse(String content) {
        try {
            return parse(new ByteArrayInputStream(content.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ExtendedJsonContext parse(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new CustomYAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        JsonDeserializerWithLocations jsonDeserializerWithLocations = new JsonDeserializerWithLocations();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Object.class, jsonDeserializerWithLocations);
        mapper.registerModule(module);

        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new CustomJacksonJsonProvider(mapper);
            private final MappingProvider mappingProvider = new JacksonMappingProvider(mapper);

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
        var content = new String(inputStream.readAllBytes());
        var parsed = JsonPath.parse(content);
        ExtendedJsonContext.of(parsed, jsonDeserializerWithLocations.getLocations()).toString();
        return ExtendedJsonContext.of(parsed, jsonDeserializerWithLocations.getLocations());
    }
}
