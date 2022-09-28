package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;
import io.zenwave360.jsonrefparser.AuthenticationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpResolver implements Resolver {
    private static Logger log = LoggerFactory.getLogger(HttpResolver.class);
    public static final String TRUST_ALL = String.format("%s.trustAll", HttpResolver.class.getName());
    private static final String ACCEPT_HEADER_VALUE = "application/json, application/yaml, */*";
    private static final String USER_AGENT_HEADER_VALUE = "Apache-HttpClient/$JSONSchemaRefParserJVM";
    private final ConnectionConfigurator CONNECTION_CONFIGURATOR = createConnectionConfigurator();

    private List<AuthenticationValue> authenticationValues = new ArrayList<>();

    public HttpResolver withAuthentication(AuthenticationValue authenticationValue) {
        this.authenticationValues.add(authenticationValue);
        return this;
    }

    @Override
    public String resolve($Ref $ref) {
        try {
            return downloadUrlToString($ref.getURI().toString(), authenticationValues);
        } catch (Exception e) {
            log.error("Error resolving {}", $ref, e);
            throw new RuntimeException(e);
        }
    }


//    protected File downloadUrlToFile(String url, List<AuthenticationValue> auths) throws Exception {
//        String filename = FilenameUtils.getName(url);
//        String content = urlToString(url, auths);
//        File temp = File.createTempFile(FilenameUtils.getBaseName(filename), FilenameUtils.getExtension(filename));
//        FileUtils.writeStringToFile(temp, content, UTF_8);
//        return temp;
//    }

    protected String downloadUrlToString(String url, List<AuthenticationValue> auths) throws Exception {
        InputStream is = null;
        BufferedReader br = null;

        try {
            URLConnection conn;
            do {
                final URL inUrl = new URL(cleanUrl(url));
                final List<AuthenticationValue> query = new ArrayList<>();
                final List<AuthenticationValue> header = new ArrayList<>();
                if (auths != null && auths.size() > 0) {
                    for (AuthenticationValue auth : auths) {
                        if (auth.getUrlMatcher() == null || auth.getUrlMatcher().test(inUrl)) {
                            if (AuthenticationValue.AuthenticationType.HEADER == auth.getType()) {
                                header.add(auth);
                            }
                            if (AuthenticationValue.AuthenticationType.QUERY == auth.getType()) {
                                query.add(auth);
                            }
                        }
                    }
                }
                if (!query.isEmpty()) {
                    final URI inUri = inUrl.toURI();
                    final StringBuilder newQuery = new StringBuilder(inUri.getQuery() == null ? "" : inUri.getQuery());
                    for (AuthenticationValue item : query) {
                        if (newQuery.length() > 0) {
                            newQuery.append("&");
                        }
                        newQuery.append(URLEncoder.encode(item.getKey(), UTF_8.name())).append("=")
                                .append(URLEncoder.encode(item.getValue(), UTF_8.name()));
                    }
                    conn = new URI(inUri.getScheme(), inUri.getAuthority(), inUri.getPath(), newQuery.toString(),
                            inUri.getFragment()).toURL().openConnection();
                } else {
                    conn = inUrl.openConnection();
                }
                CONNECTION_CONFIGURATOR.process(conn);
                for (AuthenticationValue item : header) {
                    conn.setRequestProperty(item.getKey(), item.getValue());
                }

                conn.setRequestProperty("Accept", ACCEPT_HEADER_VALUE);
                conn.setRequestProperty("User-Agent", USER_AGENT_HEADER_VALUE);
                conn.connect();
                url = ((HttpURLConnection) conn).getHeaderField("Location");
            } while (301 == ((HttpURLConnection) conn).getResponseCode());
            InputStream in = conn.getInputStream();

            StringBuilder contents = new StringBuilder();

            BufferedReader input = new BufferedReader(new InputStreamReader(in, UTF_8));

            for (int i = 0; i != -1; i = input.read()) {
                char c = (char) i;
                if (!Character.isISOControl(c)) {
                    contents.append((char) i);
                }
                if (c == '\n') {
                    contents.append('\n');
                }
            }

            in.close();

            return contents.toString();
        } catch (javax.net.ssl.SSLProtocolException e) {
            log.warn("there is a problem with the target SSL certificate");
            log.warn("**** you may want to run with -Djsse.enableSNIExtension=false\n\n");
            log.error("unable to read", e);
            throw e;
        } catch (Exception e) {
            log.error("unable to read", e);
            throw e;
        } finally {
            if (is != null) {
                is.close();
            }
            if (br != null) {
                br.close();
            }
        }
    }

    private String cleanUrl(String url) {
        String result = null;
        try {
            result = url.replaceAll("\\{", "%7B").replaceAll("\\}", "%7D").replaceAll(" ", "%20");
        } catch (PatternSyntaxException ignored) {
            log.debug("Pattern exception cleaning url", ignored);
        }
        return result;
    }
    private interface ConnectionConfigurator {

        void process(URLConnection connection);
    }

    private ConnectionConfigurator createConnectionConfigurator() {
        if (Boolean.parseBoolean(System.getProperty(TRUST_ALL))) {
            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } };

                // Install the all-trusting trust manager
                final SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                final SSLSocketFactory sf = sc.getSocketFactory();

                // Create all-trusting host name verifier
                final HostnameVerifier trustAllNames = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };

                return new ConnectionConfigurator() {

                    @Override
                    public void process(URLConnection connection) {
                        if (connection instanceof HttpsURLConnection) {
                            final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                            httpsConnection.setSSLSocketFactory(sf);
                            httpsConnection.setHostnameVerifier(trustAllNames);
                        }
                    }
                };
            } catch (NoSuchAlgorithmException e) {
                log.error("Not Supported", e);
            } catch (KeyManagementException e) {
                log.error("Not Supported", e);
            }
        }
        return new ConnectionConfigurator() {

            @Override
            public void process(URLConnection connection) {
                // Do nothing
            }
        };
    }
}
