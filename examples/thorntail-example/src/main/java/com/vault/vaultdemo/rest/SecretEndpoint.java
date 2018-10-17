package com.vault.vaultdemo.rest;


import org.wildfly.swarm.spi.runtime.annotations.ConfigurationValue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;


@ApplicationScoped
@Path("/secret")
public class SecretEndpoint {

    @Inject
    @ConfigurationValue("password")
    String password;

    @GET
    @Produces("text/plain")
    public Response doGet() {
        return Response.ok("my secret is " + password).build();
    }
}
