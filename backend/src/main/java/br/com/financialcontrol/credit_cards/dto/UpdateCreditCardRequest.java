package br.com.financialcontrol.credit_cards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCreditCardRequest(
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 1, max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
    @NotBlank(message = "O titular é obrigatório.")
        @Size(min = 1, max = 255, message = "O titular deve ter no máximo 255 caracteres.")
        String holderName,
    @Size(max = 4, message = "Os últimos dígitos devem ter no máximo 4 caracteres.")
        String lastFourDigits,
    @NotNull(message = "O limite é obrigatório.")
        @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "O limite deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O limite deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal creditLimit,
    @NotNull(message = "O dia de fechamento é obrigatório.")
        @Min(value = 1, message = "O dia de fechamento deve ser entre 1 e 31.")
        @Max(value = 31, message = "O dia de fechamento deve ser entre 1 e 31.")
        Integer closingDay,
    @NotNull(message = "O dia de vencimento é obrigatório.")
        @Min(value = 1, message = "O dia de vencimento deve ser entre 1 e 31.")
        @Max(value = 31, message = "O dia de vencimento deve ser entre 1 e 31.")
        Integer dueDay) {

  public UpdateCreditCardRequest {
    name = name == null ? null : name.trim();
    holderName = holderName == null ? null : holderName.trim();
    lastFourDigits = blankToNull(lastFourDigits);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
