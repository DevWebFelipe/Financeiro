package br.com.financialcontrol.credit_card_invoice_agreements.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAgreementRequest(
    @NotNull(message = "O valor de entrada é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = true, message = "A entrada não pode ser negativa.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "A entrada deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal entryAmount,
    @NotNull(message = "A conta é obrigatória.") UUID accountId,
    @NotNull(message = "A data do pagamento da entrada é obrigatória.") LocalDate entryPaymentDate,
    @NotNull(message = "A quantidade de parcelas é obrigatória.")
        @Min(value = 1, message = "A quantidade de parcelas deve ser maior que zero.")
        Integer installmentCount,
    @NotNull(message = "O valor da parcela é obrigatório.")
        @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "O valor da parcela deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor da parcela deve ter no máximo 17 dígitos inteiros e 2 decimais.")
        BigDecimal installmentAmount) {}
