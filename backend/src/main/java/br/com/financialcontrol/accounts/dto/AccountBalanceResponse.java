package br.com.financialcontrol.accounts.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceResponse(UUID accountId, BigDecimal balance) {}
