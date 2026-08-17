package br.com.financialcontrol.incomes.dto;

import java.util.List;

public record IncomeMovementPageResponse(
    List<IncomeMovementResponse> items, int page, int size, long totalItems, int totalPages) {}
