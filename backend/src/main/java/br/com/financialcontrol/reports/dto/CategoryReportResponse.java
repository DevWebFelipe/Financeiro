package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.reports.ReportDateType;
import java.util.List;

public record CategoryReportResponse(
    ReportPeriodResponse period,
    ReportDateType dateType,
    List<CategoryReportItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    CategoryReportSummaryResponse summary) {}
