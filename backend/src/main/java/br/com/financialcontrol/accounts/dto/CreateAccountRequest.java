package br.com.financialcontrol.accounts.dto;

import br.com.financialcontrol.accounts.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateAccountRequest(
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 1, max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
    @NotNull(message = "O tipo é obrigatório.") AccountType type,
    @Digits(
            integer = 17,
            fraction = 2,
            message = "O saldo inicial deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal initialBalance) {

  public CreateAccountRequest {
    name = name == null ? null : name.trim();
  }
}
