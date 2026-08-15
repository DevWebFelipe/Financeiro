package br.com.financialcontrol.expenses.dto;

import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseRequest(
    @NotNull(message = "A categoria é obrigatória.") UUID categoryId,
    @NotBlank(message = "A descrição é obrigatória.") String description,
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal totalAmount,
    @NotNull(message = "A data da despesa é obrigatória.") LocalDate expenseDate,
    @NotNull(message = "A data de vencimento é obrigatória.") LocalDate dueDate,
    @NotNull(message = "A forma de pagamento é obrigatória.") PaymentMethod paymentMethod,
    UUID accountId,
    @NotNull(message = "O responsável é obrigatório.") ResponsibleType responsibleType,
    String responsibleName,
    String barcode,
    String notes,
    @Min(value = 1, message = "A quantidade de parcelas deve ser maior que zero.")
        Integer installmentCount) {

  public CreateExpenseRequest {
    description = description == null ? null : description.trim();
    responsibleName = blankToNull(responsibleName);
    barcode = blankToNull(barcode);
    notes = blankToNull(notes);
  }

  public int resolvedInstallmentCount() {
    return installmentCount == null ? 1 : installmentCount;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
