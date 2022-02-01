package io.zenwave360.jsonrefparser;

import io.zenwave360.jsonrefparser.resolver.RefFormat;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class  $Ref {
    public static final String REFERENCE_SEPARATOR = "#/";

    private final String ref;
    private final RefFormat refFormat;
    private final URL url;
    private final String path;
    private final URL referencingFileURL;

    private $Ref(String ref, RefFormat refFormat, URL url, String path, URL referencingFileURL) {
        this.ref = ref;
        this.refFormat = refFormat;
        this.url = url;
        this.path = path;
        this.referencingFileURL = referencingFileURL;
    }

    public static $Ref of(String ref, URL referencingFileURL) {
        RefFormat refFormat = RefFormat.INTERNAL;
        ref = mungedRef(ref);
        if(ref.startsWith("http")||ref.startsWith("https")) {
            refFormat = RefFormat.URL;
        } else if(ref.startsWith(REFERENCE_SEPARATOR)) {
            refFormat = RefFormat.INTERNAL;
        } else if(ref.startsWith(".") || ref.startsWith("/") || ref.indexOf(REFERENCE_SEPARATOR) > 0) {
            refFormat = RefFormat.RELATIVE;
        }

        URL url = null;
        String path = null;
        if(RefFormat.INTERNAL == refFormat) {
            path = ref;
        } else {
            String[] tokens = ref.split(REFERENCE_SEPARATOR, 2);
            String urlStr = RefFormat.RELATIVE == refFormat? buildUrl(referencingFileURL.toExternalForm(), tokens[0]) : tokens[0];
            try {
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            if(tokens.length == 2) {
                path = REFERENCE_SEPARATOR + tokens[1];
            }
        }


        return new $Ref(ref, refFormat, url, path, referencingFileURL);
    }

    public String getRef() {
        return ref;
    }

    public RefFormat getRefFormat() {
        return refFormat;
    }

    public URL getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public URL getReferencingFileURL() {
        return referencingFileURL;
    }

    @Override
    public String toString() {
        return "$Ref {" + (url != null? url.toExternalForm() : "") + path + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof $Ref)) {
            return false;
        }
        $Ref that = ($Ref) o;
        return ref != null && ref.equals(that.ref);
    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(ref);
//    }

    private static String mungedRef(String refString) {
        // Ref: IETF RFC 3966, Section 5.2.2
        if (refString != null &&
                !refString.contains(":") && // No scheme
                !refString.startsWith("#") && // Path is not empty
                !refString.startsWith("/") && // Path is not absolute
                !refString.contains("$") &&
                refString.indexOf(".") > 0) { // Path does not start with dot but contains "." (file extension)
            return "./" + refString;
        }
        return refString;
    }

    private static String buildUrl(String rootPath, String relativePath) {
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
