package br.com.financialcontrol.balance_adjustments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBalanceAdjustmentRequest(
    @NotNull(message = "O saldo real informado é obrigatório.")
        @DecimalMin(
            value = "0.00",
            inclusive = true,
            message = "O saldo real informado deve ser maior ou igual a zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O saldo real deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal reportedBalance,
    LocalDate adjustmentDate) {}
