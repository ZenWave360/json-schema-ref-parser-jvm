package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;

public class ClasspathResolver implements Resolver {
    @Override
    public String resolve($Ref $ref) {
        String content = null;
        try {
            content = new String(getClass().getResourceAsStream($ref.getURI().getPath()).readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
