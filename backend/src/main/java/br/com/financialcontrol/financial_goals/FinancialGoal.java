package br.com.financialcontrol.financial_goals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
}
