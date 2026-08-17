package br.com.financialcontrol.financial_goals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

  Optional<GoalContribution> findByIdAndUserId(UUID id, UUID userId);

  List<GoalContribution> findAllByGoal_IdAndUserIdOrderByContributionDateAscCreatedAtAscIdAsc(
      UUID goalId, UUID userId);

  boolean existsByGoal_Account_IdAndUserId(UUID accountId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(c.amount), 0)
      FROM GoalContribution c
      WHERE c.userId = :userId
        AND c.goal.id = :goalId
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR c.contributionDate <= :asOfDate)
      """)
  BigDecimal sumAmountByGoalIdAndUserIdAsOf(
      @Param("goalId") UUID goalId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  @Query(
      """
      SELECT COALESCE(SUM(c.amount), 0)
      FROM GoalContribution c
      WHERE c.userId = :userId
        AND c.goal.account.id = :accountId
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR c.contributionDate <= :asOfDate)
      """)
  BigDecimal sumAmountByAccountIdAndUserIdAsOf(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);
}
