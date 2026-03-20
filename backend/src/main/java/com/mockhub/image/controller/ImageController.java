package com.mockhub.image.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.image.service.ImageStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/images")
@Tag(name = "Images", description = "Image storage and retrieval")
public class ImageController {

    private final ImageStorageService imageStorageService;
    private final Path storageRootPath;

    public ImageController(ImageStorageService imageStorageService,
                           Path storageRootPath) {
        this.imageStorageService = imageStorageService;
        this.storageRootPath = storageRootPath;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get image by ID", description = "Retrieve the full-size image for the given ID")
    @ApiResponse(responseCode = "200", description = "Image returned successfully")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<byte[]> getImage(
            @Parameter(description = "Image ID", example = "1")
            @PathVariable Long id) {
        byte[] imageData = imageStorageService.get(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .contentType(MediaType.IMAGE_PNG)
                .body(imageData);
    }

    @GetMapping("/file/{filename}")
    @Operation(summary = "Get image by filename", description = "Serve an image file from storage by filename")
    @ApiResponse(responseCode = "200", description = "Image returned successfully")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<byte[]> getImageByFilename(
            @Parameter(description = "Image filename", example = "concert-1.jpg")
            @PathVariable String filename) {
        Path imagePath = storageRootPath.resolve("images").resolve(filename).normalize();
        if (!imagePath.startsWith(storageRootPath.resolve("images")) || !Files.exists(imagePath)) {
            throw new ResourceNotFoundException("Image", "filename", filename);
        }
        try {
            byte[] imageData = Files.readAllBytes(imagePath);
            MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .contentType(mediaType)
                    .body(imageData);
        } catch (IOException _) {
            throw new ResourceNotFoundException("Image", "filename", filename);
        }
    }

    @GetMapping("/{id}/thumbnail")
    @Operation(summary = "Get image thumbnail", description = "Retrieve the thumbnail (200px wide) for the given image ID")
    @ApiResponse(responseCode = "200", description = "Thumbnail returned successfully")
    @ApiResponse(responseCode = "404", description = "Image not found")
    public ResponseEntity<byte[]> getThumbnail(
            @Parameter(description = "Image ID", example = "1")
            @PathVariable Long id) {
        byte[] thumbnailData = imageStorageService.getThumbnail(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .contentType(MediaType.IMAGE_PNG)
                .body(thumbnailData);
    }
}
