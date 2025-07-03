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
        Assert.assertTrue(schema.contains("lorem={title=lorem"));
        Assert.assertTrue(schema.contains("ipsum={title=ipsum"));
        System.out.println(refs.schema());
    }
}
