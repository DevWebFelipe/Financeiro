package br.com.financialcontrol.credit_card_invoices;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
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
      """)
  BigDecimal sumActiveAmountByAccountIdAndUserId(
      @Param("accountId") UUID accountId, @Param("userId") UUID userId);
}
