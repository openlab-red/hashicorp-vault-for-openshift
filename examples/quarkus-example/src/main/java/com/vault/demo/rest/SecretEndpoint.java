package com.vault.demo.rest;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/secret")
public class SecretEndpoint {

    @Inject
    @ConfigProperty(name="secret.example.password")
    String password;

    @GET
    @Produces("text/plain")
    public Response doGet() {
        return Response.ok("my secret is " + password).build();
    }
}
