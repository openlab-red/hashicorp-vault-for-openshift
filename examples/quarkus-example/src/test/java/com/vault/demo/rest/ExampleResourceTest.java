package com.vault.demo.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ExampleResourceTest {

    @Test
    public void testSecretEndpoint() {
        given()
          .when().get("/secret")
          .then()
             .statusCode(200)
             .body(is("my secret is local"));
    }

}