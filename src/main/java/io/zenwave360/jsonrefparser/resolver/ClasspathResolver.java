package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

public class ClasspathResolver implements Resolver {

    private ClassLoader resourceClassLoader = getClass().getClassLoader();

    public ClasspathResolver withResourceClassLoader(ClassLoader resourceClassLoader) {
        if (resourceClassLoader != null) {
            this.resourceClassLoader = resourceClassLoader;
        }
        return this;
    }

    @Override
    public String resolve($Ref $ref) {
        String content = null;
        try {
            URI uri = $ref.getURI();
            if (uri.toString().startsWith("classpath:") && !uri.toString().startsWith("classpath:/")) {
                // gracefully handle classpath: without the slash
                uri = URI.create(uri.toString().replace("classpath:", "classpath:/"));
            }
            java.io.InputStream inputStream = resourceClassLoader.getResourceAsStream(uri.getPath().replaceFirst("^/", ""));
            if (inputStream == null) {
                throw new MissingResourceException("Resource not found: " + uri, null);
            }
            content =
                    new BufferedReader((new InputStreamReader(inputStream)))
                            .lines()
                            .collect(Collectors.joining(System.lineSeparator()));
        } catch (MissingResourceException missingResourceException) {
            throw missingResourceException;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
