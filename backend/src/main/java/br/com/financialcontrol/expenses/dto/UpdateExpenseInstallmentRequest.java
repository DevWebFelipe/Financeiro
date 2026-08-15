package br.com.financialcontrol.expenses.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateExpenseInstallmentRequest(
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal amount,
    @NotNull(message = "A data de vencimento é obrigatória.") LocalDate dueDate) {}
