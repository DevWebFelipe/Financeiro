package br.com.financialcontrol.reports.dto;

import java.math.BigDecimal;

public record CashFlowSummaryResponse(BigDecimal totalIn, BigDecimal totalOut, BigDecimal net) {}
