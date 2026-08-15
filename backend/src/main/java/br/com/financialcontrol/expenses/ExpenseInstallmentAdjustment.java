package br.com.financialcontrol.expenses;

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
@Table(name = "expense_installment_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class ExpenseInstallmentAdjustment {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "installment_id", nullable = false)
  private ExpenseInstallment installment;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private AdjustmentType type;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AdjustmentStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
