package br.com.financialcontrol.incomes.dto;

import br.com.financialcontrol.expenses.ResponsibleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateIncomeRequest(
    @NotNull(message = "A categoria é obrigatória.") UUID categoryId,
    @NotBlank(message = "A descrição é obrigatória.") String description,
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal amount,
    @NotNull(message = "A data prevista é obrigatória.") LocalDate expectedDate,
    String notes,
    ResponsibleType responsibleType,
    String responsibleName) {

  public CreateIncomeRequest {
    description = description == null ? null : description.trim();
    notes = notes == null || notes.isBlank() ? null : notes.trim();
    responsibleName =
        responsibleName == null || responsibleName.isBlank() ? null : responsibleName.trim();
  }
}
