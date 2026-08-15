package br.com.financialcontrol.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountRepository;
import br.com.financialcontrol.accounts.AccountType;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryRepository;
import br.com.financialcontrol.categories.CategoryType;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.expenses.dto.CreateExpenseRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseInstallmentRequest;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
@Transactional
class ExpenseInstallmentPhase8Test {

  private static final Instant NOW = Instant.parse("2026-08-15T18:00:00Z");
  private static final String PASSWORD = "SenhaForte1!";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private ExpenseInstallmentRepository installmentRepository;
  @Autowired private ExpenseService expenseService;
  @Autowired private PasswordEncoder passwordEncoder;

  private User owner;
  private User other;
  private Account account;
  private Category category;
  private String ownerToken;

  @BeforeEach
  void setUp() throws Exception {
    owner = persistUser("phase8a-owner@example.com");
    other = persistUser("phase8a-other@example.com");
    account = persistAccount(owner.getId(), "Conta");
    category = persistCategory(owner.getId(), "Moradia");
    ownerToken = login(owner.getEmail());
  }

  @Test
  void shouldSplitAmountsWithResidualOnFirstInstallment() {
    assertThat(ExpenseService.splitInstallmentAmounts(new BigDecimal("1000.00"), 3))
        .containsExactly(
            new BigDecimal("333.34"), new BigDecimal("333.33"), new BigDecimal("333.33"));
    assertThat(ExpenseService.splitInstallmentAmounts(new BigDecimal("100.00"), 3))
        .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
    assertThat(ExpenseService.splitInstallmentAmounts(new BigDecimal("90.00"), 3))
        .containsExactly(new BigDecimal("30.00"), new BigDecimal("30.00"), new BigDecimal("30.00"));
  }

  @Test
  void shouldPreserveBaseDayAcrossMonthsIncludingLeapYear() {
    LocalDate first = LocalDate.of(2026, 1, 31);
    assertThat(ExpenseService.dueDateForInstallment(first, 1)).isEqualTo("2026-01-31");
    assertThat(ExpenseService.dueDateForInstallment(first, 2)).isEqualTo("2026-02-28");
    assertThat(ExpenseService.dueDateForInstallment(first, 3)).isEqualTo("2026-03-31");
    assertThat(ExpenseService.dueDateForInstallment(first, 4)).isEqualTo("2026-04-30");

    LocalDate leap = LocalDate.of(2024, 1, 31);
    assertThat(ExpenseService.dueDateForInstallment(leap, 2)).isEqualTo("2024-02-29");
    assertThat(ExpenseService.dueDateForInstallment(leap, 3)).isEqualTo("2024-03-31");

    LocalDate day30 = LocalDate.of(2026, 1, 30);
    assertThat(ExpenseService.dueDateForInstallment(day30, 2)).isEqualTo("2026-02-28");
    assertThat(ExpenseService.dueDateForInstallment(day30, 3)).isEqualTo("2026-03-30");
  }

  @Test
  void shouldCreateOneToOneWhenInstallmentCountOmitted() {
    var response =
        expenseService.create(
            new AuthenticatedUser(owner.getId()),
            createRequest(null, "150.00", LocalDate.of(2026, 8, 20)));

    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            response.id(), owner.getId());
    assertThat(installments).hasSize(1);
    assertThat(installments.getFirst().getAmount()).isEqualByComparingTo("150.00");
    assertThat(installments.getFirst().getTotalInstallments()).isEqualTo(1);
    assertThat(response.installmentId()).isEqualTo(installments.getFirst().getId());
  }

  @Test
  void shouldCreateThreeAndTwelveInstallmentsWithSumEqualToTotal() {
    createAndAssertInstallments(3, "1000.00", LocalDate.of(2026, 1, 31));
    createAndAssertInstallments(12, "1200.00", LocalDate.of(2026, 3, 15));
  }

  @Test
  void shouldRejectNonPositiveInstallmentCountViaApi() throws Exception {
    String body =
        """
        {"categoryId":"%s","description":"X","totalAmount":100.00,"expenseDate":"2026-08-01",
         "dueDate":"2026-08-20","paymentMethod":"ACCOUNT","accountId":"%s",
         "responsibleType":"MINE","installmentCount":0}
        """
            .formatted(category.getId(), account.getId());

    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectUnknownInstallmentsArrayProperty() throws Exception {
    String body =
        """
        {"categoryId":"%s","description":"X","totalAmount":100.00,"expenseDate":"2026-08-01",
         "dueDate":"2026-08-20","paymentMethod":"ACCOUNT","accountId":"%s",
         "responsibleType":"MINE","installments":[{"amount":100}]}
        """
            .formatted(category.getId(), account.getId());

    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldUpdateOpenInstallmentWhenSumRemainsValidAndRejectInvalidSumWithRollback() {
    var created =
        expenseService.create(
            new AuthenticatedUser(owner.getId()),
            createRequest(3, "1000.00", LocalDate.of(2026, 1, 31)));
    List<ExpenseInstallment> before =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            created.id(), owner.getId());
    UUID secondId = before.get(1).getId();
    BigDecimal originalSecond = before.get(1).getAmount();

    assertThatThrownBy(
            () ->
                expenseService.updateInstallment(
                    new AuthenticatedUser(owner.getId()),
                    created.id(),
                    secondId,
                    new UpdateExpenseInstallmentRequest(
                        new BigDecimal("400.00"), LocalDate.of(2026, 2, 28))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.INSTALLMENT_SUM_MISMATCH);

    ExpenseInstallment reloaded = installmentRepository.findById(secondId).orElseThrow();
    assertThat(reloaded.getAmount()).isEqualByComparingTo(originalSecond);

    expenseService.updateInstallment(
        new AuthenticatedUser(owner.getId()),
        created.id(),
        before.get(0).getId(),
        new UpdateExpenseInstallmentRequest(new BigDecimal("333.34"), LocalDate.of(2026, 1, 30)));

    Expense expense = expenseRepository.findById(created.id()).orElseThrow();
    assertThat(expense.getDueDate()).isEqualTo(LocalDate.of(2026, 1, 30));
    assertThat(
            installmentRepository
                .findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
                    created.id(), owner.getId())
                .get(1)
                .getDueDate())
        .isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void shouldRejectEditWhenInstallmentIsNotOpen() {
    var created =
        expenseService.create(
            new AuthenticatedUser(owner.getId()),
            createRequest(1, "100.00", LocalDate.of(2026, 8, 20)));
    ExpenseInstallment installment =
        installmentRepository
            .findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(created.id(), owner.getId())
            .getFirst();

    for (ExpenseStatus status :
        List.of(
            ExpenseStatus.PARTIALLY_PAID,
            ExpenseStatus.PAID,
            ExpenseStatus.CANCELLED,
            ExpenseStatus.REFUNDED)) {
      installment.setStatus(status);
      installmentRepository.saveAndFlush(installment);
      assertThatThrownBy(
              () ->
                  expenseService.updateInstallment(
                      new AuthenticatedUser(owner.getId()),
                      created.id(),
                      installment.getId(),
                      new UpdateExpenseInstallmentRequest(
                          new BigDecimal("100.00"), LocalDate.of(2026, 8, 21))))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage(ExpenseService.ONLY_OPEN_INSTALLMENT_CAN_BE_EDITED);
    }
  }

  @Test
  void shouldHideInstallmentFromOtherUser() {
    var created =
        expenseService.create(
            new AuthenticatedUser(owner.getId()),
            createRequest(2, "200.00", LocalDate.of(2026, 8, 20)));
    UUID installmentId =
        installmentRepository
            .findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(created.id(), owner.getId())
            .getFirst()
            .getId();

    assertThatThrownBy(
            () ->
                expenseService.updateInstallment(
                    new AuthenticatedUser(other.getId()),
                    created.id(),
                    installmentId,
                    new UpdateExpenseInstallmentRequest(
                        new BigDecimal("100.00"), LocalDate.of(2026, 8, 21))))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void shouldRejectChangingTotalOnMultiInstallmentExpensePut() throws Exception {
    var created =
        expenseService.create(
            new AuthenticatedUser(owner.getId()),
            createRequest(3, "300.00", LocalDate.of(2026, 5, 10)));

    String body =
        """
        {"categoryId":"%s","description":"X","totalAmount":400.00,"expenseDate":"2026-05-01",
         "dueDate":"2026-05-10","paymentMethod":"ACCOUNT","accountId":"%s",
         "responsibleType":"MINE"}
        """
            .formatted(category.getId(), account.getId());

    mockMvc
        .perform(
            put("/api/v1/expenses/" + created.id())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());

    assertThat(expenseRepository.findById(created.id()).orElseThrow().getTotalAmount())
        .isEqualByComparingTo("300.00");
    assertThat(
            installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
                created.id(), owner.getId()))
        .hasSize(3);
  }

  @Test
  void shouldMarkMultiInstallmentExpenseOverdueWhenAnyOpenInstallmentIsPastDue() {
    var created =
        expenseService.create(
            new AuthenticatedUser(owner.getId()),
            createRequest(3, "300.00", LocalDate.of(2026, 1, 31)));
    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            created.id(), owner.getId());
    installments.get(0).setDueDate(LocalDate.of(2020, 1, 31));
    installmentRepository.saveAndFlush(installments.get(0));

    assertThat(expenseService.get(new AuthenticatedUser(owner.getId()), created.id()).overdue())
        .isTrue();
  }

  @Test
  void shouldExposeInstallmentPutEndpoint() throws Exception {
    var created =
        expenseService.create(
            new AuthenticatedUser(owner.getId()),
            createRequest(2, "200.00", LocalDate.of(2026, 8, 20)));
    UUID firstId =
        installmentRepository
            .findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(created.id(), owner.getId())
            .getFirst()
            .getId();

    mockMvc
        .perform(
            put("/api/v1/expenses/%s/installments/%s".formatted(created.id(), firstId))
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100.00,\"dueDate\":\"2026-08-25\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(100.00))
        .andExpect(jsonPath("$.dueDate").value("2026-08-25"))
        .andExpect(jsonPath("$.installmentNumber").value(1));
  }

  private void createAndAssertInstallments(int count, String total, LocalDate firstDue) {
    var response =
        expenseService.create(
            new AuthenticatedUser(owner.getId()), createRequest(count, total, firstDue));
    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            response.id(), owner.getId());
    assertThat(installments).hasSize(count);
    BigDecimal sum =
        installments.stream()
            .map(ExpenseInstallment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo(total);
    assertThat(expenseRepository.findById(response.id()).orElseThrow().getDueDate())
        .isEqualTo(firstDue);
    assertThat(installments.getFirst().getDueDate()).isEqualTo(firstDue);
    for (int i = 0; i < count; i++) {
      assertThat(installments.get(i).getInstallmentNumber()).isEqualTo(i + 1);
      assertThat(installments.get(i).getTotalInstallments()).isEqualTo(count);
      assertThat(installments.get(i).getDueDate())
          .isEqualTo(ExpenseService.dueDateForInstallment(firstDue, i + 1));
    }
  }

  private CreateExpenseRequest createRequest(Integer count, String total, LocalDate dueDate) {
    return new CreateExpenseRequest(
        category.getId(),
        "Parcelada",
        new BigDecimal(total),
        dueDate.minusDays(5),
        dueDate,
        PaymentMethod.ACCOUNT,
        account.getId(),
        ResponsibleType.MINE,
        null,
        null,
        null,
        count);
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"%s"}
                        """
                            .formatted(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  private User persistUser(String email) {
    User user = new User();
    user.setId(UuidV7.create());
    user.setName("User");
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(PASSWORD));
    user.setActive(true);
    user.setCreatedAt(NOW);
    user.setUpdatedAt(NOW);
    return userRepository.saveAndFlush(user);
  }

  private Account persistAccount(UUID userId, String name) {
    Account account = new Account();
    account.setId(UuidV7.create());
    account.setUserId(userId);
    account.setName(name);
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal("5000.00"));
    account.setActive(true);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);
    return accountRepository.saveAndFlush(account);
  }

  private Category persistCategory(UUID userId, String name) {
    Category category = new Category();
    category.setId(UuidV7.create());
    category.setUserId(userId);
    category.setName(name);
    category.setType(CategoryType.EXPENSE);
    category.setActive(true);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);
    return categoryRepository.saveAndFlush(category);
  }
}
