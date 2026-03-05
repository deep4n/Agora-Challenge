package com.agora.booking.management.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ReferenceNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final int RANDOM_BOUND = 900000;
    private static final int RANDOM_ORIGIN = 100000;

    /**
     * Generate reference number dengan format: BK-YYYYMMDD-XXXXXX
     * Contoh: BK-20250101-483921
     *
     * - BK → prefix booking
     * - YYYYMMDD → tanggal booking dibuat
     * - XXXXXX → 6 digit random (100000 - 999999)
     */
    public String generate() {
        String date = LocalDateTime.now().format(DATE_FORMATTER);
        int random = ThreadLocalRandom.current()
                .nextInt(RANDOM_ORIGIN, RANDOM_ORIGIN + RANDOM_BOUND);

        return "BK-" + date + "-" + random;
    }
}