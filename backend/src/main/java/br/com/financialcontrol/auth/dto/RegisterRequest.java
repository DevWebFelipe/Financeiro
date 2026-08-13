package br.com.financialcontrol.auth.dto;

import br.com.financialcontrol.users.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 1, max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
    @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,
    @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.")
        String password) {

  public RegisterRequest {
    name = name == null ? null : name.trim();
    email = EmailNormalizer.normalize(email);
  }
}
