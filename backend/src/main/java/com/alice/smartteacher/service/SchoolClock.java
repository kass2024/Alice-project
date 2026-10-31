package com.alice.smartteacher.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class SchoolClock {
    public static final ZoneId ZONE = ZoneId.of("Africa/Kigali");

    private SchoolClock() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
