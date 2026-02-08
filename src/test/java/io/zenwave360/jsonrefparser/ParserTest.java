package io.zenwave360.jsonrefparser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.zenwave360.jsonrefparser.$RefParserOptions.OnMissing;
import io.zenwave360.jsonrefparser.resolver.HttpResolver;
import io.zenwave360.jsonrefparser.resolver.Resolver;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.FAIL;

public class ParserTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        SimpleModule module = new SimpleModule();
        module.addSerializer((Class<Map<?, ?>>) (Class<?>) Map.class, new CycleBreakingMapSerializer());
        mapper.registerModule(module);
    }

    private void assertNoRefs(Object object) throws JsonProcessingException {
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        boolean hasRefs = json.contains("$ref");
        if(hasRefs) {
            System.out.println(json);
        }
        Assert.assertFalse(hasRefs);
    }



    private void assertNoAllOfs(Object object) throws JsonProcessingException {
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        boolean hasRefs = json.contains("allOf");
        if(hasRefs) {
            System.out.println(json);
        }
        Assert.assertFalse(hasRefs);
    }

    @Test
    public void testDereferenceAsyncapiNestedSchemas() throws IOException {
        File file = new File("src/test/resources/asyncapi/schemas/json-schemas-payload.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
        Assert.assertFalse(refs.paths("file").isEmpty());
        Assert.assertFalse(refs.refs("#/").isEmpty());
        Assert.assertTrue(refs.get("$.components.messages.People") != null);
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAsyncapiNestedSchemasExternalRef() throws IOException {
        File file = new File("src/test/resources/asyncapi/schemas/json-schemas-external-ref.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAsyncapiNestedSchemasExternalRef_with_ClasspathResolver() throws IOException {
        URI url = URI.create("classpath:/asyncapi/schemas/json-schemas-external-ref.yml");
        $RefParser parser = new $RefParser(url).withResourceClassLoader(getClass().getClassLoader()).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAsyncapiNestedSchemasExternalRef_with_ClasspathResolver_NoSlash() throws IOException {
        URI url = URI.create("classpath:asyncapi/schemas/json-schemas-external-ref.yml");
        $RefParser parser = new $RefParser(url).withResourceClassLoader(getClass().getClassLoader()).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAsyncapiShoppingCartWithAvros() throws IOException {
        File file = new File("src/test/resources/asyncapi/shoping-cart-multiple-files/shoping-cart-multiple-files.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAsyncapiShoppingCartWithAvrosAndLocationRanges() throws IOException {
        File file = new File("src/test/resources/asyncapi/shoping-cart-multiple-files/shoping-cart-multiple-files.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
        var serverLocations = refs.getJsonLocationRange("$.servers");
        Assert.assertEquals(8, serverLocations.getLeft().getLineNr());
        Assert.assertEquals(3, serverLocations.getLeft().getColumnNr());
        Assert.assertEquals(18, serverLocations.getRight().getLineNr());
        Assert.assertEquals(1, serverLocations.getRight().getColumnNr());
        File avro = new File("src/test/resources/asyncapi/shoping-cart-multiple-files/add_cart_lines.avsc");
        var avroLocations = refs.getJsonLocationRange(avro.toURI(), "$.fields");
        Assert.assertEquals(6, avroLocations.getLeft().getLineNr());
        Assert.assertEquals(15, avroLocations.getLeft().getColumnNr());
        Assert.assertEquals(17, avroLocations.getRight().getLineNr());
        Assert.assertEquals(6, avroLocations.getRight().getColumnNr());
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testParseAvrosAndLocationRanges() throws IOException {
        File file = new File("src/test/resources/asyncapi/shoping-cart-multiple-files/add_cart_lines.avsc");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        var avroLocations = refs.getJsonLocationRange("$.fields");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        Assert.assertEquals(6, avroLocations.getLeft().getLineNr());
        Assert.assertEquals(15, avroLocations.getLeft().getColumnNr());
        Assert.assertEquals(17, avroLocations.getRight().getLineNr());
        Assert.assertEquals(6, avroLocations.getRight().getColumnNr());
    }

    @Test
    public void testDereferenceAsyncapiShoppingCartWithAvroArray() throws IOException {
        File file = new File("src/test/resources/asyncapi/shoping-cart-avro-array/shoping-cart.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAsyncapiJsonSchema() throws IOException {
        File file = new File("src/test/resources/asyncapi/transport+jsonschema/asyncapi.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAndMergeAllOf() throws IOException {
        File file = new File("src/test/resources/openapi/allOf.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
//        Assert.assertFalse(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAndMergeAllOf_AssertOriginalRef() throws IOException {
        File file = new File("src/test/resources/asyncapi/original-ref/asyncapi-original-ref.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        //        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAndMergeAllOfHttp() throws IOException {
        var uri = URI.create("https://raw.githubusercontent.com/ZenWave360/json-schema-ref-parser-jvm/refs/heads/main/src/test/resources/openapi/allOf.yml");
        $RefParser parser = new $RefParser(uri).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        //        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAndMerge_MultipleAllOf() throws IOException {
        File file = new File("src/test/resources/asyncapi/multiple-allOf.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();

        assertNoRefs(refs.schema());
        assertNoAllOfs(refs.schema());
        
        // Test merged properties from Test1 (test1a, test1b)
        var test1Properties = (Map) refs.get("$.components.schemas.Test1.properties");
        Assert.assertEquals(2, test1Properties.size());
        Assert.assertTrue(test1Properties.containsKey("test1a"));
        Assert.assertTrue(test1Properties.containsKey("test1b"));
        Assert.assertEquals("string", ((Map) test1Properties.get("test1a")).get("type"));
        Assert.assertEquals("string", ((Map) test1Properties.get("test1b")).get("type"));
        
        // Test merged properties from Test2 (test2a, test2b)
        var test2Properties = (Map) refs.get("$.components.schemas.Test2.properties");
        Assert.assertEquals(2, test2Properties.size());
        Assert.assertTrue(test2Properties.containsKey("test2a"));
        Assert.assertTrue(test2Properties.containsKey("test2b"));
        
        // Test final merged Test schema contains all 4 properties
        var properties = (Map) refs.get("$.components.schemas.Test.properties");
        Assert.assertEquals(4, properties.size());
        Assert.assertTrue(properties.containsKey("test1a"));
        Assert.assertTrue(properties.containsKey("test1b"));
        Assert.assertTrue(properties.containsKey("test2a"));
        Assert.assertTrue(properties.containsKey("test2b"));
        
        // Verify Test schema maintains its object type
        var testSchema = (Map) refs.get("$.components.schemas.Test");
        Assert.assertEquals("object", testSchema.get("type"));
        Assert.assertFalse(testSchema.containsKey("allOf"));
    }

    @Test
    public void testDereferenceAndMerge_MultipleAllOf2() throws IOException {
        File file = new File("src/test/resources/asyncapi/multiple-allOf2.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();

        assertNoRefs(refs.schema());
        assertNoAllOfs(refs.schema());
        
        // Test Test1b has merged properties (test1b, test1c)
        var test1bProperties = (Map) refs.get("$.components.schemas.Test1b.properties");
        Assert.assertEquals(2, test1bProperties.size());
        Assert.assertTrue(test1bProperties.containsKey("test1b"));
        Assert.assertTrue(test1bProperties.containsKey("test1c"));
        
        // Test Test1 has all 3 properties (test1a from itself + test1b, test1c from Test1b)
        var test1Properties = (Map) refs.get("$.components.schemas.Test1.properties");
        Assert.assertEquals(3, test1Properties.size());
        Assert.assertTrue(test1Properties.containsKey("test1a"));
        Assert.assertTrue(test1Properties.containsKey("test1b"));
        Assert.assertTrue(test1Properties.containsKey("test1c"));
        
        // Test final Test schema has all 5 properties
        var properties = (Map) refs.get("$.components.schemas.Test.properties");
        Assert.assertEquals(5, properties.size());
        Assert.assertTrue(properties.containsKey("test1a"));
        Assert.assertTrue(properties.containsKey("test1b"));
        Assert.assertTrue(properties.containsKey("test1c"));
        Assert.assertTrue(properties.containsKey("test2a"));
        Assert.assertTrue(properties.containsKey("test2b"));
        
        // Verify no circular references detected
        Assert.assertFalse(refs.circular);
    }

    @Test
    public void testDereferenceAndMergeChainedAllOf() throws IOException {
        File file = new File("src/test/resources/asyncapi/car-engine_chained_allOf.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        
        assertNoAllOfs(refs.schema());
        
        // Verify Car has all properties from Base, Engine, and Car
        var carProperties = (Map) refs.get("$.components.schemas.Car.properties");
        Assert.assertTrue(carProperties.containsKey("reference")); // from Base
        Assert.assertTrue(carProperties.containsKey("mileage")); // from Engine
        Assert.assertTrue(carProperties.containsKey("make")); // from Car
        
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }

    @Test
    public void testDereference() throws IOException {
        File file = new File("src/test/resources/openapi/allOf.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceHttpRefs() throws IOException {
        File file = new File("src/test/resources/openapi/http-external-refs.yml");
        $RefParser parser = new $RefParser(file);
        $Refs refs = parser.parse().dereference().mergeAllOf().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceHttpRefsSelfSignedCerts() throws IOException {
        System.setProperty(HttpResolver.TRUST_ALL, "true");
        File file = new File("src/test/resources/openapi/http-external-refs.yml");
        $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnCircular($RefParserOptions.OnCircular.SKIP)).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceHttpRefsWithHeaderAuthentication() throws IOException {
        File file = new File("src/test/resources/openapi/http-external-refs.yml");
        $RefParser parser = new $RefParser(file)
                .withAuthentication(new AuthenticationValue()
                        .withHeader("Basic: ")
                        .withUrlMatcher(url -> url.getHost().equals("raw.githubusercontent.com")))
                .withOptions(new $RefParserOptions().withOnCircular($RefParserOptions.OnCircular.SKIP))
                .parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceHttpRefsWithQueryAuthentication() throws IOException {
        File file = new File("src/test/resources/openapi/openapi-petstore-with-external-refs.yml");
        $RefParser parser = new $RefParser(file)
                .withAuthentication(new AuthenticationValue()
                        .withQueryParam("token", "blablabla")
                        .withUrlMatcher(url -> url.getHost().equals("petstore3.swagger.io")))
                .withOptions(new $RefParserOptions().withOnCircular($RefParserOptions.OnCircular.SKIP))
                .parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceAsyncapiCircular() throws IOException {
        File file = new File("src/test/resources/asyncapi/circular-refs/asyncapi.yml");
        {
            $RefParser parser = new $RefParser(file).parse();
            $Refs refs = parser.dereference().getRefs();
            Assert.assertTrue(refs.circular);
        }

        {
            $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnCircular($RefParserOptions.OnCircular.SKIP)).parse();
            $Refs refs = parser.dereference().getRefs();
            Assert.assertTrue(refs.circular);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        }

        {
            $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnCircular(FAIL)).parse();
            try {
                $Refs refs = parser.dereference().getRefs();
                Assert.fail("Circular references should not be allowed");
            } catch (RuntimeException e) {
                Assert.assertTrue(e.getMessage().contains("Circular references not allowed"));
            }
        }
    }

    @Test
    public void testDereferenceRecursive() throws IOException {
        File file = new File("src/test/resources/recursive/main.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertTrue(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
//        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceCircular() throws IOException {
        File file = new File("src/test/resources/recursive/auxiliar.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertTrue(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
//        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceRecursiveSimplest() throws IOException {
        File file = new File("src/test/resources/recursive/recursive-simplest.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertTrue(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        //        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceOnMissingSkip() throws IOException {
        File file = new File("src/test/resources/openapi/openapi-missing.yml");
        $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnMissing(OnMissing.SKIP)).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertEquals("missing.yaml#/responses/200", refs.get("$.paths./product.get.responses.200.$ref"));
        Assert.assertEquals("https://github.com/ZenWave360/missing.yaml#/responses/500", refs.get("$.paths./product.get.responses.500.$ref"));
    }

    @Test
    public void testDereferenceOnMissingFail() throws IOException {
        File file = new File("src/test/resources/openapi/openapi-missing.yml");
        $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnMissing(OnMissing.FAIL)).parse();
        try {
            $Refs refs = parser.dereference().getRefs();
            Assert.fail("Missing references should not be allowed");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof Resolver.MissingResourceException);
        }
    }

    @Test
    public void testMergeAllOfRecursive() throws IOException {
        File file = new File("src/test/resources/asyncapi/orders-model.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        Assert.assertTrue(refs.circular);
        assertNoRefs(refs.schema());
        assertNoAllOfs(refs.schema());
    }

    @Test
    public void testDetectCircularRecursive() throws IOException {
        File file = new File("src/test/resources/asyncapi/orders-model.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertTrue(refs.circular);
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }


    class CycleBreakingMapSerializer extends JsonSerializer<Map<?, ?>> {
        private final ThreadLocal<Set<Object>> SEEN =
                ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

        @Override
        public void serialize(Map<?, ?> map, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            Set<Object> seen = SEEN.get();
            if (seen.contains(map)) {
                gen.writeString("CYCLE");  // O gen.writeNull() para omitir
                return;
            }
            seen.add(map);
            try {
                gen.writeStartObject();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    gen.writeFieldName(entry.getKey().toString());
                    Object value = entry.getValue();
                    provider.defaultSerializeValue(value, gen);  // Usa serializador recursivo para valores Map
                }
                gen.writeEndObject();
            } finally {
                seen.remove(map);
            }
        }
    }
}
