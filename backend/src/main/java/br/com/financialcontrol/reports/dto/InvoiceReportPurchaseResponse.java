package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.expenses.ResponsibleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceReportPurchaseResponse(
    UUID expenseId,
    String description,
    LocalDate expenseDate,
    BigDecimal original,
    String categoryName,
    ResponsibleType responsibleType,
    String responsibleName,
    int installmentNumber,
    int totalInstallments,
    BigDecimal discount,
    BigDecimal surcharge) {}
