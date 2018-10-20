package com.vault.demo.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
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
                && !annotation.value().isEmpty() ?
                annotation.value() : ip.getMember().getName();


    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        this.properties = System.getProperties();
        final String key = "application";
        final String location = this.properties.getProperty(key);
        final ObjectMapper objectMapper = new ObjectMapper();


        Path path = FileSystems.getDefault().getPath(location);
        File file = path.toFile();
        if (!file.exists()) {
            watchPath(path);
        }

        byte[] data = Files.readAllBytes(path);
        Map<String, String> map = objectMapper.readValue(data, new TypeReference<HashMap<String,String>>() {});

        this.properties.putAll(map);

    }

    private void watchPath(Path path) throws IOException, InterruptedException {
        Path directoryPath = path.getParent();

        WatchService watchService = FileSystems.getDefault().newWatchService();
        directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        WatchKey watchKey = watchService.take();
        while (true) {
            for (final WatchEvent<?> event : watchKey.pollEvents()) {
                Path entry = (Path) event.context();
                if (path.toFile().getName().equalsIgnoreCase(entry.toString())) {
                    return;
                }
            }
        }
    }
}