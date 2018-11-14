package com.vault.demo.config;

import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
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
    public void init() {
        this.properties = System.getProperties();
        final String key = "application";
        final String location = this.properties.getProperty(key);

        try {

            Path path = FileSystems.getDefault().getPath(location);
            File file = path.toFile();
            if (!file.exists()) {
                watchPath(path);
            }


            Yaml yaml = new Yaml();
            Map<String, LinkedHashMap> data = yaml.load(Files.newInputStream(path));

            data.forEach((k, v) -> {
                navigate(new StringBuilder(k), v, properties);
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void navigate(StringBuilder sb, LinkedHashMap<String, ?> map, Properties properties) {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            sb.append(".").append(entry.getKey());
            if (entry.getValue().getClass().equals(String.class)) {
                properties.put(sb.toString(), String.valueOf(entry.getValue()));
            } else {
                navigate(sb, (LinkedHashMap<String, ?>) entry.getValue(), properties);
            }
        }

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