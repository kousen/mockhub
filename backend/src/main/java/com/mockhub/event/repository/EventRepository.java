package com.mockhub.event.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mockhub.event.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findBySlug(String slug);

    @Query("SELECT e FROM Event e WHERE e.isFeatured = true AND e.status = 'ACTIVE' ORDER BY e.eventDate ASC")
    List<Event> findFeaturedEvents();

    @Query(value = "SELECT e.* FROM events e "
            + "WHERE e.search_vector @@ plainto_tsquery('english', :query) "
            + "AND e.status = 'ACTIVE' "
            + "ORDER BY ts_rank(e.search_vector, plainto_tsquery('english', :query)) DESC "
            + "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> fullTextSearch(@Param("query") String query,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM events e "
            + "WHERE e.search_vector @@ plainto_tsquery('english', :query) "
            + "AND e.status = 'ACTIVE'",
            nativeQuery = true)
    long countFullTextSearch(@Param("query") String query);

    @Query(value = "SELECT DISTINCT e.name FROM events e "
            + "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(e.artist_name) LIKE LOWER(CONCAT('%', :query, '%'))) "
            + "AND e.status = 'ACTIVE' "
            + "ORDER BY e.name "
            + "LIMIT 10",
            nativeQuery = true)
    List<String> suggestEvents(@Param("query") String query);
}
