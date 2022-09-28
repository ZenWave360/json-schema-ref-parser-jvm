package io.zenwave360.jsonrefparser;

import io.zenwave360.jsonrefparser.resolver.RefFormat;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class  $Ref {
    public static final String REFERENCE_SEPARATOR = "#/";

    private final String ref;
    private final RefFormat refFormat;
    private final URI uri;
    private final String path;
    private final URI referencingFileURI;

    private $Ref(String ref, RefFormat refFormat, URI uri, String path, URI referencingFileURI) {
        this.ref = ref;
        this.refFormat = refFormat;
        this.uri = uri;
        this.path = path;
        this.referencingFileURI = referencingFileURI;
    }

    public static $Ref of(String ref, URI referencingFileURL) {
        RefFormat refFormat = RefFormat.INTERNAL;
        ref = mungedRef(ref);
        if (ref.startsWith("classpath:")) {
            refFormat = RefFormat.CLASSPATH;
        } else if(ref.startsWith("http")||ref.startsWith("https")) {
            refFormat = RefFormat.URL;
        } else if(ref.startsWith(REFERENCE_SEPARATOR)) {
            refFormat = RefFormat.INTERNAL;
        } else if(ref.startsWith(".") || ref.startsWith("/") || ref.indexOf(REFERENCE_SEPARATOR) > 0) {
            refFormat = RefFormat.RELATIVE;
        }

        URI url = null;
        String path = null;
        if(RefFormat.INTERNAL == refFormat) {
            path = ref;
        } else {
            String[] tokens = ref.split(REFERENCE_SEPARATOR, 2);
            String urlStr = RefFormat.RELATIVE == refFormat? buildUrl(referencingFileURL.toString(), tokens[0]) : tokens[0];
            url = URI.create(urlStr);
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

    public URI getURI() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public URI getReferencingFileURI() {
        return referencingFileURI;
    }

    @Override
    public String toString() {
        return "$Ref {" + (uri != null? uri.toString() : "") + path + '}';
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
