package br.com.financialcontrol.accounts.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateInitialBalanceRequest(
    @NotNull(message = "O saldo inicial é obrigatório.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O saldo inicial deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal initialBalance) {}
