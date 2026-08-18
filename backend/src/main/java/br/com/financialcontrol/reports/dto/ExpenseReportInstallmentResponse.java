package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.expenses.ExpenseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseReportInstallmentResponse(
    UUID id,
    int installmentNumber,
    int totalInstallments,
    LocalDate dueDate,
    BigDecimal original,
    BigDecimal discount,
    BigDecimal surcharge,
    BigDecimal obligation,
    BigDecimal paid,
    BigDecimal remaining,
    ExpenseStatus status) {}
