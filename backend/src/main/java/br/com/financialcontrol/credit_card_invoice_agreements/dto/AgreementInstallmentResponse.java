package br.com.financialcontrol.credit_card_invoice_agreements.dto;

import br.com.financialcontrol.expenses.ExpenseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgreementInstallmentResponse(
    UUID id,
    UUID expenseId,
    int installmentNumber,
    int totalInstallments,
    BigDecimal amount,
    BigDecimal remainingAmount,
    LocalDate dueDate,
    ExpenseStatus status,
    UUID invoiceId,
    Instant createdAt,
    Instant updatedAt) {}
