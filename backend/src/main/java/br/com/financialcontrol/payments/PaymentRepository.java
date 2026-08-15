package br.com.financialcontrol.payments;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

  List<Payment> findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(UUID expenseId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(p.amount), 0)
      FROM Payment p
      WHERE p.installment.id = :installmentId
        AND p.userId = :userId
      """)
  BigDecimal sumAmountByInstallmentIdAndUserId(
      @Param("installmentId") UUID installmentId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(p.amount), 0)
      FROM Payment p
      WHERE p.userId = :userId
        AND p.account.id = :accountId
        AND p.expense.status NOT IN (
            br.com.financialcontrol.expenses.ExpenseStatus.CANCELLED,
            br.com.financialcontrol.expenses.ExpenseStatus.REFUNDED)
      """)
  BigDecimal sumValidExpensePaymentsByAccountIdAndUserId(
      @Param("accountId") UUID accountId, @Param("userId") UUID userId);
}
