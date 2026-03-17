package com.mockhub.image.service;

import org.springframework.web.multipart.MultipartFile;

import com.mockhub.image.entity.Image;
import com.mockhub.image.entity.ImageType;

public interface ImageStorageService {

    Image store(MultipartFile file, ImageType imageType);

    byte[] get(Long imageId);

    byte[] getThumbnail(Long imageId);

    void delete(Long imageId);
}
