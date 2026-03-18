package io.zenwave360.jsonrefparser.resolver;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.zenwave360.jsonrefparser.$Ref;
import io.zenwave360.jsonrefparser.AuthenticationValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class HttpResolverHeaderTest {
    
    private WireMockServer wireMockServer;
    private HttpResolver httpResolver;
    
    @Before
    public void setup() {
        wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        
        httpResolver = new HttpResolver();
    }
    
    @After
    public void teardown() {
        wireMockServer.stop();
    }
    
    @Test
    public void testHeadersAreSent() throws Exception {
        // Setup mock response
        stubFor(get(urlEqualTo("/test.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"test\": \"data\"}")));
        
        httpResolver.withAuthentication(new AuthenticationValue()
                .withHeader("Authorization", "Bearer test-token")
                .withUrlMatcher(url -> url.getHost().equals("localhost")));

        httpResolver.withAuthentication(new AuthenticationValue()
                .withHeader("API-Key", "test-key")
                .withUrlPattern("local*"));

        httpResolver.withAuthentication(new AuthenticationValue()
                .withHeader("OTHER", "other")
                .withUrlPattern("other"));
        
        // Make request
        $Ref ref = $Ref.of("http://localhost:8089/test.json", null);
        String result = httpResolver.resolve(ref);
        
        // Verify headers were sent
        verify(getRequestedFor(urlEqualTo("/test.json"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withHeader("API-Key", equalTo("test-key"))
                .withHeader("Accept", equalTo("application/json, application/yaml, */*"))
                .withHeader("User-Agent", equalTo("Apache-HttpClient/$JSONSchemaRefParserJVM")));
        
        System.out.println("Response: " + result);
    }
}
