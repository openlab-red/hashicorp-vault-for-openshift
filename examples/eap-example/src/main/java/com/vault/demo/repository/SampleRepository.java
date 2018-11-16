package com.vault.demo.repository;

import com.vault.demo.domain.Sample;

import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

@Stateless
public class SampleRepository {

    @Inject
    PersistenceHelper helper;

    public List<Sample> findAll() {
        return helper.getEntityManager().createNamedQuery("Sample.findAll", Sample.class).getResultList();

    }
}
