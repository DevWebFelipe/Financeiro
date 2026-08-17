package br.com.financialcontrol.financial_goals.dto;

public record CreateGoalContributionResponse(
    GoalContributionResponse contribution, FinancialGoalResponse goal) {}
