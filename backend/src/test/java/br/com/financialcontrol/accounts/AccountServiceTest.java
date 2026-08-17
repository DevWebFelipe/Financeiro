package br.com.financialcontrol.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.accounts.dto.CreateAccountRequest;
import br.com.financialcontrol.accounts.dto.UpdateAccountRequest;
import br.com.financialcontrol.balance_adjustments.AccountBalanceAdjustmentRepository;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentRepository;
import br.com.financialcontrol.credit_cards.CardPurchaseAccountRefundRepository;
import br.com.financialcontrol.financial_goals.GoalContributionRepository;
import br.com.financialcontrol.financial_goals.GoalRedemptionRepository;
import br.com.financialcontrol.incomes.IncomeMovementRepository;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.transfers.TransferRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
  private static final UUID USER_A = UUID.fromString("01800000-0000-7000-8000-00000000000a");
  private static final UUID USER_B = UUID.fromString("01800000-0000-7000-8000-00000000000b");
  private static final UUID ACCOUNT_ID = UUID.fromString("01800000-0000-7000-8000-0000000000aa");

  @Mock private AccountRepository accountRepository;
  @Mock private IncomeMovementRepository incomeMovementRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private CreditCardInvoicePaymentRepository invoicePaymentRepository;
  @Mock private CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository;
  @Mock private TransferRepository transferRepository;
  @Mock private AccountBalanceAdjustmentRepository balanceAdjustmentRepository;
  @Mock private GoalContributionRepository goalContributionRepository;
  @Mock private GoalRedemptionRepository goalRedemptionRepository;

  private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService =
        new AccountService(
            accountRepository,
            incomeMovementRepository,
            paymentRepository,
            invoicePaymentRepository,
            cardPurchaseAccountRefundRepository,
            transferRepository,
            balanceAdjustmentRepository,
            goalContributionRepository,
            goalRedemptionRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
    stubZeroBalanceParts();
  }

  private void stubZeroBalanceParts() {
    lenient()
        .when(
            incomeMovementRepository.sumActiveReceiptAmountByAccountIdAndUserIdAsOf(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            paymentRepository.sumActiveValidExpensePaymentsByAccountIdAndUserIdAsOf(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            invoicePaymentRepository.sumActiveAmountByAccountIdAndUserIdAsOf(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(cardPurchaseAccountRefundRepository.sumAmountByAccountIdAndUserId(ACCOUNT_ID, USER_A))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            transferRepository.sumActiveIncomingByAccountIdAndUserId(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            transferRepository.sumActiveOutgoingByAccountIdAndUserId(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            balanceAdjustmentRepository.sumActiveAmountByAccountIdAndUserId(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            goalContributionRepository.sumAmountByAccountIdAndUserIdAsOf(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            goalRedemptionRepository.sumAmountByAccountIdAndUserIdAsOf(
                eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(BigDecimal.ZERO);
  }

  @Test
  void shouldCreateBankAccountOwnedByAuthenticatedUser() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response =
        accountService.create(
            new AuthenticatedUser(USER_A),
            new CreateAccountRequest("Nubank", AccountType.BANK_ACCOUNT, new BigDecimal("1500")));

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    verify(accountRepository).save(captor.capture());
    Account saved = captor.getValue();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getId().version()).isEqualTo(7);
    assertThat(saved.getUserId()).isEqualTo(USER_A);
    assertThat(saved.getName()).isEqualTo("Nubank");
    assertThat(saved.getType()).isEqualTo(AccountType.BANK_ACCOUNT);
    assertThat(saved.getInitialBalance()).isEqualByComparingTo("1500.00");
    assertThat(saved.isInitialBalanceLocked()).isFalse();
    assertThat(saved.isActive()).isTrue();
    assertThat(response.initialBalance()).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldDefaultInitialBalanceToZeroWhenOmitted() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response =
        accountService.create(
            new AuthenticatedUser(USER_A),
            new CreateAccountRequest("Nubank", AccountType.BANK_ACCOUNT, null));

    assertThat(response.initialBalance()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldCreateCashAccount() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response =
        accountService.create(
            new AuthenticatedUser(USER_A),
            new CreateAccountRequest("Carteira", AccountType.CASH, BigDecimal.ZERO));

    assertThat(response.type()).isEqualTo(AccountType.CASH);
    assertThat(response.initialBalance()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldIgnoreClientSuppliedOwnerAndUseAuthenticatedUser() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    accountService.create(
        new AuthenticatedUser(USER_A),
        new CreateAccountRequest("Nubank", AccountType.BANK_ACCOUNT, new BigDecimal("10.00")));

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    verify(accountRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(USER_A).isNotEqualTo(USER_B);
  }

  @Test
  void shouldNormalizeInitialBalanceToScaleTwoUsingHalfUp() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response =
        accountService.create(
            new AuthenticatedUser(USER_A),
            new CreateAccountRequest("Nubank", AccountType.BANK_ACCOUNT, new BigDecimal("1500.1")));

    assertThat(response.initialBalance()).isEqualByComparingTo("1500.10");
    assertThat(response.initialBalance().scale()).isEqualTo(2);
  }

  @Test
  void shouldListOnlyAccountsOfAuthenticatedUser() {
    Account own = ownedAccount(true);
    when(accountRepository.findAllByUserIdOrderByCreatedAtAsc(USER_A)).thenReturn(List.of(own));

    List<AccountResponse> result = accountService.list(new AuthenticatedUser(USER_A));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(ACCOUNT_ID);
    verify(accountRepository, never()).findAll();
  }

  @Test
  void shouldUpdateNameAndTypeWithoutChangingOwnerOrInitialBalance() {
    Account account = ownedAccount(true);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response =
        accountService.update(
            new AuthenticatedUser(USER_A),
            ACCOUNT_ID,
            new UpdateAccountRequest("Carteira", AccountType.CASH));

    assertThat(response.name()).isEqualTo("Carteira");
    assertThat(response.type()).isEqualTo(AccountType.CASH);
    assertThat(account.getUserId()).isEqualTo(USER_A);
    assertThat(account.getInitialBalance()).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldDeactivateAccountWhenBalanceIsZero() {
    Account account = ownedAccount(true);
    account.setInitialBalance(BigDecimal.ZERO);
    when(accountRepository.findByIdAndUserIdForUpdate(ACCOUNT_ID, USER_A))
        .thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response = accountService.deactivate(new AuthenticatedUser(USER_A), ACCOUNT_ID);

    assertThat(response.active()).isFalse();
    verify(accountRepository, never()).delete(any());
  }

  @Test
  void shouldRejectDeactivateWhenBalanceIsNotZero() {
    Account account = ownedAccount(true);
    when(accountRepository.findByIdAndUserIdForUpdate(ACCOUNT_ID, USER_A))
        .thenReturn(Optional.of(account));

    assertThatThrownBy(() -> accountService.deactivate(new AuthenticatedUser(USER_A), ACCOUNT_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(AccountService.CANNOT_DEACTIVATE_WITH_BALANCE);
  }

  @Test
  void shouldActivateInactiveAccount() {
    Account account = ownedAccount(false);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response = accountService.activate(new AuthenticatedUser(USER_A), ACCOUNT_ID);

    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldCalculateBalanceFromInitialBalanceWhenThereAreNoReceivedIncomes() {
    Account account = ownedAccount(true);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));

    AccountBalanceResponse response =
        accountService.getBalance(new AuthenticatedUser(USER_A), ACCOUNT_ID);

    assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
    assertThat(response.balance()).isEqualByComparingTo("1500.00");
    assertThat(response.totalBalance()).isEqualByComparingTo("1500.00");
    assertThat(response.reservedAmount()).isEqualByComparingTo("0.00");
    assertThat(response.availableBalance()).isEqualByComparingTo("1500.00");
    assertThat(accountService.calculateCurrentBalance(account)).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldAddReceivedIncomesToDerivedBalance() {
    Account account = ownedAccount(true);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));
    when(incomeMovementRepository.sumActiveReceiptAmountByAccountIdAndUserIdAsOf(
            eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(new BigDecimal("5400.00"));

    AccountBalanceResponse response =
        accountService.getBalance(new AuthenticatedUser(USER_A), ACCOUNT_ID);

    assertThat(response.balance()).isEqualByComparingTo("6900.00");
  }

  @Test
  void shouldSubtractValidExpensePaymentsFromDerivedBalance() {
    Account account = ownedAccount(true);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));
    when(incomeMovementRepository.sumActiveReceiptAmountByAccountIdAndUserIdAsOf(
            eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(new BigDecimal("1000.00"));
    when(paymentRepository.sumActiveValidExpensePaymentsByAccountIdAndUserIdAsOf(
            eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(new BigDecimal("300.00"));

    AccountBalanceResponse response =
        accountService.getBalance(new AuthenticatedUser(USER_A), ACCOUNT_ID);

    assertThat(response.balance()).isEqualByComparingTo("2200.00");
  }

  @Test
  void shouldKeepTotalBalanceUnchangedAndReduceAvailableWhenGoalIsReserved() {
    Account account = ownedAccount(true);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));
    when(goalContributionRepository.sumAmountByAccountIdAndUserIdAsOf(
            eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(new BigDecimal("600.00"));

    AccountBalanceResponse response =
        accountService.getBalance(new AuthenticatedUser(USER_A), ACCOUNT_ID);

    assertThat(response.totalBalance()).isEqualByComparingTo("1500.00");
    assertThat(response.balance()).isEqualByComparingTo("1500.00");
    assertThat(response.reservedAmount()).isEqualByComparingTo("600.00");
    assertThat(response.availableBalance()).isEqualByComparingTo("900.00");
    assertThat(accountService.calculateCurrentBalance(account)).isEqualByComparingTo("1500.00");
    assertThat(accountService.calculateAvailableBalance(account)).isEqualByComparingTo("900.00");
  }

  @Test
  void shouldRejectDeactivateWhenReservedAmountIsPositiveEvenIfTotalIsZero() {
    Account account = ownedAccount(true);
    account.setInitialBalance(BigDecimal.ZERO);
    when(accountRepository.findByIdAndUserIdForUpdate(ACCOUNT_ID, USER_A))
        .thenReturn(Optional.of(account));
    when(goalContributionRepository.sumAmountByAccountIdAndUserIdAsOf(
            eq(ACCOUNT_ID), eq(USER_A), isNull()))
        .thenReturn(new BigDecimal("100.00"));

    assertThatThrownBy(() -> accountService.deactivate(new AuthenticatedUser(USER_A), ACCOUNT_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(AccountService.CANNOT_DEACTIVATE_WITH_RESERVED);
  }

  @Test
  void shouldRejectAccessToAccountOfAnotherUser() {
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_B)).thenReturn(Optional.empty());

    AuthenticatedUser userB = new AuthenticatedUser(USER_B);
    assertThatThrownBy(() -> accountService.get(userB, ACCOUNT_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(AccountService.ACCOUNT_NOT_FOUND);
    assertThatThrownBy(
            () ->
                accountService.update(
                    userB, ACCOUNT_ID, new UpdateAccountRequest("X", AccountType.CASH)))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> accountService.activate(userB, ACCOUNT_ID))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> accountService.getBalance(userB, ACCOUNT_ID))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void shouldRejectInactiveAccountForNewFinancialOperations() {
    Account account = ownedAccount(false);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(AccountService.ACCOUNT_INACTIVE);
  }

  @Test
  void shouldAllowInactiveAccountForHistoryQueries() {
    Account account = ownedAccount(false);
    when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_A)).thenReturn(Optional.of(account));

    AccountResponse response = accountService.get(new AuthenticatedUser(USER_A), ACCOUNT_ID);

    assertThat(response.active()).isFalse();
    assertThat(accountService.getBalance(new AuthenticatedUser(USER_A), ACCOUNT_ID).balance())
        .isEqualByComparingTo("1500.00");
  }

  private static Account ownedAccount(boolean active) {
    Account account = new Account();
    account.setId(ACCOUNT_ID);
    account.setUserId(USER_A);
    account.setName("Nubank");
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal("1500.00"));
    account.setInitialBalanceLocked(false);
    account.setActive(active);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);
    return account;
  }
}
