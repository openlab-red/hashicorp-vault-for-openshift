package com.vault.demo.config;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

//import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class ExternalConfigSource implements ConfigSource {
    
    private final String CONFIG_PROPERTY_PATH = "com.vault.demo.config.path";
    private final String CONFIG_SOURCE_NAME = "ExternalConfigSource";
    private final int ORDINAL = 300;
    final Map<String, String> map = new HashMap<>();

    
    public ExternalConfigSource() throws IOException {
        // TODO: Caused by: java.util.NoSuchElementException: Property com.vault.demo.config.path not found 
        //final String location = ConfigProvider.getConfig().getValue(CONFIG_PROPERTY_PATH, String.class);
        final String location = "/vault/secrets/application.properties";
        final Properties properties = new Properties();
        final Path path = FileSystems.getDefault().getPath(location);
        properties.load(Files.newInputStream(path));

        properties.stringPropertyNames()
                .stream()
                .forEach(key-> map.put(key, properties.getProperty(key)));
	}


    @Override
    public String getValue(String s) {
        return map.get(s);
    }

    @Override
    public Map<String, String> getProperties() {
        return map;
    }

    @Override
    public Set<String> getPropertyNames() {
        return map.keySet();
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }


    @Override
    public String getName() {
        return CONFIG_SOURCE_NAME;
    }

}
