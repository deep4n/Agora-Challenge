package com.agora.booking.management.repository;

import com.agora.booking.management.entity.Booking;
import com.agora.booking.management.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // FR14 — cek apakah user sudah booking event ini
    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    // FR11 — ambil semua booking milik user
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // FR12 — cari booking by id dan user (pastikan booking milik user)
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    // FR12 — cari booking by id, user, dan status ACTIVE
    Optional<Booking> findByIdAndUserIdAndStatus(Long id, Long userId, BookingStatus status);
}