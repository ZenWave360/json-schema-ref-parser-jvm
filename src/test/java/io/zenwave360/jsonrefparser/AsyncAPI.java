package io.zenwave360.jsonrefparser;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;

public class AsyncAPI {
    @Test
    public void test() throws IOException {
        final var parser = new $RefParser(URI.create("https://raw.githubusercontent.com/asyncapi/spec-json-schemas/refs/heads/master/schemas/3.0.0.json"));
        final var schema = parser
                .parse()
                .dereference()
                .mergeAllOf()
                .getRefs().schema();
    }
}
