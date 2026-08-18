package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.reports.ReportDateType;
import java.util.List;

public record IncomeReportResponse(
    ReportPeriodResponse period,
    ReportDateType dateType,
    List<IncomeReportItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    IncomeReportSummaryResponse summary) {}
