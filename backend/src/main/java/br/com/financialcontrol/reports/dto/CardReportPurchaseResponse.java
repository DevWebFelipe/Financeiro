package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.ResponsibleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CardReportPurchaseResponse(
    UUID expenseId,
    String description,
    LocalDate expenseDate,
    BigDecimal original,
    ResponsibleType responsibleType,
    String responsibleName,
    ExpenseStatus status,
    int totalInstallments,
    List<CardReportPurchaseInstallmentResponse> installments) {}
