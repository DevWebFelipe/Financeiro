package br.com.financialcontrol.financial_goals;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {

  Optional<FinancialGoal> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByAccount_IdAndUserId(UUID accountId, UUID userId);
}
