package br.com.financialcontrol.financial_goals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRedemptionRepository extends JpaRepository<GoalRedemption, UUID> {

  Optional<GoalRedemption> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByGoal_Account_IdAndUserId(UUID accountId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(r.amount), 0)
      FROM GoalRedemption r
      WHERE r.userId = :userId
        AND r.goal.id = :goalId
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR r.redemptionDate <= :asOfDate)
      """)
  BigDecimal sumAmountByGoalIdAndUserIdAsOf(
      @Param("goalId") UUID goalId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  @Query(
      """
      SELECT COALESCE(SUM(r.amount), 0)
      FROM GoalRedemption r
      WHERE r.userId = :userId
        AND r.goal.account.id = :accountId
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR r.redemptionDate <= :asOfDate)
      """)
  BigDecimal sumAmountByAccountIdAndUserIdAsOf(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);
}
