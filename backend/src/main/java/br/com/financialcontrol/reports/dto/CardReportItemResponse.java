package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoiceAdjustmentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoicePaymentResponse;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import java.util.List;
import java.util.UUID;

public record CardReportItemResponse(
    UUID creditCardId,
    String name,
    String holderName,
    String lastFourDigits,
    boolean active,
    CardReportSummaryResponse summary,
    List<CardReportPurchaseResponse> purchases,
    List<CreditCardInvoiceResponse> invoices,
    List<InvoicePaymentResponse> payments,
    List<CardReportCreditApplicationResponse> credits,
    List<AdjustmentResponse> installmentAdjustments,
    List<InvoiceAdjustmentResponse> invoiceAdjustments) {}
