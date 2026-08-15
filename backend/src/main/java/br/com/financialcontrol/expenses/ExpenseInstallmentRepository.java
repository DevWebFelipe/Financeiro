package br.com.financialcontrol.expenses;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseInstallmentRepository extends JpaRepository<ExpenseInstallment, UUID> {

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

  Optional<ExpenseInstallment> findByExpense_IdAndUserIdAndInstallmentNumber(
      UUID expenseId, UUID userId, int installmentNumber);
}
