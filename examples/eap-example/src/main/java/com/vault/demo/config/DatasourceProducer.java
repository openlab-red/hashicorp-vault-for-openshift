package com.vault.demo.config;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.DependsOn;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;



@DataSourceDefinition(
        name = "java:jboss/datasources/SampleDS",
        className = "org.postgresql.xa.PGXADataSource",
        user = "postgres",
        password = "postgres",
        serverName = "postgresql",
        portNumber = 5432,
        databaseName = "sampledb")
@Singleton
@Startup
@DependsOn("PropertyProducer")
public class DatasourceProducer {

    @Resource(lookup = "java:jboss/datasources/SampleDS")
    DataSource ds;

    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}