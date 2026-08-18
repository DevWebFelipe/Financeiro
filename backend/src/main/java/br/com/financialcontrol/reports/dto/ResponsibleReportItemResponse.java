package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.expenses.ResponsibleType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponsibleReportItemResponse(
    String key,
    ResponsibleType responsibleType,
    String responsibleName,
    ExpenseReportSummaryResponse expense,
    IncomeReportSummaryResponse income) {}
