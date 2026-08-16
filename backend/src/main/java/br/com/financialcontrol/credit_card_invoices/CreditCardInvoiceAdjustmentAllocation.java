package br.com.financialcontrol.credit_card_invoices;

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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credit_card_invoice_adjustment_allocations")
@Getter
@Setter
@NoArgsConstructor
public class CreditCardInvoiceAdjustmentAllocation {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_adjustment_id", nullable = false)
  private CreditCardInvoiceAdjustment invoiceAdjustment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "installment_id", nullable = false)
  private ExpenseInstallment installment;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
