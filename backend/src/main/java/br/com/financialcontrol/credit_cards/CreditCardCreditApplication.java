package br.com.financialcontrol.credit_cards;

import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
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
@Table(name = "credit_card_credit_applications")
@Getter
@Setter
@NoArgsConstructor
public class CreditCardCreditApplication {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_id", nullable = false)
  private CreditCardCredit credit;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private CreditCardInvoice invoice;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "installment_id", nullable = false)
  private ExpenseInstallment installment;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
