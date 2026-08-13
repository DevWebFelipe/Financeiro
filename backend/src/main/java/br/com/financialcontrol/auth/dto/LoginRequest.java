package br.com.financialcontrol.auth.dto;

import br.com.financialcontrol.users.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,
    @NotBlank(message = "A senha é obrigatória.") String password) {

  public LoginRequest {
    email = EmailNormalizer.normalize(email);
  }
}
