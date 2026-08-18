package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.reports.ReportDateType;
import br.com.financialcontrol.reports.ReportNature;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ResponsibleReportResponse(
    ReportPeriodResponse period,
    ReportNature nature,
    @JsonInclude(JsonInclude.Include.NON_NULL) ReportDateType dateType,
    List<ResponsibleReportItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    ResponsibleReportSummaryResponse summary) {}
