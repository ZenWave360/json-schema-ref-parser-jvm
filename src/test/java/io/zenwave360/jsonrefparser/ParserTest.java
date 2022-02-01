package io.zenwave360.jsonrefparser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ParserTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Test
    public void testParseAsyncapiNestedSchemas() throws IOException {
        File file = new File("src/test/resources/asyncapi/schemas/json-schemas-payload.yml");
        $RefParser parser = new $RefParser(file);
        $Refs refs = parser.dereference();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }

    @Test
    public void testParseAsyncapiNestedSchemasExternalRef() throws IOException {
        File file = new File("src/test/resources/asyncapi/schemas/json-schemas-external-ref.yml");
        $RefParser parser = new $RefParser(file);
        $Refs refs = parser.dereference();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }

    @Test
    public void testParseAsyncapiShoppingCartWithAvros() throws IOException {
        File file = new File("src/test/resources/asyncapi/shoping-cart-multiple-files/shoping-cart-multiple-files.yml");
        $RefParser parser = new $RefParser(file);
        $Refs refs = parser.dereference();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }

    @Test
    public void testParseAsyncapiShoppingCartWithAvroArray() throws IOException {
        File file = new File("src/test/resources/asyncapi/shoping-cart-avro-array/shoping-cart.yml");
        $RefParser parser = new $RefParser(file);
        $Refs refs = parser.dereference();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }

    @Test
    public void testParseAsyncapiJsonSchema() throws IOException {
        File file = new File("src/test/resources/asyncapi/transport+jsonschema/asyncapi.yml");
        $RefParser parser = new $RefParser(file);
        $Refs refs = parser.dereference();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }

    @Test
    public void testParseAsyncapiCircular() throws IOException {
        File file = new File("src/test/resources/asyncapi/circular-refs/asyncapi.yml");
        {
            $RefParser parser = new $RefParser(file);
            $Refs refs = parser.dereference();
            Assert.assertTrue(refs.circular);
        }

        {
            $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions($RefParserOptions.OnCircular.SKIP));
            $Refs refs = parser.dereference();
            Assert.assertFalse(refs.circular);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        }

        {
            $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions($RefParserOptions.OnCircular.FAIL));
            try {
                $Refs refs = parser.dereference();
                Assert.fail("Circular references should not be allowed");
            } catch (RuntimeException e) {
                Assert.assertTrue(e.getMessage().contains("Circular references not allowed"));
            }
        }
    }

}
