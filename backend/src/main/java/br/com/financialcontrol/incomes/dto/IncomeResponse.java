package br.com.financialcontrol.incomes.dto;

import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.incomes.Income;
import br.com.financialcontrol.incomes.IncomeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeResponse(
    UUID id,
    UUID categoryId,
    UUID accountId,
    String description,
    BigDecimal amount,
    LocalDate expectedDate,
    LocalDate receivedDate,
    IncomeStatus status,
    ResponsibleType responsibleType,
    String responsibleName,
    String notes,
    Instant createdAt,
    Instant updatedAt) {

  public static IncomeResponse from(Income income) {
    return new IncomeResponse(
        income.getId(),
        income.getCategory().getId(),
        income.getAccount() == null ? null : income.getAccount().getId(),
        income.getDescription(),
        income.getAmount(),
        income.getExpectedDate(),
        income.getReceivedDate(),
        income.getStatus(),
        income.getResponsibleType(),
        income.getResponsibleName(),
        income.getNotes(),
        income.getCreatedAt(),
        income.getUpdatedAt());
  }
}
