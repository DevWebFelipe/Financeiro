package br.com.financialcontrol.credit_card_invoice_agreements.dto;

import br.com.financialcontrol.credit_card_invoice_agreements.CreditCardInvoiceAgreementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgreementResponse(
    UUID id,
    UUID creditCardId,
    UUID sourceInvoiceId,
    UUID expenseId,
    CreditCardInvoiceAgreementStatus status,
    BigDecimal entryAmount,
    BigDecimal financedAmount,
    int installmentCount,
    BigDecimal installmentAmount,
    BigDecimal contractedTotal,
    BigDecimal additionalCost,
    BigDecimal additionalCostPercent,
    Instant createdAt,
    UUID supersededByAgreementId,
    List<AgreementInstallmentResponse> installments) {}
