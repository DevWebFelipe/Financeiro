package br.com.financialcontrol.credit_card_invoice_agreements;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoiceAgreementRepository
    extends JpaRepository<CreditCardInvoiceAgreement, UUID> {

  Optional<CreditCardInvoiceAgreement> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT a FROM CreditCardInvoiceAgreement a
      WHERE a.id = :id AND a.userId = :userId
      """)
  Optional<CreditCardInvoiceAgreement> findByIdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("userId") UUID userId);

  List<CreditCardInvoiceAgreement> findAllBySourceInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(
      UUID sourceInvoiceId, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT a FROM CreditCardInvoiceAgreement a
      WHERE a.creditCard.id = :cardId
        AND a.userId = :userId
        AND a.status = br.com.financialcontrol.credit_card_invoice_agreements.CreditCardInvoiceAgreementStatus.ACTIVE
      ORDER BY a.createdAt ASC, a.id ASC
      """)
  List<CreditCardInvoiceAgreement> findAllActiveByCardForUpdate(
      @Param("cardId") UUID cardId, @Param("userId") UUID userId);

  boolean existsBySourceInvoice_IdAndUserIdAndStatusIn(
      UUID sourceInvoiceId, UUID userId, Collection<CreditCardInvoiceAgreementStatus> statuses);

  @Query(
      """
      SELECT a.expense.id
      FROM CreditCardInvoiceAgreement a
      WHERE a.userId = :userId
        AND a.expense.id IN :expenseIds
      """)
  List<UUID> findExpenseIdsByUserIdAndExpenseIdIn(
      @Param("userId") UUID userId, @Param("expenseIds") Collection<UUID> expenseIds);
}
