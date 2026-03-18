package io.zenwave360.jsonrefparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import static org.junit.Assert.assertTrue;

public class Gh32MergeAllOfAtRootTest {

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    public void testMergeAllOfAtRoot() throws Exception {
        File file = new File("src/test/resources/gh-32-mergeAllOf-root.yml");
        $RefParser parser = new $RefParser(file).parse();
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        
        // Verify allOf is removed
        Assert.assertFalse(refs.schema().containsKey("allOf"));
        
        // Verify properties from both Core and Extension are merged
        assertTrue(refs.schema().containsKey("properties"));
        var properties = (Map) refs.schema().get("properties");
        Assert.assertEquals(2, properties.size());
        Assert.assertTrue(properties.containsKey("prop1"));
        Assert.assertTrue(properties.containsKey("prop2"));
        
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }
}
