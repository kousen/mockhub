package com.mockhub.image.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.image.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByEventIdOrderBySortOrderAsc(Long eventId);

    Optional<Image> findByEventIdAndIsPrimaryTrue(Long eventId);
}
