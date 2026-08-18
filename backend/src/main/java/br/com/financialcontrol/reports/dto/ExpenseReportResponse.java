package br.com.financialcontrol.reports.dto;

import java.util.List;

public record ExpenseReportResponse(
    ReportPeriodResponse period,
    List<ExpenseReportItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    ExpenseReportSummaryResponse summary) {}
