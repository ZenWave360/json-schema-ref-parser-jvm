package io.zenwave360.jsonrefparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class DereferenceJsonStringTest {

    @Test
    public void testDereferenceJsonString() throws IOException {
        String json = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"definitions\": {\n" +
                "    \"appMethods\": {\n" +
                "      \"about\": {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"author\": {\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"$ref\": \"#/definitions/appMethods\"\n" +
                "  }\n" +
                "}";
        $RefParser parser = new $RefParser(json).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertEquals(1, refs.refs().size());
        Assert.assertTrue(refs.paths("file").isEmpty());
        Assert.assertFalse(refs.refs("#/").isEmpty());
        Assert.assertTrue(refs.get("$.definitions.appMethods") != null);
    }
}
