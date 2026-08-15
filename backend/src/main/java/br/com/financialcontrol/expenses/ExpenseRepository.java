package br.com.financialcontrol.expenses;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

  Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.userId = :userId")
  Optional<Expense> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

  @Query(
      """
      SELECT e FROM Expense e
      WHERE e.userId = :userId
        AND (:status IS NULL OR e.status = :status)
        AND (:categoryId IS NULL OR e.category.id = :categoryId)
        AND (:accountId IS NULL OR e.account.id = :accountId)
        AND (:responsibleType IS NULL OR e.responsibleType = :responsibleType)
        AND (:paymentMethod IS NULL OR e.paymentMethod = :paymentMethod)
        AND (
              (CAST(:startDate AS LocalDate) IS NULL AND CAST(:endDate AS LocalDate) IS NULL)
           OR EXISTS (
                SELECT 1 FROM ExpenseInstallment i
                WHERE i.expense = e
                  AND i.userId = :userId
                  AND (CAST(:startDate AS LocalDate) IS NULL OR i.dueDate >= :startDate)
                  AND (CAST(:endDate AS LocalDate) IS NULL OR i.dueDate <= :endDate)
              )
            )
      """)
  Page<Expense> searchByUser(
      @Param("userId") UUID userId,
      @Param("status") ExpenseStatus status,
      @Param("categoryId") UUID categoryId,
      @Param("accountId") UUID accountId,
      @Param("responsibleType") ResponsibleType responsibleType,
      @Param("paymentMethod") PaymentMethod paymentMethod,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      Pageable pageable);
}
