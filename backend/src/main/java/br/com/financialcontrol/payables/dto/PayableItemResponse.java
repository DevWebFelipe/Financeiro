package br.com.financialcontrol.payables.dto;

import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.payables.PayableItemType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableItemResponse(
    UUID id,
    PayableItemType type,
    UUID expenseId,
    UUID creditCardId,
    UUID categoryId,
    UUID accountId,
    PaymentMethod paymentMethod,
    String name,
    LocalDate purchaseDate,
    LocalDate dueDate,
    BigDecimal originalAmount,
    BigDecimal paidAmount,
    BigDecimal remainingAmount,
    String status,
    boolean overdue,
    ResponsibleType responsibleType,
    String responsibleName) {}
