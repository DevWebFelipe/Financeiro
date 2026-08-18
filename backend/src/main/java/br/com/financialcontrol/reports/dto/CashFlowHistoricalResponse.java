package br.com.financialcontrol.reports.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CashFlowHistoricalResponse(
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    List<CashFlowItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    CashFlowSummaryResponse summary) {}
