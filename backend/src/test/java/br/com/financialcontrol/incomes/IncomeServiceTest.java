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
import br.com.financialcontrol.incomes.dto.CreateIncomeRequest;
import br.com.financialcontrol.incomes.dto.IncomePageResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.incomes.dto.ReceiveIncomeRequest;
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
  private static final LocalDate RECEIVED_DATE = LocalDate.of(2026, 8, 6);
  private static final UUID USER_A = UUID.fromString("01800000-0000-7000-8000-00000000000a");
  private static final UUID USER_B = UUID.fromString("01800000-0000-7000-8000-00000000000b");
  private static final UUID INCOME_ID = UUID.fromString("01800000-0000-7000-8000-0000000000aa");
  private static final UUID CATEGORY_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ca");
  private static final UUID ACCOUNT_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ac");

  @Mock private IncomeRepository incomeRepository;
  @Mock private AccountService accountService;
  @Mock private CategoryService categoryService;

  private IncomeService incomeService;

  @BeforeEach
  void setUp() {
    incomeService =
        new IncomeService(
            incomeRepository, accountService, categoryService, Clock.fixed(NOW, ZoneOffset.UTC));
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

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getId().version()).isEqualTo(7);
    assertThat(saved.getUserId()).isEqualTo(USER_A).isNotEqualTo(USER_B);
    assertThat(saved.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(saved.getAccount()).isNull();
    assertThat(saved.getReceivedDate()).isNull();
    assertThat(saved.getResponsibleType()).isNull();
    assertThat(saved.getResponsibleName()).isNull();
    assertThat(saved.getAmount()).isEqualByComparingTo("5400.00");
    assertThat(response.status()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(response.accountId()).isNull();
    assertThat(response.receivedDate()).isNull();
  }

  @Test
  void shouldGetOwnedIncome() {
    when(incomeRepository.findByIdAndUserId(INCOME_ID, USER_A))
        .thenReturn(Optional.of(expectedIncome()));

    IncomeResponse response = incomeService.get(new AuthenticatedUser(USER_A), INCOME_ID);

    assertThat(response.id()).isEqualTo(INCOME_ID);
    assertThat(response.status()).isEqualTo(IncomeStatus.EXPECTED);
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
    assertThat(response.page()).isEqualTo(0);
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(incomeRepository)
        .searchByUser(
            eq(USER_A), isNull(), isNull(), isNull(), isNull(), isNull(), pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
        .isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void shouldUpdateExpectedIncome() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(categoryService.requireActiveOwnedIncomeCategory(USER_A, CATEGORY_ID))
        .thenReturn(incomeCategory());
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    IncomeResponse response =
        incomeService.update(
            new AuthenticatedUser(USER_A),
            INCOME_ID,
            new UpdateIncomeRequest(
                CATEGORY_ID, "Freelance", new BigDecimal("1000.00"), EXPECTED_DATE, "nota"));

    assertThat(response.description()).isEqualTo("Freelance");
    assertThat(response.amount()).isEqualByComparingTo("1000.00");
    assertThat(income.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
  }

  @Test
  void shouldRejectUpdateOfReceivedIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(receivedIncome()));

    assertThatThrownBy(
            () -> incomeService.update(new AuthenticatedUser(USER_A), INCOME_ID, updateRequest()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.RECEIVED_CANNOT_BE_EDITED);
    verify(incomeRepository, never()).save(any());
  }

  @Test
  void shouldRejectUpdateOfCancelledIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(cancelledIncome()));

    assertThatThrownBy(
            () -> incomeService.update(new AuthenticatedUser(USER_A), INCOME_ID, updateRequest()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.CANCELLED_CANNOT_BE_EDITED);
  }

  @Test
  void shouldReceiveExpectedIncome() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID)).thenReturn(activeAccount());
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    IncomeResponse response =
        incomeService.receive(
            new AuthenticatedUser(USER_A),
            INCOME_ID,
            new ReceiveIncomeRequest(ACCOUNT_ID, RECEIVED_DATE));

    assertThat(response.status()).isEqualTo(IncomeStatus.RECEIVED);
    assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
    assertThat(response.receivedDate()).isEqualTo(RECEIVED_DATE);
    assertThat(income.getStatus()).isEqualTo(IncomeStatus.RECEIVED);
  }

  @Test
  void shouldReverseReceivedIncomeToExpectedWithoutCancelling() {
    Income income = receivedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    IncomeResponse response = incomeService.reverse(new AuthenticatedUser(USER_A), INCOME_ID);

    assertThat(response.status()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(response.status()).isNotEqualTo(IncomeStatus.CANCELLED);
    assertThat(response.accountId()).isNull();
    assertThat(response.receivedDate()).isNull();
    assertThat(income.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(income.getAccount()).isNull();
    assertThat(income.getReceivedDate()).isNull();
    verify(accountService, never()).requireActiveOwnedAccount(any(), any());
  }

  @Test
  void shouldAllowReceiveAgainAfterReverse() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID)).thenReturn(activeAccount());
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    incomeService.receive(
        new AuthenticatedUser(USER_A),
        INCOME_ID,
        new ReceiveIncomeRequest(ACCOUNT_ID, RECEIVED_DATE));
    incomeService.reverse(new AuthenticatedUser(USER_A), INCOME_ID);
    IncomeResponse receivedAgain =
        incomeService.receive(
            new AuthenticatedUser(USER_A),
            INCOME_ID,
            new ReceiveIncomeRequest(ACCOUNT_ID, RECEIVED_DATE));

    assertThat(receivedAgain.status()).isEqualTo(IncomeStatus.RECEIVED);
    assertThat(receivedAgain.accountId()).isEqualTo(ACCOUNT_ID);
    assertThat(income.getStatus()).isNotEqualTo(IncomeStatus.CANCELLED);
  }

  @Test
  void reverseMustNotBeImplementedAsCancel() {
    Income income = receivedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    incomeService.reverse(new AuthenticatedUser(USER_A), INCOME_ID);

    assertThat(income.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(income.getStatus()).isNotEqualTo(IncomeStatus.CANCELLED);
  }

  @Test
  void shouldCancelExpectedIncomeWithoutChangingReceiptFields() {
    Income income = expectedIncome();
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(income));
    when(incomeRepository.save(any(Income.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    IncomeResponse response = incomeService.cancel(new AuthenticatedUser(USER_A), INCOME_ID);

    assertThat(response.status()).isEqualTo(IncomeStatus.CANCELLED);
    assertThat(income.getAccount()).isNull();
    assertThat(income.getReceivedDate()).isNull();
    verify(accountService, never()).requireActiveOwnedAccount(any(), any());
  }

  @Test
  void shouldRejectReceiveOnReceivedIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(receivedIncome()));

    assertThatThrownBy(
            () ->
                incomeService.receive(
                    new AuthenticatedUser(USER_A),
                    INCOME_ID,
                    new ReceiveIncomeRequest(ACCOUNT_ID, RECEIVED_DATE)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.ONLY_EXPECTED_CAN_BE_RECEIVED);
  }

  @Test
  void shouldRejectReceiveOnCancelledIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(cancelledIncome()));

    assertThatThrownBy(
            () ->
                incomeService.receive(
                    new AuthenticatedUser(USER_A),
                    INCOME_ID,
                    new ReceiveIncomeRequest(ACCOUNT_ID, RECEIVED_DATE)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.ONLY_EXPECTED_CAN_BE_RECEIVED);
  }

  @Test
  void shouldRejectReverseOnExpectedIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(expectedIncome()));

    assertThatThrownBy(() -> incomeService.reverse(new AuthenticatedUser(USER_A), INCOME_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.ONLY_RECEIVED_CAN_BE_REVERSED);
  }

  @Test
  void shouldRejectReverseOnCancelledIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(cancelledIncome()));

    assertThatThrownBy(() -> incomeService.reverse(new AuthenticatedUser(USER_A), INCOME_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.ONLY_RECEIVED_CAN_BE_REVERSED);
  }

  @Test
  void shouldRejectCancelOnReceivedIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(receivedIncome()));

    assertThatThrownBy(() -> incomeService.cancel(new AuthenticatedUser(USER_A), INCOME_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.ONLY_EXPECTED_CAN_BE_CANCELLED);
    assertThat(receivedIncome().getStatus()).isNotEqualTo(IncomeStatus.CANCELLED);
  }

  @Test
  void shouldRejectCancelOnCancelledIncome() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_A))
        .thenReturn(Optional.of(cancelledIncome()));

    assertThatThrownBy(() -> incomeService.cancel(new AuthenticatedUser(USER_A), INCOME_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(IncomeService.ONLY_EXPECTED_CAN_BE_CANCELLED);
  }

  @Test
  void shouldRejectGetOfAnotherUser() {
    when(incomeRepository.findByIdAndUserId(INCOME_ID, USER_B)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> incomeService.get(new AuthenticatedUser(USER_B), INCOME_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(IncomeService.INCOME_NOT_FOUND);
  }

  @Test
  void shouldRejectMutationsOfAnotherUser() {
    when(incomeRepository.findByIdAndUserIdForUpdate(INCOME_ID, USER_B))
        .thenReturn(Optional.empty());
    AuthenticatedUser userB = new AuthenticatedUser(USER_B);

    assertThatThrownBy(() -> incomeService.update(userB, INCOME_ID, updateRequest()))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(
            () ->
                incomeService.receive(
                    userB, INCOME_ID, new ReceiveIncomeRequest(ACCOUNT_ID, RECEIVED_DATE)))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> incomeService.reverse(userB, INCOME_ID))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> incomeService.cancel(userB, INCOME_ID))
        .isInstanceOf(NotFoundException.class);
  }

  private static CreateIncomeRequest createRequest(String description, String amount) {
    return new CreateIncomeRequest(
        CATEGORY_ID, description, new BigDecimal(amount), EXPECTED_DATE, null);
  }

  private static UpdateIncomeRequest updateRequest() {
    return new UpdateIncomeRequest(
        CATEGORY_ID, "Salário", new BigDecimal("5400.00"), EXPECTED_DATE, null);
  }

  private static Income expectedIncome() {
    Income income = baseIncome();
    income.setStatus(IncomeStatus.EXPECTED);
    return income;
  }

  private static Income receivedIncome() {
    Income income = baseIncome();
    income.setStatus(IncomeStatus.RECEIVED);
    income.setAccount(activeAccount());
    income.setReceivedDate(RECEIVED_DATE);
    return income;
  }

  private static Income cancelledIncome() {
    Income income = baseIncome();
    income.setStatus(IncomeStatus.CANCELLED);
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
