package com.vault.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.cdi.CDIFraction;
import org.wildfly.swarm.cli.CommandLine;
import org.wildfly.swarm.jaxrs.JAXRSArchive;
import org.wildfly.swarm.jaxrs.JAXRSFraction;
import org.wildfly.swarm.microprofile.health.HealthFraction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;

public class Main {


    public static void main(String[] args) throws Exception {

        CommandLine commandLine = CommandLine.parse(args);
        List<URL> urls = commandLine.get(CommandLine.CONFIG);
        if (urls != null && urls.size() > 0) {
            URL url = urls.get(0);
            Path path = FileSystems.getDefault().getPath(url.getFile());
            File file = path.toFile();
            if (!file.exists()) {
                watchPath(path);
            }
        }
        Swarm swarm = new Swarm(args);
        swarm
                .fraction(new CDIFraction())
                .fraction(new HealthFraction())
                .fraction(new JAXRSFraction())
                .start()
                .deploy();

    }

    private static void watchPath(Path path) throws IOException, InterruptedException {
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