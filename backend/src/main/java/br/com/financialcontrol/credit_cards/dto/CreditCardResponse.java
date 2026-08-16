package br.com.financialcontrol.credit_cards.dto;

import br.com.financialcontrol.credit_cards.CreditCard;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditCardResponse(
    UUID id,
    String name,
    String holderName,
    String lastFourDigits,
    BigDecimal creditLimit,
    int closingDay,
    int dueDay,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {

  public static CreditCardResponse from(CreditCard card) {
    return new CreditCardResponse(
        card.getId(),
        card.getName(),
        card.getHolderName(),
        card.getLastFourDigits(),
        card.getCreditLimit(),
        card.getClosingDay(),
        card.getDueDay(),
        card.isActive(),
        card.getCreatedAt(),
        card.getUpdatedAt());
  }
}
