package com.vault.demo.config;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.sql.DataSource;

@Singleton
@DataSourceDefinition(
        name = "java:jboss/datasources/SampleDS",
        className = "org.postgresql.xa.PGXADataSource",
        user = "postgres",
        password = "postgres",
        serverName = "postgres",
        portNumber = 5432,
        databaseName = "sampledb",
        minPoolSize = 10,
        maxPoolSize = 50)
@Startup
public class DatasourceProducer {

    @Resource(lookup="java:jboss/datasources/SampleDS")
    DataSource ds;

    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}
