package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class FileResolver implements Resolver {

    private static final Logger log = LoggerFactory.getLogger(FileResolver.class);
    @Override
    public String resolve($Ref $ref) {
        String content = null;
        try {
            log.info("Resolving file: {}", $ref.getURI());
            content = new String(Files.readAllBytes(Paths.get($ref.getURI())));
        } catch (NoSuchFileException e) {
            throw new MissingResourceException("File not found: " + $ref.getURI(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
