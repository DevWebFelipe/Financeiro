package br.com.financialcontrol.reports.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponsibleReportSummaryResponse(
    ExpenseReportSummaryResponse expense, IncomeReportSummaryResponse income) {}
