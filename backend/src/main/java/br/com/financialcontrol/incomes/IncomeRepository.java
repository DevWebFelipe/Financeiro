package br.com.financialcontrol.incomes;

import br.com.financialcontrol.expenses.ResponsibleType;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
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
   * Lock pessimista (SELECT FOR UPDATE) para receive/reverse/cancel/PUT. Impede duas transições
   * concorrentes sobre a mesma duplicata sem coluna de versão (RN167).
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

  @Query(
      """
      SELECT COALESCE(SUM(i.amount), 0)
      FROM Income i
      WHERE i.userId = :userId
        AND i.account.id = :accountId
        AND i.status = br.com.financialcontrol.incomes.IncomeStatus.RECEIVED
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR i.receivedDate <= :asOfDate)
      """)
  BigDecimal sumReceivedAmountByAccountIdAndUserIdAsOf(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  boolean existsByAccount_IdAndUserIdAndStatus(UUID accountId, UUID userId, IncomeStatus status);

  String RECEIVABLES_PREDICATE =
      """
      i.userId = :userId
        AND i.status = :status
        AND (:categoryId IS NULL OR i.category.id = :categoryId)
        AND (:accountId IS NULL OR i.account.id = :accountId)
        AND (:responsibleType IS NULL OR i.responsibleType = :responsibleType)
        AND (:responsibleName IS NULL OR i.responsibleName = :responsibleName)
        AND (CAST(:expectedMin AS LocalDate) IS NULL OR i.expectedDate >= :expectedMin)
        AND (CAST(:expectedMax AS LocalDate) IS NULL OR i.expectedDate <= :expectedMax)
        AND (CAST(:receivedMin AS LocalDate) IS NULL OR i.receivedDate >= :receivedMin)
        AND (CAST(:receivedMax AS LocalDate) IS NULL OR i.receivedDate <= :receivedMax)
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
        COALESCE(SUM(CASE WHEN i.status = br.com.financialcontrol.incomes.IncomeStatus.EXPECTED AND i.expectedDate >= :today THEN i.amount ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN i.status = br.com.financialcontrol.incomes.IncomeStatus.EXPECTED AND i.expectedDate < :today THEN i.amount ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN i.status = br.com.financialcontrol.incomes.IncomeStatus.RECEIVED THEN i.amount ELSE 0 END), 0)
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
