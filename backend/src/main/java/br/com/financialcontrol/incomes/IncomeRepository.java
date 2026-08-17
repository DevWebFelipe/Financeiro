package br.com.financialcontrol.incomes;

import br.com.financialcontrol.expenses.ResponsibleType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncomeRepository extends JpaRepository<Income, UUID> {

  Optional<Income> findByIdAndUserId(UUID id, UUID userId);

  /**
   * Lock pessimista (SELECT FOR UPDATE) para accrual, receipt, reverse de movimentação, cancel e
   * PUT. Impede duas transições concorrentes sobre a mesma duplicata sem coluna de versão (RN167).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Income i WHERE i.id = :id AND i.userId = :userId")
  Optional<Income> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

  @Query(
      """
      SELECT i FROM Income i
      WHERE i.userId = :userId
        AND (:status IS NULL OR i.status = :status)
        AND (:categoryId IS NULL OR i.category.id = :categoryId)
        AND (:accountId IS NULL OR i.account.id = :accountId)
        AND (CAST(:startDate AS LocalDate) IS NULL OR i.expectedDate >= :startDate)
        AND (CAST(:endDate AS LocalDate) IS NULL OR i.expectedDate <= :endDate)
      """)
  Page<Income> searchByUser(
      @Param("userId") UUID userId,
      @Param("status") IncomeStatus status,
      @Param("categoryId") UUID categoryId,
      @Param("accountId") UUID accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      Pageable pageable);

  String RECEIVABLES_PREDICATE =
      """
      i.userId = :userId
        AND i.status = :status
        AND (:categoryId IS NULL OR i.category.id = :categoryId)
        AND (:responsibleType IS NULL OR i.responsibleType = :responsibleType)
        AND (:responsibleName IS NULL OR i.responsibleName = :responsibleName)
        AND (CAST(:expectedMin AS LocalDate) IS NULL OR i.expectedDate >= :expectedMin)
        AND (CAST(:expectedMax AS LocalDate) IS NULL OR i.expectedDate <= :expectedMax)
        AND (:accountId IS NULL OR EXISTS (
              SELECT 1 FROM IncomeMovement rec
              WHERE rec.income = i
                AND rec.userId = i.userId
                AND rec.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT
                AND rec.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE
                AND rec.account.id = :accountId
            ))
        AND ((CAST(:receivedMin AS LocalDate) IS NULL AND CAST(:receivedMax AS LocalDate) IS NULL)
             OR EXISTS (
              SELECT 1 FROM IncomeMovement recDate
              WHERE recDate.income = i
                AND recDate.userId = i.userId
                AND recDate.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT
                AND (CAST(:receivedMin AS LocalDate) IS NULL OR recDate.movementDate >= :receivedMin)
                AND (CAST(:receivedMax AS LocalDate) IS NULL OR recDate.movementDate <= :receivedMax)
            ))
      """;

  @EntityGraph(attributePaths = {"category", "account"})
  @Query("SELECT i FROM Income i WHERE " + RECEIVABLES_PREDICATE)
  Page<Income> searchReceivables(
      @Param("userId") UUID userId,
      @Param("status") IncomeStatus status,
      @Param("categoryId") UUID categoryId,
      @Param("accountId") UUID accountId,
      @Param("responsibleType") ResponsibleType responsibleType,
      @Param("responsibleName") String responsibleName,
      @Param("expectedMin") LocalDate expectedMin,
      @Param("expectedMax") LocalDate expectedMax,
      @Param("receivedMin") LocalDate receivedMin,
      @Param("receivedMax") LocalDate receivedMax,
      Pageable pageable);

  @Query(
      """
      SELECT
        COALESCE(SUM(CASE WHEN i.status = br.com.financialcontrol.incomes.IncomeStatus.EXPECTED AND i.expectedDate >= :today THEN (
          i.amount
          + COALESCE((SELECT SUM(acc.amount) FROM IncomeMovement acc WHERE acc.income = i AND acc.userId = i.userId AND acc.type = br.com.financialcontrol.incomes.IncomeMovementType.ACCRUAL AND acc.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE), 0)
          - COALESCE((SELECT SUM(rcp.amount) FROM IncomeMovement rcp WHERE rcp.income = i AND rcp.userId = i.userId AND rcp.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT AND rcp.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE), 0)
        ) ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN i.status = br.com.financialcontrol.incomes.IncomeStatus.EXPECTED AND i.expectedDate < :today THEN (
          i.amount
          + COALESCE((SELECT SUM(acc.amount) FROM IncomeMovement acc WHERE acc.income = i AND acc.userId = i.userId AND acc.type = br.com.financialcontrol.incomes.IncomeMovementType.ACCRUAL AND acc.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE), 0)
          - COALESCE((SELECT SUM(rcp.amount) FROM IncomeMovement rcp WHERE rcp.income = i AND rcp.userId = i.userId AND rcp.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT AND rcp.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE), 0)
        ) ELSE 0 END), 0),
        COALESCE(SUM(COALESCE((SELECT SUM(rcp.amount) FROM IncomeMovement rcp WHERE rcp.income = i AND rcp.userId = i.userId AND rcp.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT AND rcp.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE), 0)), 0)
      FROM Income i
      WHERE
      """
          + RECEIVABLES_PREDICATE)
  List<Object[]> sumReceivableAmounts(
      @Param("userId") UUID userId,
      @Param("status") IncomeStatus status,
      @Param("categoryId") UUID categoryId,
      @Param("accountId") UUID accountId,
      @Param("responsibleType") ResponsibleType responsibleType,
      @Param("responsibleName") String responsibleName,
      @Param("expectedMin") LocalDate expectedMin,
      @Param("expectedMax") LocalDate expectedMax,
      @Param("receivedMin") LocalDate receivedMin,
      @Param("receivedMax") LocalDate receivedMax,
      @Param("today") LocalDate today);
}
