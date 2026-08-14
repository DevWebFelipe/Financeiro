package br.com.financialcontrol.categories.dto;

import br.com.financialcontrol.categories.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 1, max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
    @NotNull(message = "O tipo é obrigatório.") CategoryType type) {

  public CreateCategoryRequest {
    name = name == null ? null : name.trim();
  }
}
