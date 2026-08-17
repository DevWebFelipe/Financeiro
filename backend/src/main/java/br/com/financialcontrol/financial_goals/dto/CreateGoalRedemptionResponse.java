package br.com.financialcontrol.financial_goals.dto;

public record CreateGoalRedemptionResponse(
    GoalRedemptionResponse redemption, FinancialGoalResponse goal) {}
