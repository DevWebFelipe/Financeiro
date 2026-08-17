package br.com.financialcontrol.balance_adjustments;

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

public interface AccountBalanceAdjustmentRepository
    extends JpaRepository<AccountBalanceAdjustment, UUID> {

  Optional<AccountBalanceAdjustment> findByIdAndAccount_IdAndUserId(
      UUID id, UUID accountId, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT a FROM AccountBalanceAdjustment a
      WHERE a.id = :id AND a.account.id = :accountId AND a.userId = :userId
      """)
  Optional<AccountBalanceAdjustment> findByIdAndAccount_IdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("accountId") UUID accountId, @Param("userId") UUID userId);

  List<AccountBalanceAdjustment>
      findAllByAccount_IdAndUserIdOrderByAdjustmentDateAscCreatedAtAscIdAsc(
          UUID accountId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.adjustmentAmount), 0)
      FROM AccountBalanceAdjustment a
      WHERE a.userId = :userId
        AND a.account.id = :accountId
        AND a.status = br.com.financialcontrol.balance_adjustments.BalanceAdjustmentStatus.ACTIVE
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR a.adjustmentDate <= :asOfDate)
      """)
  BigDecimal sumActiveAmountByAccountIdAndUserId(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  boolean existsByAccount_IdAndUserId(UUID accountId, UUID userId);
}
