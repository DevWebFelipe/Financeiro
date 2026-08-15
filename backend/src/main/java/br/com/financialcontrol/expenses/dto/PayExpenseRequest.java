package br.com.financialcontrol.expenses.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayExpenseRequest(
    UUID accountId,
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal amount,
    @NotNull(message = "A data de pagamento é obrigatória.") LocalDate paymentDate,
    String notes) {

  public PayExpenseRequest {
    notes = notes == null || notes.isBlank() ? null : notes.trim();
  }
}
