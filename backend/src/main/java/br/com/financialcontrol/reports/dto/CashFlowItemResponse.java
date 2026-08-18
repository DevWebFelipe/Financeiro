package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.reports.CashFlowType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashFlowItemResponse(
    UUID id,
    CashFlowType type,
    LocalDate date,
    BigDecimal amount,
    UUID accountId,
    String description) {}
