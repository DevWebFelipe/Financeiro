package br.com.financialcontrol.financial_goals.dto;

import br.com.financialcontrol.financial_goals.FinancialGoal;
import br.com.financialcontrol.financial_goals.FinancialGoalStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialGoalResponse(
    UUID id,
    UUID accountId,
    String name,
    String description,
    BigDecimal targetAmount,
    LocalDate targetDate,
    FinancialGoalStatus status,
    BigDecimal currentAmount,
    BigDecimal progressPercent,
    Instant createdAt,
    Instant updatedAt) {

  public static FinancialGoalResponse from(FinancialGoal goal, BigDecimal currentAmount) {
    BigDecimal current = currentAmount.setScale(2, RoundingMode.HALF_UP);
    return new FinancialGoalResponse(
        goal.getId(),
        goal.getAccount().getId(),
        goal.getName(),
        goal.getDescription(),
        goal.getTargetAmount(),
        goal.getTargetDate(),
        goal.getStatus(),
        current,
        FinancialGoal.progressPercent(current, goal.getTargetAmount()),
        goal.getCreatedAt(),
        goal.getUpdatedAt());
  }
}
