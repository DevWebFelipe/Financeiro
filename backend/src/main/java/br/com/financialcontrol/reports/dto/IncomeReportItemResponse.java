package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.incomes.IncomeStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeReportItemResponse(
    UUID id,
    String description,
    IncomeStatus status,
    UUID categoryId,
    ResponsibleType responsibleType,
    String responsibleName,
    LocalDate expectedDate,
    BigDecimal amount,
    BigDecimal accruedAmount,
    BigDecimal receivedAmount,
    BigDecimal remainingAmount,
    @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal periodReceivedAmount) {}
