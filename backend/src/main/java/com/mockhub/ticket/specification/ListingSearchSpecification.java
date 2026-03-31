package com.mockhub.ticket.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.entity.Listing;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/**
 * Builds a JPA Specification for listing search from a ListingSearchCriteria.
 * Only non-null criteria add predicates — no "IS NULL OR" patterns needed,
 * which eliminates the Hibernate bytea type inference issue on PostgreSQL.
 */
public final class ListingSearchSpecification {

    private static final String COMPUTED_PRICE = "computedPrice";

    private ListingSearchSpecification() {
    }

    public static Specification<Listing> fromCriteria(ListingSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter for ACTIVE listings
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            // Join through to event, venue, category, section
            Join<Object, Object> event = root.join("event");
            Join<Object, Object> venue = event.join("venue");
            Join<Object, Object> category = event.join("category");
            Join<Object, Object> ticket = root.join("ticket");
            Join<Object, Object> section = ticket.join("section");

            // Text search: match event name or artist name (case-insensitive)
            if (criteria.query() != null && !criteria.query().isBlank()) {
                String pattern = "%" + criteria.query().strip().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(event.get("name")), pattern),
                        cb.like(cb.lower(event.get("artistName")), pattern)
                ));
            }

            // Category filter
            if (criteria.categorySlug() != null && !criteria.categorySlug().isBlank()) {
                predicates.add(cb.equal(category.get("slug"), criteria.categorySlug().strip()));
            }

            // City filter (case-insensitive)
            if (criteria.city() != null && !criteria.city().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(venue.get("city")),
                        criteria.city().strip().toLowerCase(Locale.ROOT)));
            }

            // Date range: event must be after dateFrom (defaults to now in criteria)
            predicates.add(cb.greaterThan(event.get("eventDate"), criteria.dateFrom()));

            // Optional upper date bound
            if (criteria.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(event.get("eventDate"), criteria.dateTo()));
            }

            // Price range filters
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(COMPUTED_PRICE), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(COMPUTED_PRICE), criteria.maxPrice()));
            }

            // Section filter (case-insensitive)
            if (criteria.section() != null && !criteria.section().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(section.get("name")),
                        criteria.section().strip().toLowerCase(Locale.ROOT)));
            }

            // Order by price ascending
            query.orderBy(cb.asc(root.get(COMPUTED_PRICE)));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
