package br.com.financialcontrol.expenses.dto;

import java.util.List;

public record ExpensePageResponse(
    List<ExpenseResponse> items, int page, int size, long totalItems, int totalPages) {}
