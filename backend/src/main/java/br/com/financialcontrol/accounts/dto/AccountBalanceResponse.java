package br.com.financialcontrol.accounts.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record AccountBalanceResponse(
    UUID accountId,
    BigDecimal totalBalance,
    BigDecimal reservedAmount,
    BigDecimal availableBalance,
    BigDecimal balance) {

  public AccountBalanceResponse(UUID accountId, BigDecimal balance) {
    this(
        accountId,
        scale(balance),
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
        scale(balance),
        scale(balance));
  }

  public static AccountBalanceResponse of(
      UUID accountId, BigDecimal totalBalance, BigDecimal reservedAmount) {
    BigDecimal total = scale(totalBalance);
    BigDecimal reserved = scale(reservedAmount);
    BigDecimal available = scale(total.subtract(reserved));
    return new AccountBalanceResponse(accountId, total, reserved, available, total);
  }

  private static BigDecimal scale(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
