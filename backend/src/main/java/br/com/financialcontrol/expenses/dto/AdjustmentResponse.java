package br.com.financialcontrol.expenses.dto;

import br.com.financialcontrol.expenses.AdjustmentStatus;
import br.com.financialcontrol.expenses.AdjustmentType;
import br.com.financialcontrol.expenses.ExpenseInstallmentAdjustment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdjustmentResponse(
    UUID id,
    UUID expenseId,
    UUID installmentId,
    AdjustmentType type,
    BigDecimal amount,
    AdjustmentStatus status,
    Instant createdAt) {

  public static AdjustmentResponse from(ExpenseInstallmentAdjustment adjustment) {
    return new AdjustmentResponse(
        adjustment.getId(),
        adjustment.getInstallment().getExpense().getId(),
        adjustment.getInstallment().getId(),
        adjustment.getType(),
        adjustment.getAmount(),
        adjustment.getStatus(),
        adjustment.getCreatedAt());
  }
}
