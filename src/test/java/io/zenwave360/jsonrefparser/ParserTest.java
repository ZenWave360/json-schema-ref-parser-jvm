package io.zenwave360.jsonrefparser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.jsonrefparser.resolver.HttpResolver;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.FAIL;
import static io.zenwave360.jsonrefparser.$RefParserOptions.OnCircular.SKIP;

public class ParserTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
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
        $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnCircular(SKIP)).parse();
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
                .withOptions(new $RefParserOptions().withOnCircular(SKIP))
                .parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        Assert.assertFalse(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
    }

    @Test
    public void testDereferenceHttpRefsWithQueryAuthentication() throws IOException {
        File file = new File("src/test/resources/openapi/http-external-refs.yml");
        $RefParser parser = new $RefParser(file)
                .withAuthentication(new AuthenticationValue()
                        .withQueryParam("token", "blablabla")
                        .withUrlMatcher(url -> url.getHost().equals("raw.githubusercontent.com")))
                .withOptions(new $RefParserOptions().withOnCircular(SKIP))
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
            $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions().withOnCircular(SKIP)).parse();
            $Refs refs = parser.dereference().getRefs();
            Assert.assertFalse(refs.circular);
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
    @Ignore
    public void testMergeAllOfRecursive() throws IOException {
        File file = new File("src/test/resources/asyncapi/orders-model.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        assertNoRefs(refs.schema());
        assertNoAllOfs(refs.schema());
    }

    @Test
    @Ignore
    public void testDetectCircularRecursive() throws IOException {
        File file = new File("src/test/resources/asyncapi/orders-model.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().getRefs();
        Assert.assertTrue(refs.circular);
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
        //        assertNoRefs(refs.schema());
    }

}
