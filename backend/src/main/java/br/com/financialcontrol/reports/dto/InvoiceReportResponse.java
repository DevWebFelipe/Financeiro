package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.credit_card_invoices.dto.InvoiceAdjustmentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoicePaymentResponse;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import java.util.List;
import java.util.UUID;

public record InvoiceReportResponse(
    UUID invoiceId,
    InvoiceReportCardResponse card,
    InvoiceReportHeaderResponse invoice,
    List<InvoiceReportPurchaseResponse> purchases,
    List<InvoiceReportCategoryGroupResponse> byCategory,
    List<InvoiceReportResponsibleGroupResponse> byResponsible,
    List<AdjustmentResponse> installmentAdjustments,
    List<InvoiceAdjustmentResponse> invoiceAdjustments,
    List<CardReportCreditApplicationResponse> credits,
    List<InvoicePaymentResponse> payments,
    List<InvoiceReportAllocationResponse> allocations) {}
