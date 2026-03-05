package com.agora.booking.management.repository;

import com.agora.booking.management.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    // FR07, FR08 — cari event yang masih aktif
    // isActive=false (soft-deleted) → return empty → 404
    Optional<Event> findByIdAndIsActiveTrue(Long id);
}