package io.zenwave360.jsonrefparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

public class GH36Test {

    @Test
    public void test() throws Exception {
        $RefParser refParser = new $RefParser(new File("src/test/resources/GH-36/root.json"));

        $Refs refs = refParser.parse().dereference().mergeAllOf().getRefs();
        var schema = refs.schema();
        Collection<String> required = (Collection<String>) schema.get("required");
        Assert.assertEquals(2, required.size());
        Assert.assertTrue(required.contains("ingressDomain"));
        Assert.assertTrue(required.contains("a"));
    }
}
