package io.zenwave360.jsonrefparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class GH18 {

    @Test
    public void test() throws Exception {
        $RefParser refParser = new $RefParser(new File("src/test/resources/GH-18.json"));
        refParser.withOptions(new $RefParserOptions().withOnCircular($RefParserOptions.OnCircular.SKIP));
        $Refs refs = refParser.parse().dereference().getRefs();
        String schema = refs.schema().toString();
        System.out.println("Actual schema output:");
        System.out.println(schema);
        System.out.println("Contains 'lorem={title=lorem': " + schema.contains("lorem={title=lorem"));
        System.out.println("Contains 'ipsum={title=ipsum': " + schema.contains("ipsum={title=ipsum"));
        Assert.assertTrue(schema.contains("lorem={title=lorem"));
        Assert.assertTrue(schema.contains("ipsum={title=ipsum"));
    }
}
