package br.com.financialcontrol.financial_goals;

import br.com.financialcontrol.accounts.Account;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "financial_goals")
@Getter
@Setter
@NoArgsConstructor
public class FinancialGoal {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal targetAmount;

  @Column(name = "target_date")
  private LocalDate targetDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FinancialGoalStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static BigDecimal progressPercent(BigDecimal currentAmount, BigDecimal targetAmount) {
    return currentAmount
        .multiply(new BigDecimal("100"))
        .divide(targetAmount, 2, RoundingMode.HALF_UP);
  }
}
