package br.com.financialcontrol.receivables.dto;

import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.incomes.IncomeStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableItemResponse(
    UUID id,
    UUID categoryId,
    UUID accountId,
    ResponsibleType responsibleType,
    String responsibleName,
    String description,
    BigDecimal amount,
    LocalDate expectedDate,
    LocalDate receivedDate,
    IncomeStatus status,
    boolean overdue) {}
