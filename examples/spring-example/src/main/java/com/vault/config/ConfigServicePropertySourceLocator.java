
package com.vault.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Order(0)
public class ConfigServicePropertySourceLocator implements PropertySourceLocator {

    private static Log logger = LogFactory
            .getLog(ConfigServicePropertySourceLocator.class);

    private ConfigClientProperties defaultProperties;

    public ConfigServicePropertySourceLocator(ConfigClientProperties defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    @Value("${vault.path:/var/run/secrets/vaultproject.io}")
    private String vaultPath;

    @Value("${vault.name:application.yaml}")
    private String vaultProperties;

    @Override
    public org.springframework.core.env.PropertySource<?> locate(
            org.springframework.core.env.Environment environment) {

        ConfigClientProperties properties = this.defaultProperties.override(environment);
        CompositePropertySource composite = new CompositePropertySource("configService");

        Exception error = null;
        try {
            String[] labels = new String[]{""};
            if (StringUtils.hasText(properties.getLabel())) {
                labels = StringUtils
                        .commaDelimitedListToStringArray(properties.getLabel());
            }
            for (String label : labels) {
                Environment result = getEnvironment(properties);
                if (result != null) {
                    log(result);

                    if (result.getPropertySources() != null) {
                        for (PropertySource source : result.getPropertySources()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) source
                                    .getSource();
                            composite.addPropertySource(
                                    new MapPropertySource(source.getName(), map));
                        }
                    }

                    if (StringUtils.hasText(result.getState())
                            || StringUtils.hasText(result.getVersion())) {
                        HashMap<String, Object> map = new HashMap<>();
                        putValue(map, "config.client.state", result.getState());
                        putValue(map, "config.client.version", result.getVersion());
                        composite.addFirstPropertySource(
                                new MapPropertySource("configClient", map));
                    }
                    return composite;
                }
            }
        } catch (Exception e) {
            error = e;
        }
        if (properties.isFailFast()) {
            throw new IllegalStateException(
                    "Could not locate PropertySource and the fail fast property is set, failing", error);
        }
        logger.warn("Could not locate PropertySource:  " + error);
        return null;

    }

    private void log(Environment result) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format(
                    "Located environment: name=%s, profiles=%s, label=%s, version=%s, state=%s",
                    result.getName(),
                    result.getProfiles() == null ? ""
                            : Arrays.asList(result.getProfiles()),
                    result.getLabel(), result.getVersion(), result.getState()));
        }
        if (logger.isDebugEnabled()) {
            List<PropertySource> propertySourceList = result.getPropertySources();
            if (propertySourceList != null) {
                int propertyCount = 0;
                for (PropertySource propertySource : propertySourceList) {
                    propertyCount += propertySource.getSource().size();
                }
                logger.debug(String.format(
                        "Environment %s has %d property sources with %d properties.",
                        result.getName(), result.getPropertySources().size(),
                        propertyCount));
            }

        }
    }

    private void putValue(HashMap<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }

    private Environment getEnvironment(
            ConfigClientProperties properties) throws Exception {
        String name = properties.getName();
        String profile = properties.getProfile();
        Environment result = new Environment(name, profile);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        Path path = FileSystems.getDefault().getPath(vaultPath + "/" + vaultProperties);
        File file = path.toFile();

        if (file.exists()) {
            return propertySource(name, result, mapper, file);
        }

        return watchPath(name, result, mapper);
    }

    private Environment watchPath(String name, Environment result, ObjectMapper mapper) throws IOException, InterruptedException {
        Path directoryPath = FileSystems.getDefault().getPath(vaultPath);

        WatchService watchService = FileSystems.getDefault().newWatchService();
        directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        WatchKey watchKey = watchService.take();
        for (final WatchEvent<?> event : watchKey.pollEvents()) {
            Path entry = (Path) event.context();
            if (vaultProperties.equalsIgnoreCase(entry.toString())) {
                File file = directoryPath.resolve(entry).toFile();
                return propertySource(name, result, mapper, file);
            }
        }
        return null;
    }

    private Environment propertySource(String name, Environment result, ObjectMapper mapper, File file) throws java.io.IOException {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        result.add(new PropertySource(name, mapper.readValue(file, typeRef)));
        result.setState("OK");
        result.setVersion(String.valueOf(file.lastModified()));
        return result;
    }
}