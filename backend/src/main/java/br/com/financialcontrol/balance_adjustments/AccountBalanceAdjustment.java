package br.com.financialcontrol.balance_adjustments;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_balance_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class AccountBalanceAdjustment {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "adjustment_date", nullable = false)
  private LocalDate adjustmentDate;

  @Column(name = "calculated_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal calculatedBalance;

  @Column(name = "reported_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal reportedBalance;

  @Column(name = "adjustment_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal adjustmentAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private BalanceAdjustmentStatus status = BalanceAdjustmentStatus.ACTIVE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
