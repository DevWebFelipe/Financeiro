package br.com.financialcontrol.payments.dto;

import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID expenseId,
    UUID installmentId,
    UUID accountId,
    BigDecimal amount,
    LocalDate paymentDate,
    PaymentStatus status,
    String notes,
    Instant createdAt) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getExpense().getId(),
        payment.getInstallment().getId(),
        payment.getAccount().getId(),
        payment.getAmount(),
        payment.getPaymentDate(),
        payment.getStatus(),
        payment.getNotes(),
        payment.getCreatedAt());
  }
}
