package br.com.financialcontrol.credit_cards;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardCreditApplicationRepository
    extends JpaRepository<CreditCardCreditApplication, UUID> {

  List<CreditCardCreditApplication> findAllByCredit_IdAndUserId(UUID creditId, UUID userId);

  List<CreditCardCreditApplication> findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(
      UUID invoiceId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM CreditCardCreditApplication a
      WHERE a.credit.id = :creditId AND a.userId = :userId
      """)
  BigDecimal sumAmountByCreditIdAndUserId(
      @Param("creditId") UUID creditId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM CreditCardCreditApplication a
      WHERE a.installment.id = :installmentId AND a.userId = :userId
      """)
  BigDecimal sumAmountByInstallmentIdAndUserId(
      @Param("installmentId") UUID installmentId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM CreditCardCreditApplication a
      WHERE a.installment.expense.id = :expenseId AND a.userId = :userId
      """)
  BigDecimal sumAmountByExpenseIdAndUserId(
      @Param("expenseId") UUID expenseId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT DISTINCT a FROM CreditCardCreditApplication a
      JOIN FETCH a.credit c
      JOIN FETCH c.creditCard
      JOIN FETCH a.invoice
      JOIN FETCH a.installment
      WHERE a.userId = :userId
        AND a.createdAt >= :startInstant
        AND a.createdAt < :endInstant
        AND (:creditCardId IS NULL OR c.creditCard.id = :creditCardId)
      ORDER BY a.createdAt ASC, a.id ASC
      """)
  List<CreditCardCreditApplication> findAllByUserIdAndCreatedAtBetween(
      @Param("userId") UUID userId,
      @Param("startInstant") Instant startInstant,
      @Param("endInstant") Instant endInstant,
      @Param("creditCardId") UUID creditCardId);
}
