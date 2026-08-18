package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceReportHeaderResponse(
    int referenceYear,
    int referenceMonth,
    LocalDate closingDate,
    LocalDate dueDate,
    CreditCardInvoiceStatus status,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal remainingAmount) {}
