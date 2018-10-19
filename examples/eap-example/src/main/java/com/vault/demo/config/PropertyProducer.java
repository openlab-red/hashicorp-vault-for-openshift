package com.vault.demo.config;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

@Singleton
@Startup
public class PropertyProducer {
    private Properties properties;

    @Property
    @Produces
    public String produceString(final InjectionPoint ip) {
        return this.properties.getProperty(getKey(ip));
    }

    @Property
    @Produces
    public int produceInt(final InjectionPoint ip) {
        return Integer.valueOf(this.properties.getProperty(getKey(ip)));
    }

    @Property
    @Produces
    public boolean produceBoolean(final InjectionPoint ip) {
        return Boolean.valueOf(this.properties.getProperty(getKey(ip)));
    }

    private String getKey(final InjectionPoint ip) {
        final Property annotation = ip.getAnnotated().getAnnotation(Property.class);

        return ip.getAnnotated().isAnnotationPresent(Property.class)
                && !(annotation.value().length() == 0) ?
                annotation.value() : ip.getMember().getName();


    }

    @PostConstruct
    public void init() throws IOException {
        this.properties = System.getProperties();
        final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties");
        this.properties.load(stream);

    }
}