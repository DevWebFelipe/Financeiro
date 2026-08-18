package br.com.financialcontrol.projections.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectionSummaryResponse(
    BigDecimal currentBalance,
    BigDecimal projectedFinalBalance,
    BigDecimal projectedIncome,
    BigDecimal projectedExpense,
    BigDecimal projectedNetCashFlow,
    BigDecimal minimumProjectedBalance,
    LocalDate minimumProjectedBalanceDate,
    BigDecimal reservedAmount,
    BigDecimal availableProjectedBalance) {}
