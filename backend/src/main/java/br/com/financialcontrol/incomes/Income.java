package br.com.financialcontrol.incomes;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.expenses.ResponsibleType;
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
@Table(name = "incomes")
@Getter
@Setter
@NoArgsConstructor
public class Income {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id")
  private Account account;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "expected_date", nullable = false)
  private LocalDate expectedDate;

  @Column(name = "received_date")
  private LocalDate receivedDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private IncomeStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "responsible_type", nullable = false)
  private ResponsibleType responsibleType;

  @Column(name = "responsible_name")
  private String responsibleName;

  @Column(name = "notes")
  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
