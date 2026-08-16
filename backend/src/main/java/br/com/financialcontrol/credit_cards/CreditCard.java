package br.com.financialcontrol.credit_cards;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credit_cards")
@Getter
@Setter
@NoArgsConstructor
public class CreditCard {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "holder_name", nullable = false)
  private String holderName;

  @Column(name = "last_four_digits")
  private String lastFourDigits;

  @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
  private BigDecimal creditLimit;

  @Column(name = "closing_day", nullable = false)
  private int closingDay;

  @Column(name = "due_day", nullable = false)
  private int dueDay;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
