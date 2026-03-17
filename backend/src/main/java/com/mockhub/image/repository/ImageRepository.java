package com.mockhub.image.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.image.entity.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByEventIdOrderBySortOrderAsc(Long eventId);

    Optional<Image> findByEventIdAndIsPrimaryTrue(Long eventId);
}
