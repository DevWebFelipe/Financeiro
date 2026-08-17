package br.com.financialcontrol.financial_goals.dto;

import br.com.financialcontrol.financial_goals.GoalRedemption;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalRedemptionResponse(
    UUID id,
    UUID goalId,
    BigDecimal amount,
    LocalDate redemptionDate,
    String notes,
    Instant createdAt) {

  public static GoalRedemptionResponse from(GoalRedemption redemption) {
    return new GoalRedemptionResponse(
        redemption.getId(),
        redemption.getGoal().getId(),
        redemption.getAmount(),
        redemption.getRedemptionDate(),
        redemption.getNotes(),
        redemption.getCreatedAt());
  }
}
