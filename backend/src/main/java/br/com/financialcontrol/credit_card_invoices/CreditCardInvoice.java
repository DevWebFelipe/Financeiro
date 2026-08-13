package br.com.financialcontrol.credit_card_invoices;

import br.com.financialcontrol.credit_cards.CreditCard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credit_card_invoices")
@Getter
@Setter
@NoArgsConstructor
public class CreditCardInvoice {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_card_id", nullable = false)
  private CreditCard creditCard;

  @Column(name = "reference_year", nullable = false)
  private int referenceYear;

  @Column(name = "reference_month", nullable = false)
  private int referenceMonth;

  @Column(name = "closing_date", nullable = false)
  private LocalDate closingDate;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CreditCardInvoiceStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
