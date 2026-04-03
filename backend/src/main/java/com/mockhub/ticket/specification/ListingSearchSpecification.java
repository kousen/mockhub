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

            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            Join<Object, Object> event = root.join("event");
            Join<Object, Object> venue = event.join("venue");
            Join<Object, Object> category = event.join("category");
            Join<Object, Object> ticket = root.join("ticket");
            Join<Object, Object> section = ticket.join("section");

            addTextSearchPredicate(predicates, cb, event, criteria);
            addStringPredicate(predicates, cb, category.get("slug"),
                    criteria.categorySlug(), false);
            addStringPredicate(predicates, cb, venue.get("city"),
                    criteria.city(), true);
            addDatePredicates(predicates, cb, event, criteria);
            addPricePredicates(predicates, cb, root, criteria);
            addStringPredicate(predicates, cb, section.get("name"),
                    criteria.section(), true);

            query.orderBy(cb.asc(root.get(COMPUTED_PRICE)));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addTextSearchPredicate(List<Predicate> predicates,
                                                jakarta.persistence.criteria.CriteriaBuilder cb,
                                                Join<Object, Object> event,
                                                ListingSearchCriteria criteria) {
        if (criteria.query() != null && !criteria.query().isBlank()) {
            String pattern = "%" + criteria.query().strip().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(event.get("name")), pattern),
                    cb.like(cb.lower(event.get("artistName")), pattern)
            ));
        }
    }

    private static void addStringPredicate(List<Predicate> predicates,
                                            jakarta.persistence.criteria.CriteriaBuilder cb,
                                            jakarta.persistence.criteria.Path<String> path,
                                            String value, boolean caseInsensitive) {
        if (value != null && !value.isBlank()) {
            if (caseInsensitive) {
                predicates.add(cb.equal(cb.lower(path),
                        value.strip().toLowerCase(Locale.ROOT)));
            } else {
                predicates.add(cb.equal(path, value.strip()));
            }
        }
    }

    private static void addDatePredicates(List<Predicate> predicates,
                                           jakarta.persistence.criteria.CriteriaBuilder cb,
                                           Join<Object, Object> event,
                                           ListingSearchCriteria criteria) {
        predicates.add(cb.greaterThan(event.get("eventDate"), criteria.dateFrom()));
        if (criteria.dateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(event.get("eventDate"), criteria.dateTo()));
        }
    }

    private static void addPricePredicates(List<Predicate> predicates,
                                            jakarta.persistence.criteria.CriteriaBuilder cb,
                                            jakarta.persistence.criteria.Root<Listing> root,
                                            ListingSearchCriteria criteria) {
        if (criteria.minPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(COMPUTED_PRICE), criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(COMPUTED_PRICE), criteria.maxPrice()));
        }
    }
}
