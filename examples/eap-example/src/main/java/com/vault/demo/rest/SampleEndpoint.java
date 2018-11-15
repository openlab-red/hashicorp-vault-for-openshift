package com.vault.demo.rest;

import com.vault.demo.domain.Sample;
import com.vault.demo.repository.SampleRepository;

import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Stateless
@Path("/samples")
public class SampleEndpoint {


    @Inject
    private SampleRepository sampleRepository;

    @GET
    @Produces("application/json")
    public Response samples() {
        final Iterable<Sample> all = sampleRepository.findAll();
        return Response.ok(all).build();
    }


}