package br.com.financialcontrol.reports.dto;

import java.math.BigDecimal;

public record CardReportSummaryResponse(
    BigDecimal purchaseAmount,
    BigDecimal invoiceAmount,
    BigDecimal paidAmount,
    BigDecimal creditAmount) {}
