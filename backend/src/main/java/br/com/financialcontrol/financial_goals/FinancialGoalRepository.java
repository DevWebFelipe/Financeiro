package br.com.financialcontrol.financial_goals;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {

  Optional<FinancialGoal> findByIdAndUserId(UUID id, UUID userId);

  Page<FinancialGoal> findAllByUserId(UUID userId, Pageable pageable);

  Page<FinancialGoal> findAllByUserIdAndStatus(
      UUID userId, FinancialGoalStatus status, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM FinancialGoal g WHERE g.id = :id AND g.userId = :userId")
  Optional<FinancialGoal> findByIdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("userId") UUID userId);

  boolean existsByAccount_IdAndUserId(UUID accountId, UUID userId);
}
