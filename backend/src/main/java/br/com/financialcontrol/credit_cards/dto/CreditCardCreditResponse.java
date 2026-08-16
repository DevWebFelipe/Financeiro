package br.com.financialcontrol.credit_cards.dto;

import br.com.financialcontrol.credit_cards.CreditCardCredit;
import br.com.financialcontrol.credit_cards.CreditCardCreditOrigin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditCardCreditResponse(
    UUID id,
    UUID creditCardId,
    BigDecimal amount,
    BigDecimal remainingAmount,
    String reason,
    CreditCardCreditOrigin origin,
    UUID expenseId,
    Instant createdAt) {

  public static CreditCardCreditResponse from(CreditCardCredit credit, BigDecimal remainingAmount) {
    return new CreditCardCreditResponse(
        credit.getId(),
        credit.getCreditCard().getId(),
        credit.getAmount(),
        remainingAmount,
        credit.getReason(),
        credit.getOrigin(),
        credit.getExpense() == null ? null : credit.getExpense().getId(),
        credit.getCreatedAt());
  }
}
