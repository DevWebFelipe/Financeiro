package br.com.financialcontrol.expenses.dto;

import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    UUID categoryId,
    UUID accountId,
    UUID creditCardId,
    String description,
    BigDecimal totalAmount,
    LocalDate expenseDate,
    LocalDate dueDate,
    PaymentMethod paymentMethod,
    ExpenseStatus status,
    ResponsibleType responsibleType,
    String responsibleName,
    String barcode,
    String notes,
    boolean overdue,
    UUID installmentId,
    Instant createdAt,
    Instant updatedAt) {

  public static ExpenseResponse from(Expense expense, UUID installmentId, boolean overdue) {
    return new ExpenseResponse(
        expense.getId(),
        expense.getCategory().getId(),
        expense.getAccount() == null ? null : expense.getAccount().getId(),
        expense.getCreditCard() == null ? null : expense.getCreditCard().getId(),
        expense.getDescription(),
        expense.getTotalAmount(),
        expense.getExpenseDate(),
        expense.getDueDate(),
        expense.getPaymentMethod(),
        expense.getStatus(),
        expense.getResponsibleType(),
        expense.getResponsibleName(),
        expense.getBarcode(),
        expense.getNotes(),
        overdue,
        installmentId,
        expense.getCreatedAt(),
        expense.getUpdatedAt());
  }
}
