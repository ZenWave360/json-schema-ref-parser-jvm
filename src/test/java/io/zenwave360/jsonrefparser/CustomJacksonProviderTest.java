package io.zenwave360.jsonrefparser;

import com.jayway.jsonpath.JsonPath;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CustomJacksonProviderTest {

    @Test
    public void testAccesingIndexedJsonPathWithAndWithOutQuotes() throws IOException {
        File file = new File("src/test/resources/indexed-array.yml");
        $RefParser parser = new $RefParser(file);
        $Refs refs = parser.dereference().mergeAllOf().getRefs();
        Object firstServer = parser.refs.jsonContext.read(JsonPath.compile("$.servers[1]"));
        Object firstServerAsObjects = parser.refs.jsonContext.read(JsonPath.compile("$['servers']['1']"));
        Object firstServerWithQuotes = parser.refs.jsonContext.read(JsonPath.compile("$.servers['1']"));
        Object firstServerWithDots = parser.refs.jsonContext.read(JsonPath.compile("$.servers.1"));

        Assert.assertTrue(firstServer == firstServerAsObjects && firstServerAsObjects == firstServerWithQuotes && firstServerWithQuotes == firstServerWithDots);
        parser.refs.jsonContext.delete(JsonPath.compile("$.servers.1"));
        parser.refs.jsonContext.delete(JsonPath.compile("$['servers']['1']"));
        //        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(refs.schema()));
    }
}
