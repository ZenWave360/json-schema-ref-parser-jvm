package io.zenwave360.jsonrefparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class GH22Test {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testLocalSchemaWithPatternProperties() throws IOException {
        File file = new File("src/test/resources/schema-with-pattern-properties.json");
        final var parser = new $RefParser(file);
        final var schema = parser
                .parse()
                .dereference()
                .mergeAllOf()
                .getRefs().schema();

        Assert.assertNotNull(schema);

        // Verify the schema has the expected structure
        Assert.assertTrue(schema.containsKey("properties"));
        Assert.assertTrue(schema.containsKey("patternProperties"));
        Assert.assertTrue(schema.containsKey("definitions"));

        // Verify patternProperties contains the expected pattern
        Map<String, Object> patternProperties = (Map<String, Object>) schema.get("patternProperties");
        Assert.assertTrue(patternProperties.containsKey("^x-[\\w\\d\\.\\x2d_]+$"));

        // Verify the pattern property has been dereferenced (should contain the extension definition)
        Map<String, Object> extensionPattern = (Map<String, Object>) patternProperties.get("^x-[\\w\\d\\.\\x2d_]+$");
        Assert.assertNotNull(extensionPattern);
        Assert.assertEquals("Any property starting with x- is valid.", extensionPattern.get("description"));

        // Verify nested patternProperties in info definition
        Map<String, Object> definitions = (Map<String, Object>) schema.get("definitions");
        Map<String, Object> infoDefinition = (Map<String, Object>) definitions.get("info");
        Map<String, Object> infoPatternProperties = (Map<String, Object>) infoDefinition.get("patternProperties");
        Assert.assertTrue(infoPatternProperties.containsKey("^x-[\\w\\d\\.\\x2d_]+$"));

        // Print the schema for debugging
//        System.out.println("Dereferenced schema:");
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
    }
}
