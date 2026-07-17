package com.mockhub.event.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.event.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderBySortOrderAsc();
}
