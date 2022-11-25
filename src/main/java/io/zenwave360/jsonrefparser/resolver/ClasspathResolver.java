package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;
import io.zenwave360.jsonrefparser.$RefParser;

import java.net.URI;

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
            URI uri = $ref.getURI();
            if(uri.toString().startsWith("classpath:") && !uri.toString().startsWith("classpath:/")) {
                // gracefully handle classpath: without the slash
                uri = URI.create(uri.toString().replace("classpath:", "classpath:/"));
            }
            var inputStream = resourceClassLoader.getResourceAsStream(uri.getPath().replaceFirst("^/", ""));
            if(inputStream == null) {
                throw new MissingResourceException("Resource not found: " + uri, null);
            }
            content = new String(inputStream.readAllBytes());
        } catch (MissingResourceException missingResourceException) {
            throw missingResourceException;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
