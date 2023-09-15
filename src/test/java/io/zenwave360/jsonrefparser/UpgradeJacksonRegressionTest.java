package io.zenwave360.jsonrefparser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class UpgradeJacksonRegressionTest {

    @Test
    public void testUpgradeJacksonRegressionTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        UntypedObjectDeserializer deserializer = new UntypedObjectDeserializer() {
            @Override
            public Object deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
                com.fasterxml.jackson.core.JsonStreamContext context = jsonParser.getParsingContext();
                System.out.println("Context " + context.toString() + " - " + jsonParser.getCurrentLocation());
                return super.deserialize(jsonParser, ctxt);
            }
        };
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Object.class, deserializer);
        mapper.registerModule(module);

        File file = new File("src/test/resources/asyncapi/shoping-cart-multiple-files/shoping-cart-multiple-files.yml");
        Map result = mapper.readValue(file, Map.class);
    }
}
