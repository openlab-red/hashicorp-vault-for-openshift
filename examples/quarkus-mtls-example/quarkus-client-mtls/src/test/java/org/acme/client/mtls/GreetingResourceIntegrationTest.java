package org.acme.client.mtls;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.MediaType;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@QuarkusTest
public class GreetingResourceIntegrationTest {

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig()
        .httpsPort(8443)
        .keystorePath(GreetingResourceTest.class.getClassLoader().getResource("ssl/server.keystore").getPath())
        .keystorePassword("password")
        .needClientAuth(true)
        .trustStorePath(GreetingResourceTest.class.getClassLoader().getResource("ssl/server.truststore").getPath())
        .trustStorePassword("password"));


    @BeforeAll
    static void start() {
        wireMockServer.start();

        stubFor(get(urlEqualTo("/hello-client")).willReturn(
          aResponse().withStatus(200)
            .withHeader("Content-Type", MediaType.TEXT_PLAIN)
            .withBody("hello")));

    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @Disabled("not ready yet") // TODO: handshake failure
    public void testHelloEndpoint() {    
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("hello from server"));
    }

}