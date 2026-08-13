package br.com.financialcontrol.users.dto;

import br.com.financialcontrol.users.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 1, max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
    @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email) {

  public UpdateProfileRequest {
    name = name == null ? null : name.trim();
    email = EmailNormalizer.normalize(email);
  }
}
