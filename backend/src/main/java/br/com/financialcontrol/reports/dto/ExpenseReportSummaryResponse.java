package br.com.financialcontrol.reports.dto;

import java.math.BigDecimal;

public record ExpenseReportSummaryResponse(
    BigDecimal periodOriginal,
    BigDecimal periodDiscount,
    BigDecimal periodSurcharge,
    BigDecimal periodObligation,
    BigDecimal periodPaid,
    BigDecimal periodRemaining) {}
