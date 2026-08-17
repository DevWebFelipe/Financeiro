package br.com.financialcontrol.financial_goals.dto;

import br.com.financialcontrol.financial_goals.GoalContribution;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalContributionResponse(
    UUID id,
    UUID goalId,
    BigDecimal amount,
    LocalDate contributionDate,
    String notes,
    Instant createdAt) {

  public static GoalContributionResponse from(GoalContribution contribution) {
    return new GoalContributionResponse(
        contribution.getId(),
        contribution.getGoal().getId(),
        contribution.getAmount(),
        contribution.getContributionDate(),
        contribution.getNotes(),
        contribution.getCreatedAt());
  }
}
