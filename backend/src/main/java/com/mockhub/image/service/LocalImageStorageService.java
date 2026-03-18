package com.mockhub.image.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.image.entity.Image;
import com.mockhub.image.entity.ImageType;
import com.mockhub.image.repository.ImageRepository;

@Service
public class LocalImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorageService.class);
    private static final String IMAGES_DIR = "images";
    private static final String THUMBNAILS_DIR = "thumbnails";

    private final Path storageRootPath;
    private final ImageResizer imageResizer;
    private final ImageRepository imageRepository;

    public LocalImageStorageService(Path storageRootPath,
                                    ImageResizer imageResizer,
                                    ImageRepository imageRepository) {
        this.storageRootPath = storageRootPath;
        this.imageResizer = imageResizer;
        this.imageRepository = imageRepository;
    }

    @Override
    @Transactional
    public Image store(MultipartFile file, ImageType imageType) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String uniqueName = UUID.randomUUID().toString() + extension;

        Path imagePath = storageRootPath.resolve(IMAGES_DIR).resolve(uniqueName);
        Path thumbnailPath = storageRootPath.resolve(THUMBNAILS_DIR).resolve(uniqueName);

        try {
            Files.copy(file.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

            InputStream thumbnailStream = imageResizer.createThumbnail(file.getInputStream());
            Files.copy(thumbnailStream, thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store image: " + originalFilename, exception);
        }

        Image image = new Image();
        image.setUrl("/api/v1/images/" + uniqueName);
        image.setThumbnailUrl("/api/v1/images/" + uniqueName + "/thumbnail");

        log.info("Stored image: {} as {}", originalFilename != null ? originalFilename.replaceAll("[\\r\\n]", "") : "unknown", uniqueName);
        return image;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] get(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        String filename = extractFilenameFromUrl(image.getUrl());
        Path imagePath = storageRootPath.resolve(IMAGES_DIR).resolve(filename);

        try {
            return Files.readAllBytes(imagePath);
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Image file", "id", imageId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getThumbnail(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        String filename = extractFilenameFromUrl(image.getUrl());
        Path thumbnailPath = storageRootPath.resolve(THUMBNAILS_DIR).resolve(filename);

        try {
            return Files.readAllBytes(thumbnailPath);
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Thumbnail file", "id", imageId);
        }
    }

    @Override
    @Transactional
    public void delete(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        String filename = extractFilenameFromUrl(image.getUrl());

        try {
            Files.deleteIfExists(storageRootPath.resolve(IMAGES_DIR).resolve(filename));
            Files.deleteIfExists(storageRootPath.resolve(THUMBNAILS_DIR).resolve(filename));
        } catch (IOException exception) {
            log.warn("Failed to delete image files for image ID: {}", imageId, exception);
        }

        imageRepository.delete(image);
        log.info("Deleted image with ID: {}", imageId);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".png";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String extractFilenameFromUrl(String url) {
        if (url == null) {
            return "";
        }
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }
}
