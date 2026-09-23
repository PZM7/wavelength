package com.wavelength.social;

import com.wavelength.common.ApiException;
import com.wavelength.users.UserRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReportService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public ReportService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional
    public ReportResponse create(UUID reporter, ReportRequest request) {
        if (reporter.equals(request.reportedUserId())) throw new IllegalArgumentException();
        if (!users.existsById(request.reportedUserId())) throw ApiException.userNotFound();
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO reports (id, reporter_id, reported_user_id, reason, description)"
                        + " VALUES (?, ?, ?, ?, ?)",
                id,
                reporter,
                request.reportedUserId(),
                request.reason().name(),
                request.description());
        return new ReportResponse(id);
    }
}
