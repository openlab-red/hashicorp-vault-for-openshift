package com.vault.demo;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/secret")
public class SecretEndpoint {

    @Inject
    SecretService secretService;

    @GET
    @Produces("text/plain")
    public Response doGet() {
        return Response.ok(secretService.secret()).build();
    }


}
