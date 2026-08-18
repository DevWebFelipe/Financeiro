package br.com.financialcontrol.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardReportCreditApplicationResponse(
    UUID id,
    UUID creditId,
    UUID invoiceId,
    UUID installmentId,
    BigDecimal amount,
    Instant createdAt) {}
