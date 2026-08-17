package br.com.financialcontrol.receivables.dto;

import java.math.BigDecimal;

public record ReceivableSummaryResponse(
    BigDecimal futureAmount,
    BigDecimal overdueAmount,
    BigDecimal totalReceivableAmount,
    BigDecimal receivedAmount) {}
