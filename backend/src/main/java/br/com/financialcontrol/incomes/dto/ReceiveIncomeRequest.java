package br.com.financialcontrol.incomes.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiveIncomeRequest(
    @NotNull(message = "A conta é obrigatória.") UUID accountId,
    @NotNull(message = "A data de recebimento é obrigatória.") LocalDate receivedDate) {}
