package br.com.financialcontrol.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DashboardCreditCardResponse(
    UUID id,
    String name,
    BigDecimal creditLimit,
    BigDecimal usedLimit,
    BigDecimal availableLimit,
    BigDecimal invoiceRemaining,
    BigDecimal overdueInvoiceRemaining) {}
