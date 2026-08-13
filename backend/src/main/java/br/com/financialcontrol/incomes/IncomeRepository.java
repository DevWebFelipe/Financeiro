package br.com.financialcontrol.incomes;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeRepository extends JpaRepository<Income, UUID> {}
