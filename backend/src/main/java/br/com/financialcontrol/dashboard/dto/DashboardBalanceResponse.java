package br.com.financialcontrol.dashboard.dto;

import java.math.BigDecimal;

public record DashboardBalanceResponse(
    BigDecimal totalBalance, BigDecimal reservedAmount, BigDecimal availableBalance) {}
