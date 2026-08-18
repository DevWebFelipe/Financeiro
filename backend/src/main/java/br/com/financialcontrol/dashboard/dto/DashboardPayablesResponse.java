package br.com.financialcontrol.dashboard.dto;

import java.math.BigDecimal;

public record DashboardPayablesResponse(
    BigDecimal totalRemaining,
    BigDecimal installmentRemaining,
    BigDecimal invoiceRemaining,
    BigDecimal overdueRemaining,
    BigDecimal overdueInstallmentRemaining,
    BigDecimal overdueInvoiceRemaining,
    long openCount,
    long overdueCount) {}
