package br.com.financialcontrol.expenses;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseInstallmentRepository extends JpaRepository<ExpenseInstallment, UUID> {}
