package br.com.financialcontrol.payables;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record PayablesSummary(
    BigDecimal totalRemaining,
    BigDecimal installmentRemaining,
    BigDecimal invoiceRemaining,
    BigDecimal overdueRemaining,
    BigDecimal overdueInstallmentRemaining,
    BigDecimal overdueInvoiceRemaining,
    long openCount,
    long overdueCount,
    Map<UUID, BigDecimal> invoiceRemainingByCardId,
    Map<UUID, BigDecimal> overdueInvoiceRemainingByCardId) {}
