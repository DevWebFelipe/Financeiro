package br.com.financialcontrol.transfers.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransferRequest(
    @NotNull(message = "A conta de origem é obrigatória.") UUID sourceAccountId,
    @NotNull(message = "A conta de destino é obrigatória.") UUID destinationAccountId,
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal amount,
    @NotNull(message = "A data da transferência é obrigatória.") LocalDate transferDate,
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description) {

  public CreateTransferRequest {
    description = description == null ? null : description.trim();
    if (description != null && description.isEmpty()) {
      description = null;
    }
  }
}
