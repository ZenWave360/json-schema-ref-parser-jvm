package io.zenwave360.jsonrefparser;

import org.junit.Test;

import java.io.File;

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
        assertTrue(refs.schema().containsKey("properties"));
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }
}
