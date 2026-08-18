package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.reports.InvoiceReportAllocationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceReportAllocationResponse(
    UUID id,
    InvoiceReportAllocationType type,
    UUID sourceId,
    UUID installmentId,
    BigDecimal amount,
    Instant createdAt) {}
