package br.com.financialcontrol.reports.dto;

import java.util.List;

public record CardReportResponse(
    ReportPeriodResponse period,
    List<CardReportItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    CardReportSummaryResponse summary) {}
