package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class FileResolver implements Resolver {
    @Override
    public String resolve($Ref $ref) {
        String content = null;
        try {
            content = new String(Files.readAllBytes(Paths.get($ref.getURI())));
        } catch (NoSuchFileException e) {
            throw new MissingResourceException("File not found: " + $ref.getURI(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
