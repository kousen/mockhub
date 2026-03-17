package com.mockhub.event.specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.mockhub.event.entity.Event;

import jakarta.persistence.criteria.Join;

public final class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> hasStatus(String status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Event> hasCategory(String categorySlug) {
        return (root, query, criteriaBuilder) -> {
            Join<Object, Object> category = root.join("category");
            return criteriaBuilder.equal(category.get("slug"), categorySlug);
        };
    }

    public static Specification<Event> hasTags(List<String> tagSlugs) {
        return (root, query, criteriaBuilder) -> {
            Join<Object, Object> tags = root.join("tags");
            return tags.get("slug").in(tagSlugs);
        };
    }

    public static Specification<Event> inCity(String city) {
        return (root, query, criteriaBuilder) -> {
            Join<Object, Object> venue = root.join("venue");
            return criteriaBuilder.equal(venue.get("city"), city);
        };
    }

    public static Specification<Event> eventDateAfter(Instant dateFrom) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), dateFrom);
    }

    public static Specification<Event> eventDateBefore(Instant dateTo) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), dateTo);
    }

    public static Specification<Event> minPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("minPrice"), minPrice);
    }

    public static Specification<Event> maxPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("maxPrice"), maxPrice);
    }

    public static Specification<Event> nameOrArtistContains(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("artistName")), pattern)
            );
        };
    }
}
