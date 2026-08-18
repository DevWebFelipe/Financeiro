package br.com.financialcontrol.incomes;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
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

public interface IncomeMovementRepository extends JpaRepository<IncomeMovement, UUID> {

  Optional<IncomeMovement> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT m FROM IncomeMovement m
      WHERE m.id = :id AND m.income.id = :incomeId AND m.userId = :userId
      """)
  Optional<IncomeMovement> findByIdAndIncome_IdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("incomeId") UUID incomeId, @Param("userId") UUID userId);

  @EntityGraph(attributePaths = {"account", "income"})
  @Query(
      """
      SELECT m FROM IncomeMovement m
      WHERE m.income.id = :incomeId AND m.userId = :userId
      """)
  Page<IncomeMovement> searchByIncomeIdAndUserId(
      @Param("incomeId") UUID incomeId, @Param("userId") UUID userId, Pageable pageable);

  @Query(
      """
      SELECT COALESCE(SUM(m.amount), 0)
      FROM IncomeMovement m
      WHERE m.income.id = :incomeId
        AND m.userId = :userId
        AND m.type = :type
        AND m.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE
      """)
  BigDecimal sumActiveAmountByIncomeIdAndUserIdAndType(
      @Param("incomeId") UUID incomeId,
      @Param("userId") UUID userId,
      @Param("type") IncomeMovementType type);

  @Query(
      """
      SELECT m.income.id, m.type, COALESCE(SUM(m.amount), 0)
      FROM IncomeMovement m
      WHERE m.userId = :userId
        AND m.income.id IN :incomeIds
        AND m.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE
      GROUP BY m.income.id, m.type
      """)
  List<Object[]> sumActiveAmountsByIncomeIdsAndUserId(
      @Param("userId") UUID userId, @Param("incomeIds") Collection<UUID> incomeIds);

  @Query(
      """
      SELECT COALESCE(SUM(m.amount), 0)
      FROM IncomeMovement m
      WHERE m.userId = :userId
        AND m.account.id = :accountId
        AND m.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT
        AND m.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR m.movementDate <= :asOfDate)
      """)
  BigDecimal sumActiveReceiptAmountByAccountIdAndUserIdAsOf(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  boolean existsByIncome_IdAndUserId(UUID incomeId, UUID userId);

  boolean existsByIncome_IdAndUserIdAndTypeAndStatus(
      UUID incomeId, UUID userId, IncomeMovementType type, IncomeMovementStatus status);

  boolean existsByAccount_IdAndUserIdAndType(UUID accountId, UUID userId, IncomeMovementType type);

  @EntityGraph(attributePaths = {"income", "income.category", "account"})
  @Query(
      """
      SELECT m FROM IncomeMovement m
      WHERE m.userId = :userId
        AND m.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT
        AND m.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE
        AND m.movementDate >= :startDate
        AND m.movementDate <= :endDate
      """)
  List<IncomeMovement> findActiveReceiptsByUserIdAndMovementDateBetween(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      """
      SELECT DISTINCT m.income.id
      FROM IncomeMovement m
      WHERE m.userId = :userId
        AND m.account.id = :accountId
        AND m.type = br.com.financialcontrol.incomes.IncomeMovementType.RECEIPT
        AND m.status = br.com.financialcontrol.incomes.IncomeMovementStatus.ACTIVE
        AND m.income.id IN :incomeIds
      """)
  List<UUID> findIncomeIdsWithActiveReceiptOnAccount(
      @Param("userId") UUID userId,
      @Param("accountId") UUID accountId,
      @Param("incomeIds") Collection<UUID> incomeIds);
}
