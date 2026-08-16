package br.com.financialcontrol.credit_card_invoices.dto;

import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreditCardInvoiceResponse(
    UUID id,
    UUID creditCardId,
    int referenceYear,
    int referenceMonth,
    LocalDate closingDate,
    LocalDate dueDate,
    CreditCardInvoiceStatus status,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal remainingAmount,
    Instant createdAt,
    Instant updatedAt) {

  public static CreditCardInvoiceResponse from(
      CreditCardInvoice invoice,
      BigDecimal totalAmount,
      BigDecimal paidAmount,
      BigDecimal remainingAmount) {
    return new CreditCardInvoiceResponse(
        invoice.getId(),
        invoice.getCreditCard().getId(),
        invoice.getReferenceYear(),
        invoice.getReferenceMonth(),
        invoice.getClosingDate(),
        invoice.getDueDate(),
        invoice.getStatus(),
        totalAmount,
        paidAmount,
        remainingAmount,
        invoice.getCreatedAt(),
        invoice.getUpdatedAt());
  }
}
