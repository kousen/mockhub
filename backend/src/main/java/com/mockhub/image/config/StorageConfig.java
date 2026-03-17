package com.mockhub.image.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${mockhub.storage.location:./uploads}")
    private String storageLocation;

    @Bean
    public Path storageRootPath() {
        Path rootPath = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
            Files.createDirectories(rootPath.resolve("images"));
            Files.createDirectories(rootPath.resolve("thumbnails"));
            log.info("Storage initialized at: {}", rootPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize storage directory: " + rootPath, exception);
        }
        return rootPath;
    }
}
