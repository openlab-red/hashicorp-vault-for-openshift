package com.vault.demo.config;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;


@DataSourceDefinition(
        name = "java:jboss/datasources/SampleDS",
        className = "org.postgresql.xa.PGXADataSource",
        url = "jdbc:postgresql://postgresql:5432/sampledb",
        user = "postgres",
        password = "postgres",
        serverName = "postgresql",
        portNumber = 5432,
        databaseName = "sampledb")
@Singleton
public class DataSourceProducer {

    @Resource(lookup = "java:jboss/datasources/SampleDS")
    DataSource ds;

    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}