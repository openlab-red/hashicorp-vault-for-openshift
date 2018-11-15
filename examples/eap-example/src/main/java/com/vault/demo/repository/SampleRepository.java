package com.vault.demo.repository;

import com.vault.demo.domain.Sample;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class SampleRepository {

    @Inject
    private EntityManager em;

    public List<Sample> findAll() {
        return em.createNamedQuery("Sample.findAll", Sample.class).getResultList();

    }
}
