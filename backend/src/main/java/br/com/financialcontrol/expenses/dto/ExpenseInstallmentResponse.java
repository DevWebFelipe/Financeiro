package br.com.financialcontrol.expenses.dto;

import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseInstallmentResponse(
    UUID id,
    UUID expenseId,
    int installmentNumber,
    int totalInstallments,
    BigDecimal amount,
    BigDecimal remainingAmount,
    LocalDate dueDate,
    ExpenseStatus status,
    boolean overdue,
    Instant createdAt,
    Instant updatedAt) {

  public static ExpenseInstallmentResponse from(
      ExpenseInstallment installment, BigDecimal remainingAmount, boolean overdue) {
    return new ExpenseInstallmentResponse(
        installment.getId(),
        installment.getExpense().getId(),
        installment.getInstallmentNumber(),
        installment.getTotalInstallments(),
        installment.getAmount(),
        remainingAmount,
        installment.getDueDate(),
        installment.getStatus(),
        overdue,
        installment.getCreatedAt(),
        installment.getUpdatedAt());
  }
}
