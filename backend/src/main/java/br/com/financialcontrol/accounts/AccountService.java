package br.com.financialcontrol.accounts;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.accounts.dto.CreateAccountRequest;
import br.com.financialcontrol.accounts.dto.UpdateAccountRequest;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.incomes.IncomeRepository;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  static final String ACCOUNT_NOT_FOUND = "Conta não encontrada.";
  static final String ACCOUNT_INACTIVE =
      "Somente contas ativas podem ser utilizadas em novas operações.";

  private final AccountRepository accountRepository;
  private final IncomeRepository incomeRepository;
  private final Clock clock;

  public AccountService(
      AccountRepository accountRepository, IncomeRepository incomeRepository, Clock clock) {
    this.accountRepository = accountRepository;
    this.incomeRepository = incomeRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AccountResponse> list(AuthenticatedUser authenticatedUser) {
    return accountRepository.findAllByUserIdOrderByCreatedAtAsc(authenticatedUser.userId()).stream()
        .map(AccountResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public AccountResponse get(AuthenticatedUser authenticatedUser, UUID accountId) {
    return AccountResponse.from(requireOwnedAccount(authenticatedUser.userId(), accountId));
  }

  @Transactional
  public AccountResponse create(AuthenticatedUser authenticatedUser, CreateAccountRequest request) {
    Instant now = Instant.now(clock);
    Account account = new Account();
    account.setId(UuidV7.create());
    account.setUserId(authenticatedUser.userId());
    account.setName(request.name());
    account.setType(request.type());
    account.setInitialBalance(normalizeMoney(request.initialBalance()));
    account.setActive(true);
    account.setCreatedAt(now);
    account.setUpdatedAt(now);
    return AccountResponse.from(accountRepository.save(account));
  }

  @Transactional
  public AccountResponse update(
      AuthenticatedUser authenticatedUser, UUID accountId, UpdateAccountRequest request) {
    Account account = requireOwnedAccount(authenticatedUser.userId(), accountId);
    account.setName(request.name());
    account.setType(request.type());
    account.setUpdatedAt(Instant.now(clock));
    return AccountResponse.from(accountRepository.save(account));
  }

  @Transactional
  public AccountResponse deactivate(AuthenticatedUser authenticatedUser, UUID accountId) {
    Account account = requireOwnedAccount(authenticatedUser.userId(), accountId);
    account.setActive(false);
    account.setUpdatedAt(Instant.now(clock));
    return AccountResponse.from(accountRepository.save(account));
  }

  @Transactional
  public AccountResponse activate(AuthenticatedUser authenticatedUser, UUID accountId) {
    Account account = requireOwnedAccount(authenticatedUser.userId(), accountId);
    account.setActive(true);
    account.setUpdatedAt(Instant.now(clock));
    return AccountResponse.from(accountRepository.save(account));
  }

  @Transactional(readOnly = true)
  public AccountBalanceResponse getBalance(AuthenticatedUser authenticatedUser, UUID accountId) {
    Account account = requireOwnedAccount(authenticatedUser.userId(), accountId);
    return new AccountBalanceResponse(account.getId(), calculateCurrentBalance(account));
  }

  /**
   * Saldo derivado: saldo inicial + receitas RECEIVED da conta. Demais entradas e saídas entram
   * quando os respectivos domínios forem implementados. Não consulta IncomeService (evita ciclo).
   */
  BigDecimal calculateCurrentBalance(Account account) {
    BigDecimal received =
        incomeRepository.sumReceivedAmountByAccountIdAndUserId(
            account.getId(), account.getUserId());
    if (received == null) {
      received = BigDecimal.ZERO;
    }
    return normalizeMoney(account.getInitialBalance().add(received));
  }

  Account requireOwnedAccount(UUID userId, UUID accountId) {
    return accountRepository
        .findByIdAndUserId(accountId, userId)
        .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND));
  }

  public Account requireActiveOwnedAccount(UUID userId, UUID accountId) {
    Account account = requireOwnedAccount(userId, accountId);
    if (!account.isActive()) {
      throw new BusinessRuleException(ACCOUNT_INACTIVE);
    }
    return account;
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
