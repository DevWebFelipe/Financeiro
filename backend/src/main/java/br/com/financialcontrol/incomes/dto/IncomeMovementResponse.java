package br.com.financialcontrol.incomes.dto;

import br.com.financialcontrol.incomes.IncomeMovement;
import br.com.financialcontrol.incomes.IncomeMovementStatus;
import br.com.financialcontrol.incomes.IncomeMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeMovementResponse(
    UUID id,
    UUID incomeId,
    IncomeMovementType type,
    IncomeMovementStatus status,
    BigDecimal amount,
    LocalDate movementDate,
    UUID accountId,
    Instant createdAt,
    Instant updatedAt,
    Instant reversedAt) {

  public static IncomeMovementResponse from(IncomeMovement movement) {
    return new IncomeMovementResponse(
        movement.getId(),
        movement.getIncome().getId(),
        movement.getType(),
        movement.getStatus(),
        movement.getAmount(),
        movement.getMovementDate(),
        movement.getAccount() == null ? null : movement.getAccount().getId(),
        movement.getCreatedAt(),
        movement.getUpdatedAt(),
        movement.getReversedAt());
  }
}
