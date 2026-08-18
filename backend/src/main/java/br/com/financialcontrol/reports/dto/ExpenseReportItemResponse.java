package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.reports.ExpenseReportOrigin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseReportItemResponse(
    UUID id,
    String description,
    LocalDate expenseDate,
    PaymentMethod paymentMethod,
    ExpenseStatus status,
    UUID categoryId,
    UUID accountId,
    UUID creditCardId,
    ResponsibleType responsibleType,
    String responsibleName,
    ExpenseReportOrigin origin,
    BigDecimal periodOriginal,
    BigDecimal periodDiscount,
    BigDecimal periodSurcharge,
    BigDecimal periodObligation,
    BigDecimal periodPaid,
    BigDecimal periodRemaining,
    List<ExpenseReportInstallmentResponse> installments) {}
