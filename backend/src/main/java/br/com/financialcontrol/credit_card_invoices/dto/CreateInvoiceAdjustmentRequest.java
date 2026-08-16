package br.com.financialcontrol.credit_card_invoices.dto;

import br.com.financialcontrol.expenses.AdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateInvoiceAdjustmentRequest(
    @NotNull(message = "O tipo do ajuste é obrigatório.") AdjustmentType type,
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal amount,
    @NotBlank(message = "O motivo é obrigatório.") String reason) {

  public CreateInvoiceAdjustmentRequest {
    reason = reason == null ? null : reason.trim();
  }
}
