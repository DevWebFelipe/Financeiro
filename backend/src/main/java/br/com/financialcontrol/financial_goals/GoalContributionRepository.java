package br.com.financialcontrol.financial_goals;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {}
