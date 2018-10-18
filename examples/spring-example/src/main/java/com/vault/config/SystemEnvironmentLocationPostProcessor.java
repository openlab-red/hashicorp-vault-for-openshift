package com.vault.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.*;
import java.util.Map;

public class SystemEnvironmentLocationPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SPRING_CONFIG_LOCATION = "SPRING_CONFIG_LOCATION";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String sourceName = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        PropertySource<?> propertySource = environment.getPropertySources()
                .get(sourceName);
        Map<String, Object> originalSource = (Map<String, Object>) propertySource
                .getSource();

        SystemEnvironmentPropertySource systemEnvironmentPropertySource = new SystemEnvironmentPropertySource(sourceName, originalSource);
        if (systemEnvironmentPropertySource.containsProperty(SPRING_CONFIG_LOCATION)) {
            String location = String.valueOf(systemEnvironmentPropertySource.getProperty(SPRING_CONFIG_LOCATION));
            if (!StringUtils.isEmpty(location)) {
                location = location.replace(ResourceUtils.FILE_URL_PREFIX, "");
                Path path = FileSystems.getDefault().getPath(location);
                File file = path.toFile();
                if (!file.exists()) {
                    watchPath(path);
                }
            }
        }
    }

    private void watchPath(Path path) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
