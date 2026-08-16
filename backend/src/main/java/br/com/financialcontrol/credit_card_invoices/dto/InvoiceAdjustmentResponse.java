package br.com.financialcontrol.credit_card_invoices.dto;

import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceAdjustment;
import br.com.financialcontrol.expenses.AdjustmentStatus;
import br.com.financialcontrol.expenses.AdjustmentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceAdjustmentResponse(
    UUID id,
    UUID invoiceId,
    AdjustmentType type,
    BigDecimal amount,
    String reason,
    AdjustmentStatus status,
    Instant createdAt) {

  public static InvoiceAdjustmentResponse from(CreditCardInvoiceAdjustment adjustment) {
    return new InvoiceAdjustmentResponse(
        adjustment.getId(),
        adjustment.getInvoice().getId(),
        adjustment.getType(),
        adjustment.getAmount(),
        adjustment.getReason(),
        adjustment.getStatus(),
        adjustment.getCreatedAt());
  }
}
