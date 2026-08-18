package br.com.financialcontrol.credit_card_invoices;

import br.com.financialcontrol.expenses.AdjustmentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
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

  @Query(
      """
      SELECT DISTINCT a FROM CreditCardInvoiceAdjustment a
      JOIN FETCH a.invoice i
      JOIN FETCH i.creditCard
      WHERE a.userId = :userId
        AND a.createdAt >= :startInstant
        AND a.createdAt < :endInstant
        AND (:creditCardId IS NULL OR i.creditCard.id = :creditCardId)
      ORDER BY a.createdAt ASC, a.id ASC
      """)
  List<CreditCardInvoiceAdjustment> findAllByUserIdAndCreatedAtBetween(
      @Param("userId") UUID userId,
      @Param("startInstant") Instant startInstant,
      @Param("endInstant") Instant endInstant,
      @Param("creditCardId") UUID creditCardId);
}
