package br.com.financialcontrol.incomes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.accounts.AccountType;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryService;
import br.com.financialcontrol.categories.CategoryType;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.incomes.dto.CreateIncomeAccrualRequest;
import br.com.financialcontrol.incomes.dto.CreateIncomeReceiptRequest;
import br.com.financialcontrol.incomes.dto.CreateIncomeRequest;
import br.com.financialcontrol.incomes.dto.IncomePageResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.incomes.dto.UpdateIncomeRequest;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
  private static final LocalDate EXPECTED_DATE = LocalDate.of(2026, 8, 5);
  private static final LocalDate RECEIPT_DATE = LocalDate.of(2026, 8, 6);
  private static final UUID USER_A = UUID.fromString("01800000-0000-7000-8000-00000000000a");
  private static final UUID USER_B = UUID.fromString("01800000-0000-7000-8000-00000000000b");
  private static final UUID INCOME_ID = UUID.fromString("01800000-0000-7000-8000-0000000000aa");
  private static final UUID CATEGORY_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ca");
  private static final UUID ACCOUNT_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ac");

  @Mock private IncomeRepository incomeRepository;
  @Mock private IncomeMovementRepository incomeMovementRepository;
  @Mock private AccountService accountService;
  @Mock private CategoryService categoryService;

  private IncomeService incomeService;

  @BeforeEach
  void setUp() {
    incomeService =
        new IncomeService(
            incomeRepository,
            incomeMovementRepository,
            accountService,
            categoryService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldCreateExpectedIncomeOwnedByAuthenticatedUser() {
    when(categoryService.requireActiveOwnedIncomeCategory(USER_A, CATEGORY_ID))
        .thenReturn(incomeCategory());
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    IncomeResponse response =
        incomeService.create(new AuthenticatedUser(USER_A), createRequest("Salário", "5400.00"));

    ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
    verify(incomeRepository).save(captor.capture());
    Income saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(saved.getAccount()).isNull();
    assertThat(saved.getReceivedDate()).isNull();
    assertThat(response.status()).isEqualTo(IncomeStatus.EXPECTED);
  }

  @Test
  void shouldUpdateExpectedIncome() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeMovementRepository.existsByIncome_IdAndUserId(INCOME_ID, USER_A)).thenReturn(false);
    when(categoryService.requireActiveOwnedIncomeCategory(USER_A, CATEGORY_ID))
        .thenReturn(incomeCategory());
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    IncomeResponse response =
        incomeService.update(
            new AuthenticatedUser(USER_A),
            INCOME_ID,
            new UpdateIncomeRequest(
                CATEGORY_ID,
                "Freelance",
                new BigDecimal("1000.00"),
                EXPECTED_DATE,
                null,
                null,
                null));

    assertThat(response.description()).isEqualTo("Freelance");
    assertThat(income.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
  }

  @Test
  void shouldRejectAmountUpdateAfterMovements() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeMovementRepository.existsByIncome_IdAndUserId(INCOME_ID, USER_A)).thenReturn(true);

    assertThatThrownBy(
            () ->
                incomeService.update(
                    new AuthenticatedUser(USER_A),
                    INCOME_ID,
                    new UpdateIncomeRequest(
                        CATEGORY_ID,
                        "Salário",
                        new BigDecimal("1000.00"),
                        EXPECTED_DATE,
                        null,
                        null,
                        null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.AMOUNT_LOCKED_AFTER_MOVEMENTS);
  }

  @Test
  void shouldCreateReceiptAndMarkReceivedWhenRemainingZero() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
            INCOME_ID, USER_A, IncomeMovementType.ACCRUAL))
        .thenReturn(BigDecimal.ZERO);
    when(incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
            INCOME_ID, USER_A, IncomeMovementType.RECEIPT))
        .thenReturn(BigDecimal.ZERO);
    when(accountService.requireActiveOwnedAccountForUpdate(USER_A, ACCOUNT_ID))
        .thenReturn(activeAccount());
    when(accountService.today()).thenReturn(RECEIPT_DATE);
    when(incomeMovementRepository.save(any(IncomeMovement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    incomeService.createReceipt(
        new AuthenticatedUser(USER_A),
        INCOME_ID,
        new CreateIncomeReceiptRequest(new BigDecimal("5400.00"), RECEIPT_DATE, ACCOUNT_ID));

    assertThat(income.getStatus()).isEqualTo(IncomeStatus.RECEIVED);
    verify(accountService).markInitialBalanceLocked(any(Account.class));
  }

  @Test
  void shouldRejectOverReceipt() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
            INCOME_ID, USER_A, IncomeMovementType.ACCRUAL))
        .thenReturn(BigDecimal.ZERO);
    when(incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
            INCOME_ID, USER_A, IncomeMovementType.RECEIPT))
        .thenReturn(new BigDecimal("100.00"));
    when(accountService.today()).thenReturn(RECEIPT_DATE);

    assertThatThrownBy(
            () ->
                incomeService.createReceipt(
                    new AuthenticatedUser(USER_A),
                    INCOME_ID,
                    new CreateIncomeReceiptRequest(
                        new BigDecimal("5401.00"), RECEIPT_DATE, ACCOUNT_ID)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.OVER_RECEIPT);
  }

  @Test
  void shouldCancelExpectedWithoutActiveReceipt() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeMovementRepository.existsByIncome_IdAndUserIdAndTypeAndStatus(
            INCOME_ID, USER_A, IncomeMovementType.RECEIPT, IncomeMovementStatus.ACTIVE))
        .thenReturn(false);
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    IncomeResponse response = incomeService.cancel(new AuthenticatedUser(USER_A), INCOME_ID);

    assertThat(response.status()).isEqualTo(IncomeStatus.CANCELLED);
  }

  @Test
  void shouldRejectCancelWhenActiveReceiptExists() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeMovementRepository.existsByIncome_IdAndUserIdAndTypeAndStatus(
            INCOME_ID, USER_A, IncomeMovementType.RECEIPT, IncomeMovementStatus.ACTIVE))
        .thenReturn(true);

    assertThatThrownBy(() -> incomeService.cancel(new AuthenticatedUser(USER_A), INCOME_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.CANCEL_REQUIRES_NO_ACTIVE_RECEIPT);
  }

  @Test
  void shouldRejectCancelOnReceivedIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(receivedIncome()));

    assertThatThrownBy(() -> incomeService.cancel(new AuthenticatedUser(USER_A), INCOME_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.ONLY_EXPECTED_CAN_BE_CANCELLED);
  }

  @Test
  void shouldListOwnedIncomesWithDefaultPagination() {
    when(incomeRepository.searchByUser(
            eq(USER_A), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(
            new PageImpl<>(
                List.of(expectedIncome()),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")),
                1));

    IncomePageResponse response =
        incomeService.list(new AuthenticatedUser(USER_A), null, null, null, null, null, 0, 20);

    assertThat(response.items()).hasSize(1);
    assertThat(response.totalItems()).isEqualTo(1);
  }

  @Test
  void shouldCreateAccrualWithoutLockingAccount() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(accountService.today()).thenReturn(RECEIPT_DATE);
    when(incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
            INCOME_ID, USER_A, IncomeMovementType.ACCRUAL))
        .thenReturn(BigDecimal.ZERO);
    when(incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
            INCOME_ID, USER_A, IncomeMovementType.RECEIPT))
        .thenReturn(BigDecimal.ZERO);
    when(incomeMovementRepository.save(any(IncomeMovement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    incomeService.createAccrual(
        new AuthenticatedUser(USER_A),
        INCOME_ID,
        new CreateIncomeAccrualRequest(new BigDecimal("10.00"), RECEIPT_DATE));

    verify(accountService, never()).requireActiveOwnedAccountForUpdate(any(), any());
  }

  @Test
  void shouldRejectGetOfAnotherUser() {
    when(incomeRepository.findByIdAndUserId(INCOME_ID, USER_B)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> incomeService.get(new AuthenticatedUser(USER_B), INCOME_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(IncomeService.INCOME_NOT_FOUND);
  }

  private static CreateIncomeRequest createRequest(String description, String amount) {
    return new CreateIncomeRequest(
        CATEGORY_ID, description, new BigDecimal(amount), EXPECTED_DATE, null, null, null);
  }

  private static Income expectedIncome() {
    Income income = baseIncome();
    income.setStatus(IncomeStatus.EXPECTED);
    return income;
  }

  private static Income receivedIncome() {
    Income income = baseIncome();
    income.setStatus(IncomeStatus.RECEIVED);
    return income;
  }

  private static Income baseIncome() {
    Income income = new Income();
    income.setId(INCOME_ID);
    income.setUserId(USER_A);
    income.setCategory(incomeCategory());
    income.setDescription("Salário");
    income.setAmount(new BigDecimal("5400.00"));
    income.setExpectedDate(EXPECTED_DATE);
    income.setCreatedAt(NOW);
    income.setUpdatedAt(NOW);
    return income;
  }

  private static Category incomeCategory() {
    Category category = new Category();
    category.setId(CATEGORY_ID);
    category.setUserId(USER_A);
    category.setName("Salário");
    category.setType(CategoryType.INCOME);
    category.setActive(true);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);
    return category;
  }

  private static Account activeAccount() {
    Account account = new Account();
    account.setId(ACCOUNT_ID);
    account.setUserId(USER_A);
    account.setName("Nubank");
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal("200.00"));
    account.setActive(true);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);
    return account;
  }
}
