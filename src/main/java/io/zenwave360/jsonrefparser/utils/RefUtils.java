package io.zenwave360.jsonrefparser.utils;

import io.zenwave360.jsonrefparser.resolver.RefFormat;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;


public class RefUtils {

    private static final Logger log = LoggerFactory.getLogger(RefUtils.class);

    public static String readExternalRef(String file, Path parentDirectory, RefFormat refFormat, List<AuthenticationValue> auths) {

        if (!refFormat.isAnExternalRefFormat()) {
            throw new RuntimeException("Ref is not external");
        }

        String result = null;

        try {
            if (refFormat == RefFormat.URL) {
                result = HttpClient.urlToString(file, auths);
            } else { // its assumed to be a relative file ref
                final Path pathToUse = parentDirectory.resolve(file).normalize();

                if (Files.exists(pathToUse)) {
                    result = readPathToString(pathToUse);
                } else {
                    String url = file;
                    if (url.contains("..")) {
                        int parentCount = 0;
                        while (url.contains("..")) {
                            url = url.substring(url.indexOf(".") + 2);
                            parentCount++;
                        }
                        for (int i = 0; i < parentCount - 1; i++) {
                            parentDirectory = parentDirectory.getParent();
                        }
                        url = parentDirectory + url;
                    } else {
                        url = parentDirectory + url.substring(url.indexOf(".") + 1);
                    }
                    final Path pathToUse2 = parentDirectory.resolve(url).normalize();

                    if (Files.exists(pathToUse2)) {
                        result = readPathToString(pathToUse2);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + refFormat + " ref: '" + file + "' folder: " + parentDirectory, e);
        }

        return result;

    }

    private static String readPathToString(Path path) throws IOException {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }

    public static String buildUrl(String rootPath, String relativePath) {
        String[] rootPathParts = rootPath.split("/");
        String [] relPathParts = relativePath.split("/");

        if(rootPath == null || relativePath == null) {
            return null;
        }

        int trimRoot = 0;
        int trimRel = 0;

        if(!"".equals(rootPathParts[rootPathParts.length - 1])) {
            trimRoot = 1;
        }
        if("".equals(relPathParts[0])) {
            trimRel = 1; trimRoot = rootPathParts.length-3;
        }        
        for(int i = 0; i < rootPathParts.length; i++) {
            if("".equals(rootPathParts[i])) {
                trimRel += 1;
            }
            else {
                break;
            }
        }
        for(int i = 0; i < relPathParts.length; i ++) {
            if(".".equals(relPathParts[i])) {
                trimRel += 1;
            }
            else if ("..".equals(relPathParts[i])) {
                trimRel += 1; trimRoot += 1;
            }
        }

        String [] outputParts = new String[rootPathParts.length + relPathParts.length - trimRoot - trimRel];
        System.arraycopy(rootPathParts, 0, outputParts, 0, rootPathParts.length - trimRoot);
        System.arraycopy(relPathParts,
                trimRel,
                outputParts,
                rootPathParts.length - trimRoot,
                relPathParts.length - trimRel);

        return StringUtils.join(outputParts, "/");
    }

}