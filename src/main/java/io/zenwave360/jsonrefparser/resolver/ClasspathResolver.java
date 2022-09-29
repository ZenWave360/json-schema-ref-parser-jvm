package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;
import io.zenwave360.jsonrefparser.$RefParser;

public class ClasspathResolver implements Resolver {

    private ClassLoader resourceClassLoader = getClass().getClassLoader();
    public ClasspathResolver withResourceClassLoader(ClassLoader resourceClassLoader) {
        if(resourceClassLoader != null) {
            this.resourceClassLoader = resourceClassLoader;
        }
        return this;
    }
    @Override
    public String resolve($Ref $ref) {
        String content = null;
        try {
            content = new String(resourceClassLoader.getResourceAsStream($ref.getURI().getPath().replaceFirst("^/", "")).readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
