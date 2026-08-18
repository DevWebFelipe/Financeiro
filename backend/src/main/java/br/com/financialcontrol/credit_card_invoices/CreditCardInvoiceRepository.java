package br.com.financialcontrol.credit_card_invoices;

import br.com.financialcontrol.credit_cards.CreditCard;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, UUID> {

  Optional<CreditCardInvoice> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM CreditCardInvoice i WHERE i.id = :id AND i.userId = :userId")
  Optional<CreditCardInvoice> findByIdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("userId") UUID userId);

  Optional<CreditCardInvoice> findByCreditCard_IdAndUserIdAndReferenceYearAndReferenceMonth(
      UUID creditCardId, UUID userId, int referenceYear, int referenceMonth);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT i FROM CreditCardInvoice i
      WHERE i.creditCard.id = :creditCardId
        AND i.userId = :userId
        AND i.referenceYear = :year
        AND i.referenceMonth = :month
      """)
  Optional<CreditCardInvoice> findByCardAndCycleForUpdate(
      @Param("creditCardId") UUID creditCardId,
      @Param("userId") UUID userId,
      @Param("year") int year,
      @Param("month") int month);

  List<CreditCardInvoice> findAllByCreditCard_IdAndUserIdOrderByClosingDateAscIdAsc(
      UUID creditCardId, UUID userId);

  Optional<CreditCardInvoice> findFirstByCreditCard_IdAndUserIdAndStatus(
      UUID creditCardId, UUID userId, CreditCardInvoiceStatus status);

  boolean existsByCreditCard_IdAndUserIdAndStatus(
      UUID creditCardId, UUID userId, CreditCardInvoiceStatus status);

  boolean existsByCreditCard(CreditCard creditCard);

  List<CreditCardInvoice> findAllByUserIdAndStatus(UUID userId, CreditCardInvoiceStatus status);

  List<CreditCardInvoice> findAllByStatus(CreditCardInvoiceStatus status);

  List<CreditCardInvoice> findAllByCreditCard_IdAndUserIdAndStatusInOrderByDueDateAscIdAsc(
      UUID creditCardId, UUID userId, List<CreditCardInvoiceStatus> statuses);

  @Query(
      """
      SELECT i FROM CreditCardInvoice i
      WHERE i.creditCard.id = :creditCardId
        AND i.userId = :userId
        AND (:year IS NULL OR i.referenceYear = :year)
        AND (:month IS NULL OR i.referenceMonth = :month)
        AND (:status IS NULL OR i.status = :status)
      ORDER BY i.closingDate ASC, i.id ASC
      """)
  List<CreditCardInvoice> searchByCard(
      @Param("creditCardId") UUID creditCardId,
      @Param("userId") UUID userId,
      @Param("year") Integer year,
      @Param("month") Integer month,
      @Param("status") CreditCardInvoiceStatus status);

  @Query(
      """
      SELECT DISTINCT i FROM CreditCardInvoice i
      JOIN FETCH i.creditCard
      WHERE i.userId = :userId
        AND i.status IN :statuses
      """)
  List<CreditCardInvoice> findAllByUserIdAndStatusInWithCard(
      @Param("userId") UUID userId,
      @Param("statuses") Collection<CreditCardInvoiceStatus> statuses);

  @Query(
      """
      SELECT DISTINCT i FROM CreditCardInvoice i
      JOIN FETCH i.creditCard
      WHERE i.userId = :userId
        AND i.status IN :statuses
        AND i.dueDate <= :rangeEnd
      """)
  List<CreditCardInvoice> findAllByUserIdAndStatusInWithCardDueOnOrBefore(
      @Param("userId") UUID userId,
      @Param("statuses") Collection<CreditCardInvoiceStatus> statuses,
      @Param("rangeEnd") LocalDate rangeEnd);

  @Query(
      """
      SELECT DISTINCT i FROM CreditCardInvoice i
      JOIN FETCH i.creditCard
      WHERE i.userId = :userId
        AND i.closingDate >= :startDate
        AND i.closingDate <= :endDate
        AND (:creditCardId IS NULL OR i.creditCard.id = :creditCardId)
      ORDER BY i.closingDate ASC, i.id ASC
      """)
  List<CreditCardInvoice> findAllByUserIdAndClosingDateBetween(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("creditCardId") UUID creditCardId);
}
