package br.com.financialcontrol.reports.dto;

import java.time.LocalDate;

public record ReportPeriodResponse(LocalDate startDate, LocalDate endDate) {}
