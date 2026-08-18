package br.com.financialcontrol.reports.dto;

public record CategoryReportSummaryResponse(
    ExpenseReportSummaryResponse expense, IncomeReportSummaryResponse income) {}
