package br.com.financialcontrol.payments;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "expense_id", nullable = false)
  private Expense expense;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "installment_id", nullable = false)
  private ExpenseInstallment installment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "payment_date", nullable = false)
  private LocalDate paymentDate;

  /**
   * Official values are not defined (docs/23 §269.1). No Java enum, CHECK, or validation. May be
   * null until a business decision is made.
   */
  @Column(name = "type")
  private String type;

  @Column(name = "notes")
  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
