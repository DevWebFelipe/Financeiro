package br.com.financialcontrol.financial_goals.dto;

import java.util.List;

public record FinancialGoalPageResponse(
    List<FinancialGoalResponse> items, int page, int size, long totalItems, int totalPages) {}
