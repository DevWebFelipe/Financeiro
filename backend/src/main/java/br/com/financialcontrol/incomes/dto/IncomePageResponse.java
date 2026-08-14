package br.com.financialcontrol.incomes.dto;

import java.util.List;

public record IncomePageResponse(
    List<IncomeResponse> items, int page, int size, long totalItems, int totalPages) {}
