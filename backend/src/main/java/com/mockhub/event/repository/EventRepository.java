package com.mockhub.event.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mockhub.event.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findBySlug(String slug);

    Optional<Event> findByTicketmasterEventId(String ticketmasterEventId);

    @Query("SELECT e FROM Event e WHERE e.isFeatured = true AND e.status = 'ACTIVE' ORDER BY e.eventDate ASC")
    List<Event> findFeaturedEvents();

    @Query("SELECT e FROM Event e WHERE e.isFeatured = true AND e.status = 'ACTIVE' AND LOWER(e.venue.city) = LOWER(:city) ORDER BY e.eventDate ASC")
    List<Event> findFeaturedEventsByCity(@Param("city") String city);

    @Query("SELECT e FROM Event e WHERE (e.spotifyArtistId IS NULL OR e.spotifyArtistId = '') "
            + "AND e.ticketmasterEventId IS NOT NULL")
    List<Event> findMissingSpotifyWithTicketmasterId();

    @Query("SELECT e FROM Event e WHERE e.spotifyArtistId IN :artistIds AND e.status = 'ACTIVE' ORDER BY e.eventDate ASC")
    List<Event> findBySpotifyArtistIdIn(@Param("artistIds") List<String> artistIds);

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

    @Modifying
    @Query("UPDATE Event e SET e.status = 'COMPLETED' WHERE e.status = 'ACTIVE' AND e.eventDate < :now")
    int markPastEventsAsCompleted(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Event e SET e.status = 'CANCELLED' WHERE e.ticketmasterEventId IS NULL AND e.status = 'ACTIVE'")
    int deactivateSeedEvents();

    @Modifying
    @Query("UPDATE Event e SET e.isFeatured = true WHERE e.ticketmasterEventId IS NOT NULL AND e.status = 'ACTIVE'")
    int featureTicketmasterEvents();

    @Modifying
    @Query("UPDATE Event e SET e.status = 'COMPLETED' WHERE e.ticketmasterEventId IS NOT NULL AND e.eventDate < :now AND e.status = 'ACTIVE'")
    int completePastTicketmasterEvents(@Param("now") Instant now);
}
