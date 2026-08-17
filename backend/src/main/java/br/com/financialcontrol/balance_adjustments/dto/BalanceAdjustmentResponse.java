package br.com.financialcontrol.balance_adjustments.dto;

import br.com.financialcontrol.balance_adjustments.AccountBalanceAdjustment;
import br.com.financialcontrol.balance_adjustments.BalanceAdjustmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BalanceAdjustmentResponse(
    UUID id,
    UUID accountId,
    LocalDate adjustmentDate,
    BigDecimal calculatedBalance,
    BigDecimal reportedBalance,
    BigDecimal adjustmentAmount,
    BalanceAdjustmentStatus status,
    Instant createdAt,
    Instant updatedAt) {

  public static BalanceAdjustmentResponse from(AccountBalanceAdjustment adjustment) {
    return new BalanceAdjustmentResponse(
        adjustment.getId(),
        adjustment.getAccount().getId(),
        adjustment.getAdjustmentDate(),
        adjustment.getCalculatedBalance(),
        adjustment.getReportedBalance(),
        adjustment.getAdjustmentAmount(),
        adjustment.getStatus(),
        adjustment.getCreatedAt(),
        adjustment.getUpdatedAt());
  }
}
