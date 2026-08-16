package br.com.financialcontrol.credit_card_invoices;

import br.com.financialcontrol.expenses.AdjustmentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoiceAdjustmentRepository
    extends JpaRepository<CreditCardInvoiceAdjustment, UUID> {

  List<CreditCardInvoiceAdjustment> findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(
      UUID invoiceId, UUID userId);

  Optional<CreditCardInvoiceAdjustment> findByIdAndInvoice_IdAndUserId(
      UUID id, UUID invoiceId, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT a FROM CreditCardInvoiceAdjustment a
      WHERE a.id = :id AND a.invoice.id = :invoiceId AND a.userId = :userId
      """)
  Optional<CreditCardInvoiceAdjustment> findOwned(
      @Param("id") UUID id, @Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

  List<CreditCardInvoiceAdjustment> findAllByInvoice_IdAndUserIdAndStatus(
      UUID invoiceId, UUID userId, AdjustmentStatus status);
}
