package com.wavelength.concerts;

import java.util.UUID;

public record ConcertAttendance(
        UUID concertId, UUID userId, AttendanceStatus status, AttendanceVisibility visibility) {}
