package br.com.financialcontrol.accounts;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.accounts.dto.CreateAccountRequest;
import br.com.financialcontrol.accounts.dto.UpdateAccountRequest;
import br.com.financialcontrol.accounts.dto.UpdateInitialBalanceRequest;
import br.com.financialcontrol.balance_adjustments.AccountBalanceAdjustmentRepository;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentRepository;
import br.com.financialcontrol.credit_cards.CardPurchaseAccountRefundRepository;
import br.com.financialcontrol.financial_goals.GoalContributionRepository;
import br.com.financialcontrol.financial_goals.GoalRedemptionRepository;
import br.com.financialcontrol.incomes.IncomeMovementRepository;
import br.com.financialcontrol.incomes.IncomeMovementType;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.transfers.TransferRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  static final String ACCOUNT_NOT_FOUND = "Conta não encontrada.";
  static final String ACCOUNT_INACTIVE =
      "Somente contas ativas podem ser utilizadas em novas operações.";
  static final String INITIAL_BALANCE_LOCKED =
      "O saldo inicial não pode ser alterado após a primeira movimentação financeira da conta.";
  static final String CANNOT_DEACTIVATE_WITH_BALANCE =
      "Não é permitido desativar conta com saldo diferente de zero.";
  static final String CANNOT_DEACTIVATE_WITH_RESERVED =
      "Não é permitido desativar conta com valor reservado em metas.";

  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  private final AccountRepository accountRepository;
  private final IncomeMovementRepository incomeMovementRepository;
  private final PaymentRepository paymentRepository;
  private final CreditCardInvoicePaymentRepository invoicePaymentRepository;
  private final CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository;
  private final TransferRepository transferRepository;
  private final AccountBalanceAdjustmentRepository balanceAdjustmentRepository;
  private final GoalContributionRepository goalContributionRepository;
  private final GoalRedemptionRepository goalRedemptionRepository;
  private final Clock clock;

  public AccountService(
      AccountRepository accountRepository,
      IncomeMovementRepository incomeMovementRepository,
      PaymentRepository paymentRepository,
      CreditCardInvoicePaymentRepository invoicePaymentRepository,
      CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository,
      TransferRepository transferRepository,
      AccountBalanceAdjustmentRepository balanceAdjustmentRepository,
      GoalContributionRepository goalContributionRepository,
      GoalRedemptionRepository goalRedemptionRepository,
      Clock clock) {
    this.accountRepository = accountRepository;
    this.incomeMovementRepository = incomeMovementRepository;
    this.paymentRepository = paymentRepository;
    this.invoicePaymentRepository = invoicePaymentRepository;
    this.cardPurchaseAccountRefundRepository = cardPurchaseAccountRefundRepository;
    this.transferRepository = transferRepository;
    this.balanceAdjustmentRepository = balanceAdjustmentRepository;
    this.goalContributionRepository = goalContributionRepository;
    this.goalRedemptionRepository = goalRedemptionRepository;
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
    BigDecimal initial =
        request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();
    account.setInitialBalance(normalizeMoney(initial));
    account.setInitialBalanceLocked(false);
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
  public AccountResponse updateInitialBalance(
      AuthenticatedUser authenticatedUser, UUID accountId, UpdateInitialBalanceRequest request) {
    Account account = requireOwnedAccountForUpdate(authenticatedUser.userId(), accountId);
    assertInitialBalanceEditable(account);
    account.setInitialBalance(normalizeMoney(request.initialBalance()));
    account.setUpdatedAt(Instant.now(clock));
    return AccountResponse.from(accountRepository.save(account));
  }

  @Transactional
  public AccountResponse deactivate(AuthenticatedUser authenticatedUser, UUID accountId) {
    Account account = requireOwnedAccountForUpdate(authenticatedUser.userId(), accountId);
    BigDecimal totalBalance = calculateCurrentBalance(account);
    if (totalBalance.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) != 0) {
      throw new BusinessRuleException(CANNOT_DEACTIVATE_WITH_BALANCE);
    }
    if (calculateReservedAmount(account)
            .compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        > 0) {
      throw new BusinessRuleException(CANNOT_DEACTIVATE_WITH_RESERVED);
    }
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
    return AccountBalanceResponse.of(
        account.getId(), calculateCurrentBalance(account), calculateReservedAmount(account));
  }

  /**
   * Saldo financeiro total atual (RN240 / Fase 14): initial + SUM(RECEIPT ACTIVE por conta e
   * movement_date) + refunds - payments ACTIVE - invoice payments ACTIVE + transfers ACTIVE in -
   * transfers ACTIVE out + balance adjustments ACTIVE. Contribuições/resgates de meta não entram
   * nesta fórmula.
   */
  public BigDecimal calculateCurrentBalance(Account account) {
    return calculateBalanceAsOf(account, null);
  }

  /**
   * Saldo as-of-date. {@code asOfDate == null} means current (all eligible facts). When set, only
   * facts with financial date &lt;= asOfDate (refunds: createdAt &lt;= end of that day in
   * America/Sao_Paulo).
   */
  public BigDecimal calculateBalanceAsOf(Account account, LocalDate asOfDate) {
    BigDecimal received =
        zeroIfNull(
            incomeMovementRepository.sumActiveReceiptAmountByAccountIdAndUserIdAsOf(
                account.getId(), account.getUserId(), asOfDate));
    BigDecimal paid =
        zeroIfNull(
            paymentRepository.sumActiveValidExpensePaymentsByAccountIdAndUserIdAsOf(
                account.getId(), account.getUserId(), asOfDate));
    BigDecimal invoicePaid =
        zeroIfNull(
            invoicePaymentRepository.sumActiveAmountByAccountIdAndUserIdAsOf(
                account.getId(), account.getUserId(), asOfDate));
    // Instant null breaks PostgreSQL type inference; split current vs as-of.
    BigDecimal cardRefunds =
        asOfDate == null
            ? zeroIfNull(
                cardPurchaseAccountRefundRepository.sumAmountByAccountIdAndUserId(
                    account.getId(), account.getUserId()))
            : zeroIfNull(
                cardPurchaseAccountRefundRepository.sumAmountByAccountIdAndUserIdAsOf(
                    account.getId(), account.getUserId(), endOfFinancialDay(asOfDate)));
    BigDecimal transferIn =
        zeroIfNull(
            transferRepository.sumActiveIncomingByAccountIdAndUserId(
                account.getId(), account.getUserId(), asOfDate));
    BigDecimal transferOut =
        zeroIfNull(
            transferRepository.sumActiveOutgoingByAccountIdAndUserId(
                account.getId(), account.getUserId(), asOfDate));
    BigDecimal adjustments =
        zeroIfNull(
            balanceAdjustmentRepository.sumActiveAmountByAccountIdAndUserId(
                account.getId(), account.getUserId(), asOfDate));

    return normalizeMoney(
        account
            .getInitialBalance()
            .add(received)
            .subtract(paid)
            .subtract(invoicePaid)
            .add(cardRefunds)
            .add(transferIn)
            .subtract(transferOut)
            .add(adjustments));
  }

  public BigDecimal calculateReservedAmount(Account account) {
    return calculateReservedAmountAsOf(account, null);
  }

  public BigDecimal calculateReservedAmountAsOf(Account account, LocalDate asOfDate) {
    BigDecimal contributions =
        zeroIfNull(
            goalContributionRepository.sumAmountByAccountIdAndUserIdAsOf(
                account.getId(), account.getUserId(), asOfDate));
    BigDecimal redemptions =
        zeroIfNull(
            goalRedemptionRepository.sumAmountByAccountIdAndUserIdAsOf(
                account.getId(), account.getUserId(), asOfDate));
    return normalizeMoney(contributions.subtract(redemptions));
  }

  public BigDecimal calculateAvailableBalance(Account account) {
    return calculateAvailableBalanceAsOf(account, null);
  }

  public BigDecimal calculateAvailableBalanceAsOf(Account account, LocalDate asOfDate) {
    return normalizeMoney(
        calculateBalanceAsOf(account, asOfDate)
            .subtract(calculateReservedAmountAsOf(account, asOfDate)));
  }

  public BigDecimal calculateGoalCurrentAmount(UUID goalId, UUID userId, LocalDate asOfDate) {
    BigDecimal contributions =
        zeroIfNull(
            goalContributionRepository.sumAmountByGoalIdAndUserIdAsOf(goalId, userId, asOfDate));
    BigDecimal redemptions =
        zeroIfNull(
            goalRedemptionRepository.sumAmountByGoalIdAndUserIdAsOf(goalId, userId, asOfDate));
    return normalizeMoney(contributions.subtract(redemptions));
  }

  public void markInitialBalanceLocked(Account account) {
    if (!account.isInitialBalanceLocked()) {
      account.setInitialBalanceLocked(true);
      account.setUpdatedAt(Instant.now(clock));
      accountRepository.save(account);
    }
  }

  public boolean hasFinancialMovements(Account account) {
    if (account.isInitialBalanceLocked()) {
      return true;
    }
    UUID accountId = account.getId();
    UUID userId = account.getUserId();
    boolean moved =
        incomeMovementRepository.existsByAccount_IdAndUserIdAndType(
                accountId, userId, IncomeMovementType.RECEIPT)
            || paymentRepository.existsByAccount_IdAndUserId(accountId, userId)
            || invoicePaymentRepository.existsByAccount_IdAndUserId(accountId, userId)
            || cardPurchaseAccountRefundRepository.existsByAccount_IdAndUserId(accountId, userId)
            || transferRepository.existsBySourceAccount_IdAndUserId(accountId, userId)
            || transferRepository.existsByDestinationAccount_IdAndUserId(accountId, userId)
            || balanceAdjustmentRepository.existsByAccount_IdAndUserId(accountId, userId)
            || goalContributionRepository.existsByGoal_Account_IdAndUserId(accountId, userId)
            || goalRedemptionRepository.existsByGoal_Account_IdAndUserId(accountId, userId);
    if (moved) {
      markInitialBalanceLocked(account);
    }
    return moved;
  }

  public void assertInitialBalanceEditable(Account account) {
    if (hasFinancialMovements(account)) {
      throw new BusinessRuleException(INITIAL_BALANCE_LOCKED);
    }
  }

  public Account requireActiveOwnedAccountForUpdate(UUID userId, UUID accountId) {
    Account account =
        accountRepository
            .findByIdAndUserIdForUpdate(accountId, userId)
            .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND));
    if (!account.isActive()) {
      throw new BusinessRuleException(ACCOUNT_INACTIVE);
    }
    return account;
  }

  public Account requireOwnedAccountForUpdate(UUID userId, UUID accountId) {
    return accountRepository
        .findByIdAndUserIdForUpdate(accountId, userId)
        .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND));
  }

  public Account requireOwnedAccount(UUID userId, UUID accountId) {
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

  public LocalDate today() {
    return LocalDate.now(clock.withZone(FINANCIAL_ZONE));
  }

  private Instant endOfFinancialDay(LocalDate date) {
    return date.atTime(LocalTime.MAX).atZone(FINANCIAL_ZONE).toInstant();
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
