package io.zenwave360.jsonrefparser;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class Issue11Test {

    @Test
    public void testIssue11() throws IOException {
        String schema = "{\n" +
                "        \"$schema\" : \"http://json-schema.org/draft-07/schema#\",\n" +
                "                \"type\" : \"object\",\n" +
                "                \"properties\" : {\n" +
                "            \"referingProperty\" : {\n" +
                "                \"$ref\" : \"https://json.schemastore.org/base.json#/definitions/nullable-boolean\",\n" +
                "                        \"title\" : \"referingProperty\",\n" +
                "                        \"description\" : \"A property that is refering to the base.json of json.schemastore.org\"\n" +
                "            }\n" +
                "        }\n" +
                "    }";

        $RefParser parser = new $RefParser(schema)
                .withOptions(new $RefParserOptions().withOnCircular($RefParserOptions.OnCircular.SKIP));
        $Refs refs = parser.parse().dereference().getRefs();
        Map<String, Object> properties = (Map<String, Object>) refs.get("properties");
        Map<String, Object>referingProperty = (Map<String, Object>) properties.get("referingProperty");
        for(var value : referingProperty.entrySet()){
            System.out.println(value);
        }
    }
}
