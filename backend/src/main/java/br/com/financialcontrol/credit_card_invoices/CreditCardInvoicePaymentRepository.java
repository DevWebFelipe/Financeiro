package br.com.financialcontrol.credit_card_invoices;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoicePaymentRepository
    extends JpaRepository<CreditCardInvoicePayment, UUID> {

  List<CreditCardInvoicePayment> findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(
      UUID invoiceId, UUID userId);

  Optional<CreditCardInvoicePayment> findByIdAndInvoice_IdAndUserId(
      UUID id, UUID invoiceId, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT p FROM CreditCardInvoicePayment p
      WHERE p.id = :id AND p.invoice.id = :invoiceId AND p.userId = :userId
      """)
  Optional<CreditCardInvoicePayment> findByIdAndInvoice_IdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(p.amount), 0)
      FROM CreditCardInvoicePayment p
      WHERE p.account.id = :accountId
        AND p.userId = :userId
        AND p.status = br.com.financialcontrol.credit_card_invoices.InvoicePaymentStatus.ACTIVE
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR p.paymentDate <= :asOfDate)
      """)
  BigDecimal sumActiveAmountByAccountIdAndUserIdAsOf(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  boolean existsByAccount_IdAndUserId(UUID accountId, UUID userId);

  @Query(
      """
      SELECT DISTINCT p FROM CreditCardInvoicePayment p
      JOIN FETCH p.invoice i
      JOIN FETCH i.creditCard
      JOIN FETCH p.account
      WHERE p.userId = :userId
        AND p.paymentDate >= :startDate
        AND p.paymentDate <= :endDate
        AND (:creditCardId IS NULL OR i.creditCard.id = :creditCardId)
      ORDER BY p.paymentDate ASC, p.id ASC
      """)
  List<CreditCardInvoicePayment> findAllByUserIdAndPaymentDateBetween(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("creditCardId") UUID creditCardId);
}
