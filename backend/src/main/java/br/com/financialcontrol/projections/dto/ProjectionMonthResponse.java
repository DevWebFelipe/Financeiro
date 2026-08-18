package br.com.financialcontrol.projections.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectionMonthResponse(
    String period,
    BigDecimal openingBalance,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netCashFlow,
    BigDecimal closingBalance,
    BigDecimal minimumProjectedBalance,
    LocalDate minimumProjectedBalanceDate,
    boolean negative,
    BigDecimal reservedAmount,
    BigDecimal availableProjectedBalance) {}
