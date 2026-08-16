package br.com.financialcontrol.credit_card_invoice_agreements;

import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_cards.CreditCard;
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
@Table(name = "credit_card_invoice_agreements")
@Getter
@Setter
@NoArgsConstructor
public class CreditCardInvoiceAgreement {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_card_id", nullable = false)
  private CreditCard creditCard;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_invoice_id", nullable = false)
  private CreditCardInvoice sourceInvoice;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "expense_id", nullable = false)
  private Expense expense;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CreditCardInvoiceAgreementStatus status;

  @Column(name = "entry_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal entryAmount;

  @Column(name = "financed_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal financedAmount;

  @Column(name = "installment_count", nullable = false)
  private int installmentCount;

  @Column(name = "installment_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal installmentAmount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "superseded_by_agreement_id")
  private CreditCardInvoiceAgreement supersededByAgreement;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
