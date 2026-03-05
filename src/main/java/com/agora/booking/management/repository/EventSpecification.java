package com.agora.booking.management.repository;

import com.agora.booking.management.entity.Event;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> findUpcomingEvents(
            String title,
            String location,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // isActive = true
            predicates.add(cb.isTrue(root.get("isActive")));

            // eventDate > NOW()
            predicates.add(cb.greaterThan(root.get("eventDate"), LocalDateTime.now()));

            // title — case-insensitive partial match
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"));
            }

            // location — case-insensitive partial match
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("location")),
                        "%" + location.toLowerCase() + "%"));
            }

            // startDate filter
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("eventDate"), startDate));
            }

            // endDate filter
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("eventDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}