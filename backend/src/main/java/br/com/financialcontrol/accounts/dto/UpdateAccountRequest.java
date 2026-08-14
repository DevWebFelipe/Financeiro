package br.com.financialcontrol.accounts.dto;

import br.com.financialcontrol.accounts.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 1, max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
    @NotNull(message = "O tipo é obrigatório.") AccountType type) {

  public UpdateAccountRequest {
    name = name == null ? null : name.trim();
  }
}
