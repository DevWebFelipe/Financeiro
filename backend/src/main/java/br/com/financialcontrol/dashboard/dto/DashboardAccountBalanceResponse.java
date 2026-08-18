package br.com.financialcontrol.dashboard.dto;

import br.com.financialcontrol.accounts.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public record DashboardAccountBalanceResponse(
    UUID id,
    String name,
    AccountType type,
    BigDecimal totalBalance,
    BigDecimal reservedAmount,
    BigDecimal availableBalance) {}
