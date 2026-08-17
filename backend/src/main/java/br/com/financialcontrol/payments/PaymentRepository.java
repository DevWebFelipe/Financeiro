package br.com.financialcontrol.payments;

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

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT p FROM Payment p
      WHERE p.id = :id
        AND p.userId = :userId
      """)
  Optional<Payment> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

  /** Historical list for an expense (ACTIVE and REVERSED). Phase 7 callers remain valid. */
  List<Payment> findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(UUID expenseId, UUID userId);

  List<Payment> findAllByExpense_IdAndUserIdOrderByCreatedAtAscIdAsc(UUID expenseId, UUID userId);

  List<Payment> findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
      UUID installmentId, UUID userId);

  List<Payment> findAllByInstallment_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
      UUID installmentId, UUID userId, PaymentStatus status);

  List<Payment> findAllByExpense_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
      UUID expenseId, UUID userId, PaymentStatus status);

  /**
   * Phase 7 installment total used by {@code ExpenseService#pay}. All Phase 7 payments are ACTIVE;
   * Phase 8 Service must prefer {@link #sumActiveAmountByInstallmentIdAndUserId}.
   */
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
      WHERE p.installment.id = :installmentId
        AND p.userId = :userId
        AND p.status = br.com.financialcontrol.payments.PaymentStatus.ACTIVE
      """)
  BigDecimal sumActiveAmountByInstallmentIdAndUserId(
      @Param("installmentId") UUID installmentId, @Param("userId") UUID userId);

  /**
   * Phase 7 account debit total (RN216). Does not filter {@code payments.status}. Kept for {@code
   * AccountService} until Phase 8 balance (RN240) is implemented in Service.
   */
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

  @Query(
      """
      SELECT COALESCE(SUM(p.amount), 0)
      FROM Payment p
      WHERE p.userId = :userId
        AND p.account.id = :accountId
        AND p.status = br.com.financialcontrol.payments.PaymentStatus.ACTIVE
        AND p.expense.status NOT IN (
            br.com.financialcontrol.expenses.ExpenseStatus.CANCELLED,
            br.com.financialcontrol.expenses.ExpenseStatus.REFUNDED)
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR p.paymentDate <= :asOfDate)
      """)
  BigDecimal sumActiveValidExpensePaymentsByAccountIdAndUserIdAsOf(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  boolean existsByAccount_IdAndUserId(UUID accountId, UUID userId);
}
