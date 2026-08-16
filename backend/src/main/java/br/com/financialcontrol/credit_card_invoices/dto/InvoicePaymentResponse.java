package br.com.financialcontrol.credit_card_invoices.dto;

import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePayment;
import br.com.financialcontrol.credit_card_invoices.InvoicePaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoicePaymentResponse(
    UUID id,
    UUID invoiceId,
    UUID accountId,
    BigDecimal amount,
    LocalDate paymentDate,
    String notes,
    InvoicePaymentStatus status,
    Instant createdAt) {

  public static InvoicePaymentResponse from(CreditCardInvoicePayment payment) {
    return new InvoicePaymentResponse(
        payment.getId(),
        payment.getInvoice().getId(),
        payment.getAccount().getId(),
        payment.getAmount(),
        payment.getPaymentDate(),
        payment.getNotes(),
        payment.getStatus(),
        payment.getCreatedAt());
  }
}
