package com.wavelength.social;

import com.wavelength.auth.CurrentUserProvider;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final CurrentUserProvider current;
    private final ReportService reports;

    public ReportController(CurrentUserProvider current, ReportService reports) {
        this.current = current;
        this.reports = reports;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse report(@Valid @RequestBody ReportRequest request) {
        return reports.create(current.get().getId(), request);
    }
}
