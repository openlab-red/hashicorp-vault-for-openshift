package com.vault.demo.config;


import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ExternalConfigSource implements ConfigSource {

    private final String FILE_CONFIG_PROPERTY = "com.vault.demo.config.path";
    private final String CONFIG_SOURCE_NAME = "ExternalConfigSource";
    private final int ORDINAL = 300;

    private String fileConfig;

    @Override
    public Map<String, String> getProperties() {

        try(InputStream in = new FileInputStream( readPath() )){

            Properties properties = new Properties();
            properties.load( in );

            Map<String, String> map = new HashMap<>();
            properties.stringPropertyNames()
                    .stream()
                    .forEach(key-> map.put(key, properties.getProperty(key)));

            return map;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Set<String> getPropertyNames() {

        try(InputStream in = new FileInputStream( readPath() )){

            Properties properties = new Properties();
            properties.load( in );

            return properties.stringPropertyNames();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }

    @Override
    public String getValue(String s) {

        try(InputStream in = new FileInputStream( readPath() )){

            Properties properties = new Properties();
            properties.load( in );

            return properties.getProperty(s);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getName() {
        return CONFIG_SOURCE_NAME;
    }


    public String readPath(){

        if(Objects.nonNull(fileConfig)){
            return fileConfig;
        }

        final Config cfg = ConfigProvider.getConfig();
        return fileConfig = cfg.getValue(FILE_CONFIG_PROPERTY, String.class);
    }

}