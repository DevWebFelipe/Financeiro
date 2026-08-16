package br.com.financialcontrol.credit_cards;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardCreditApplicationRepository
    extends JpaRepository<CreditCardCreditApplication, UUID> {

  List<CreditCardCreditApplication> findAllByCredit_IdAndUserId(UUID creditId, UUID userId);

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
}
