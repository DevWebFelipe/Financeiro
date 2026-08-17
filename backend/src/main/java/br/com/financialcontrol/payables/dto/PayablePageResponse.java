package br.com.financialcontrol.payables.dto;

import java.math.BigDecimal;
import java.util.List;

public record PayablePageResponse(
    List<PayableItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    BigDecimal totalRemaining,
    BigDecimal totalOriginal,
    BigDecimal totalPaid) {}
