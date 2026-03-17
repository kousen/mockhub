package com.mockhub.event.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.event.entity.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    List<Tag> findBySlugIn(List<String> slugs);
}
