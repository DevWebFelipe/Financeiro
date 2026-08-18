package br.com.financialcontrol.projections.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjectionQuarterResponse(
    String period,
    List<String> months,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netCashFlow,
    BigDecimal openingBalance,
    BigDecimal closingBalance) {}
