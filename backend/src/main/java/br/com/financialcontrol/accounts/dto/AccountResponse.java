package br.com.financialcontrol.accounts.dto;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String name,
    AccountType type,
    BigDecimal initialBalance,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {

  public static AccountResponse from(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getName(),
        account.getType(),
        account.getInitialBalance(),
        account.isActive(),
        account.getCreatedAt(),
        account.getUpdatedAt());
  }
}
