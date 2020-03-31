package com.vault.demo.config;


import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ExternalConfigSource implements ConfigSource {

    private final String CONFIG_PROPERTY_PATH = "com.vault.demo.config.path";
    private final String CONFIG_SOURCE_NAME = "ExternalConfigSource";
    private final int ORDINAL = 300;

    private String configSource;

    @Override
    public Map<String, String> getProperties() {

        final Map<String, String> map = new HashMap<>();
        final Properties load = load();

        load.stringPropertyNames()
                .stream()
                .forEach(key-> map.put(key, load.getProperty(key)));

        return map;
    }

    @Override
    public Set<String> getPropertyNames() {
        return load().stringPropertyNames();
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }

    @Override
    public String getValue(String s) {
        return load().getProperty(s);
    }

    @Override
    public String getName() {
        return CONFIG_SOURCE_NAME;
    }


    public String read(){

        if(Objects.isNull(configSource)) {
            this.configSource = ConfigProvider.getConfig().getValue(CONFIG_PROPERTY_PATH, String.class);
        }
        return this.configSource;
    }

    public Properties load() {
        Properties properties = new Properties();
        try(InputStream in = new FileInputStream(read())){
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
