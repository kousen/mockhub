package com.mockhub.image.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.config.SecurityConfig;
import com.mockhub.image.service.ImageStorageService;

import java.nio.file.Path;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private Path storageRootPath;

    private byte[] createTestImageBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    @Test
    @DisplayName("GET /api/v1/images/{id} - given existing image - returns PNG with cache headers")
    void getImage_givenExistingImage_returnsPngWithCacheHeaders() throws Exception {
        byte[] imageData = createTestImageBytes();
        when(imageStorageService.get(1L)).thenReturn(imageData);

        mockMvc.perform(get("/api/v1/images/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(imageData))
                .andExpect(header().string("Cache-Control", "max-age=3600"));
    }

    @Test
    @DisplayName("GET /api/v1/images/{id} - given nonexistent image - returns 404")
    void getImage_givenNonexistentImage_returns404() throws Exception {
        when(imageStorageService.get(999L))
                .thenThrow(new ResourceNotFoundException("Image", "id", "999"));

        mockMvc.perform(get("/api/v1/images/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/images/{id} - no auth required - returns image")
    void getImage_noAuthRequired_returnsImage() throws Exception {
        byte[] imageData = createTestImageBytes();
        when(imageStorageService.get(1L)).thenReturn(imageData);

        mockMvc.perform(get("/api/v1/images/1"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageData));
    }

    @Test
    @DisplayName("GET /api/v1/images/{id}/thumbnail - given existing image - returns thumbnail PNG")
    void getThumbnail_givenExistingImage_returnsThumbnailPng() throws Exception {
        byte[] thumbnailData = createTestImageBytes();
        when(imageStorageService.getThumbnail(1L)).thenReturn(thumbnailData);

        mockMvc.perform(get("/api/v1/images/1/thumbnail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(thumbnailData))
                .andExpect(header().string("Cache-Control", "max-age=3600"));
    }

    @Test
    @DisplayName("GET /api/v1/images/{id}/thumbnail - given nonexistent image - returns 404")
    void getThumbnail_givenNonexistentImage_returns404() throws Exception {
        when(imageStorageService.getThumbnail(999L))
                .thenThrow(new ResourceNotFoundException("Image", "id", "999"));

        mockMvc.perform(get("/api/v1/images/999/thumbnail"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/images/{id}/thumbnail - no auth required - returns thumbnail")
    void getThumbnail_noAuthRequired_returnsThumbnail() throws Exception {
        byte[] thumbnailData = createTestImageBytes();
        when(imageStorageService.getThumbnail(1L)).thenReturn(thumbnailData);

        mockMvc.perform(get("/api/v1/images/1/thumbnail"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(thumbnailData));
    }
}
