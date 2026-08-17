package br.com.financialcontrol.financial_goals.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateFinancialGoalRequest(
    @NotNull(message = "A conta é obrigatória.") UUID accountId,
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 1, max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
    String description,
    @NotNull(message = "O valor alvo é obrigatório.")
        @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "O valor alvo deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor alvo deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal targetAmount,
    LocalDate targetDate) {

  public CreateFinancialGoalRequest {
    name = name == null ? null : name.trim();
    description = description == null || description.isBlank() ? null : description.trim();
  }
}
