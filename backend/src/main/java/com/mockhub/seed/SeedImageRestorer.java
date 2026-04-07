package com.mockhub.seed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;

/**
 * Restores seed images from the classpath to the filesystem on every startup.
 * Railway's ephemeral filesystem loses uploaded files on redeploy, so this
 * ensures event images are always available regardless of profile.
 */
@Component
public class SeedImageRestorer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedImageRestorer.class);
    private static final int IMAGES_PER_CATEGORY = 5;
    private static final Map<String, String> CATEGORY_IMAGE_PREFIX = Map.of(
            "concerts", "concert",
            "sports", "sports",
            "theater", "theater",
            "comedy", "comedy",
            "festivals", "festival"
    );

    private final EventRepository eventRepository;
    private final Path storageRootPath;

    public SeedImageRestorer(EventRepository eventRepository, Path storageRootPath) {
        this.eventRepository = eventRepository;
        this.storageRootPath = storageRootPath;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        restoreSeedImages();
    }

    private void restoreSeedImages() {
        Path imagesDir = storageRootPath.resolve("images");
        try {
            Files.createDirectories(imagesDir);
        } catch (IOException _) {
            log.warn("Failed to create images directory");
            return;
        }

        int copied = 0;
        for (Map.Entry<String, String> entry : CATEGORY_IMAGE_PREFIX.entrySet()) {
            String categorySlug = entry.getKey();
            String prefix = entry.getValue();
            for (int i = 1; i <= IMAGES_PER_CATEGORY; i++) {
                String filename = prefix + "-" + i + ".jpg";
                ClassPathResource resource = new ClassPathResource(
                        "seed-images/" + categorySlug + "/" + filename);
                if (resource.exists()) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        Path destPath = imagesDir.resolve(filename);
                        Files.copy(inputStream, destPath, StandardCopyOption.REPLACE_EXISTING);
                        copied++;
                    } catch (IOException _) {
                        log.warn("Failed to restore seed image: {}", filename);
                    }
                }
            }
        }

        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            String imageUrl = event.getPrimaryImageUrl();
            if (imageUrl != null && imageUrl.startsWith("/api/v1/images/file/")) {
                String destFilename = imageUrl.replace("/api/v1/images/file/", "");
                Path destPath = imagesDir.resolve(destFilename);
                if (!Files.exists(destPath)) {
                    String categorySlug = event.getCategory() != null
                            ? event.getCategory().getSlug() : "concerts";
                    copySeedImage(categorySlug, event.getName(), imagesDir);
                }
            }
        }

        log.info("Restored {} seed images to {}", copied, imagesDir);
    }

    private void copySeedImage(String categorySlug, String eventName, Path imagesDir) {
        String prefix = CATEGORY_IMAGE_PREFIX.getOrDefault(categorySlug, "concert");
        int imageIndex = ((eventName.hashCode() & Integer.MAX_VALUE) % IMAGES_PER_CATEGORY) + 1;
        String sourceFilename = prefix + "-" + imageIndex + ".jpg";
        String destFilename = slugify(eventName) + ".jpg";

        try {
            ClassPathResource resource = new ClassPathResource(
                    "seed-images/" + categorySlug + "/" + sourceFilename);
            if (!resource.exists()) {
                return;
            }
            Path destPath = imagesDir.resolve(destFilename);
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, destPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException _) {
            log.warn("Failed to copy seed image for event: {}", eventName);
        }
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-)|(-$)", "");
    }
}
