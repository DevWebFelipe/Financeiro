package br.com.financialcontrol.expenses;

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

public interface ExpenseInstallmentRepository extends JpaRepository<ExpenseInstallment, UUID> {

  List<ExpenseInstallment> findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
      UUID expenseId, UUID userId);

  List<ExpenseInstallment> findAllByExpense_IdInAndUserIdOrderByExpense_IdAscInstallmentNumberAsc(
      Collection<UUID> expenseIds, UUID userId);

  Optional<ExpenseInstallment> findByIdAndExpense_IdAndUserId(UUID id, UUID expenseId, UUID userId);

  List<ExpenseInstallment> findAllByInvoice_IdAndUserIdOrderByDueDateAscIdAsc(
      UUID invoiceId, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT i FROM ExpenseInstallment i
      WHERE i.invoice.id = :invoiceId AND i.userId = :userId
      ORDER BY i.dueDate ASC, i.id ASC
      """)
  List<ExpenseInstallment> findAllByInvoiceIdAndUserIdForUpdate(
      @Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

  List<ExpenseInstallment> findAllByExpense_CreditCard_IdAndUserId(UUID creditCardId, UUID userId);

  Optional<ExpenseInstallment> findByExpense_IdAndUserIdAndInstallmentNumber(
      UUID expenseId, UUID userId, int installmentNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT i FROM ExpenseInstallment i
      WHERE i.id = :installmentId
        AND i.expense.id = :expenseId
        AND i.userId = :userId
      """)
  Optional<ExpenseInstallment> findByIdAndExpense_IdAndUserIdForUpdate(
      @Param("installmentId") UUID installmentId,
      @Param("expenseId") UUID expenseId,
      @Param("userId") UUID userId);

  /**
   * Phase 7 lock for the single 1/1 installment. Kept for existing pay/cancel/refund flows until
   * Phase 8 Service migrates to {@link #findByIdAndExpense_IdAndUserIdForUpdate}.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT i FROM ExpenseInstallment i
      WHERE i.expense.id = :expenseId
        AND i.userId = :userId
        AND i.installmentNumber = 1
      """)
  Optional<ExpenseInstallment> findSingleByExpenseIdAndUserIdForUpdate(
      @Param("expenseId") UUID expenseId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT i FROM ExpenseInstallment i
      WHERE i.expense.id IN :expenseIds
        AND i.userId = :userId
        AND i.installmentNumber = 1
      """)
  List<ExpenseInstallment> findSingleByExpenseIdsAndUserId(
      @Param("expenseIds") Collection<UUID> expenseIds, @Param("userId") UUID userId);

  @Query(
      """
      SELECT DISTINCT i FROM ExpenseInstallment i
      JOIN FETCH i.expense e
      JOIN FETCH e.category
      LEFT JOIN FETCH e.account
      WHERE i.userId = :userId
        AND e.paymentMethod IN :paymentMethods
        AND e.status NOT IN :excludedStatuses
        AND i.status NOT IN :excludedStatuses
      """)
  List<ExpenseInstallment> findAllByUserIdAndPaymentMethodsExcludingStatuses(
      @Param("userId") UUID userId,
      @Param("paymentMethods") Collection<PaymentMethod> paymentMethods,
      @Param("excludedStatuses") Collection<ExpenseStatus> excludedStatuses);

  @Query(
      """
      SELECT DISTINCT i FROM ExpenseInstallment i
      JOIN FETCH i.expense e
      JOIN FETCH e.category
      LEFT JOIN FETCH e.account
      WHERE i.userId = :userId
        AND e.paymentMethod IN :paymentMethods
        AND e.status NOT IN :excludedStatuses
        AND i.status NOT IN :excludedStatuses
        AND i.dueDate <= :rangeEnd
      """)
  List<ExpenseInstallment> findAllByUserIdAndPaymentMethodsExcludingStatusesDueOnOrBefore(
      @Param("userId") UUID userId,
      @Param("paymentMethods") Collection<PaymentMethod> paymentMethods,
      @Param("excludedStatuses") Collection<ExpenseStatus> excludedStatuses,
      @Param("rangeEnd") LocalDate rangeEnd);
}
