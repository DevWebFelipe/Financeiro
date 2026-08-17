package br.com.financialcontrol.receivables.dto;

import java.util.List;

public record ReceivablePageResponse(
    List<ReceivableItemResponse> items,
    ReceivableSummaryResponse summary,
    int page,
    int size,
    long totalItems,
    int totalPages) {}
