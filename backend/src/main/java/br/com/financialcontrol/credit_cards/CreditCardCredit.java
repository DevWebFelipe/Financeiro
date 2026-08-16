package br.com.financialcontrol.credit_cards;

import br.com.financialcontrol.expenses.Expense;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credit_card_credits")
@Getter
@Setter
@NoArgsConstructor
public class CreditCardCredit {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_card_id", nullable = false)
  private CreditCard creditCard;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "reason", nullable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false)
  private CreditCardCreditOrigin origin;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id")
  private Expense expense;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
