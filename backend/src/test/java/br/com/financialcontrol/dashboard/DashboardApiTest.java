package br.com.financialcontrol.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.dashboard.dto.DashboardAccountBalanceResponse;
import br.com.financialcontrol.dashboard.dto.DashboardCreditCardResponse;
import br.com.financialcontrol.dashboard.dto.DashboardResponse;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.projections.dto.ProjectionResponse;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class DashboardApiTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);
  private static final String TODAY_TEXT = "2026-08-17";

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-08-17T15:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Test
  void shouldRejectUnauthenticatedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectUnknownParamsConflictingFiltersAndPastPeriod() throws Exception {
    Fixture fx = bootstrap("invalid", "10.00");
    mockMvc
        .perform(
            get("/api/v1/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("includeEvents", "true"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("accountId", fx.accountId().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("year", "2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("months", "13"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-06-01")
                .param("endDate", "2026-06-30"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnTwelveMonthsByDefaultWithoutFutureEvents() throws Exception {
    Fixture fx = bootstrap("empty", "10000.00");
    DashboardResponse dashboard = dashboard(fx.token());
    assertThat(dashboard.asOfDate()).isEqualTo(TODAY);
    assertThat(dashboard.startDate()).isEqualTo(TODAY);
    assertThat(dashboard.endDate()).isEqualTo(LocalDate.of(2027, 7, 31));
    assertThat(dashboard.balance().totalBalance()).isEqualByComparingTo("10000.00");
    assertThat(dashboard.balance().reservedAmount()).isEqualByComparingTo("0.00");
    assertThat(dashboard.balance().availableBalance()).isEqualByComparingTo("10000.00");
    assertThat(dashboard.projection().summary().currentBalance()).isEqualByComparingTo("10000.00");
    assertThat(dashboard.projection().summary().projectedFinalBalance())
        .isEqualByComparingTo("10000.00");
    assertThat(dashboard.projection().months()).hasSize(12);
    assertThat(dashboard.payables().totalRemaining()).isEqualByComparingTo("0.00");
    assertThat(dashboard.payables().openCount()).isZero();
    assertThat(dashboard.receivables().totalReceivableAmount()).isEqualByComparingTo("0.00");
    assertThat(dashboard.accounts()).hasSize(1);
    assertThat(dashboard.creditCards()).isEmpty();
  }

  @Test
  void shouldReuseOfficialBalancesAndProjectionWithoutEmbeddingEvents() throws Exception {
    Fixture fx = bootstrap("income", "1000.00");
    createIncome(fx, "Salario", "400.00", "2026-08-25");
    DashboardResponse dashboard = dashboard(fx.token(), "year", "2026", "month", "8");
    ProjectionResponse projection = projectAugust(fx.token());
    AccountBalanceResponse accountBalance = balance(fx.token(), fx.accountId());

    assertThat(dashboard.balance().totalBalance())
        .isEqualByComparingTo(accountBalance.totalBalance());
    assertThat(dashboard.projection().summary().projectedIncome()).isEqualByComparingTo("400.00");
    assertThat(dashboard.projection().summary().projectedFinalBalance())
        .isEqualByComparingTo(projection.summary().projectedFinalBalance());
    assertThat(dashboard.receivables().futureAmount()).isEqualByComparingTo("400.00");
    assertThat(dashboard.projection().months()).hasSize(1);
    MvcResult raw =
        mockMvc
            .perform(
                get("/api/v1/dashboard")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .param("year", "2026")
                    .param("month", "8"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(raw.getResponse().getContentAsString()).doesNotContain("\"events\"");
    assertThat(raw.getResponse().getContentAsString()).doesNotContain("undatedEvents");
  }

  @Test
  void shouldKeepRemainingAfterPartialReceiptAndRestoreItAfterReverse() throws Exception {
    Fixture fx = bootstrap("receipt", "0.00");
    IncomeResponse income = createIncome(fx, "Parcial", "500.00", "2026-08-25");
    receive(fx, income.id(), "200.00", TODAY_TEXT);
    DashboardResponse afterReceipt = dashboard(fx.token(), "year", "2026", "month", "8");
    assertThat(afterReceipt.balance().totalBalance()).isEqualByComparingTo("200.00");
    assertThat(afterReceipt.receivables().totalReceivableAmount()).isEqualByComparingTo("300.00");
    assertThat(afterReceipt.projection().summary().projectedIncome())
        .isEqualByComparingTo("300.00");

    reverseFirstReceipt(fx.token(), income.id());
    DashboardResponse afterReverse = dashboard(fx.token(), "year", "2026", "month", "8");
    assertThat(afterReverse.balance().totalBalance()).isEqualByComparingTo("0.00");
    assertThat(afterReverse.receivables().totalReceivableAmount()).isEqualByComparingTo("500.00");
    assertThat(afterReverse.projection().summary().projectedIncome())
        .isEqualByComparingTo("500.00");
  }

  @Test
  void shouldExcludeCancelledIncomeAndExpense() throws Exception {
    Fixture fx = bootstrap("cancel", "2000.00");
    IncomeResponse cancelledIncome = createIncome(fx, "Cancelada", "70.00", "2026-08-25");
    mockMvc
        .perform(
            post("/api/v1/incomes/" + cancelledIncome.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    ExpenseResponse cancelled =
        createExpense(fx, "ACCOUNT", fx.accountId(), "Cancelada", "40.00", "2026-08-26");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    DashboardResponse dashboard = dashboard(fx.token(), "year", "2026", "month", "8");
    assertThat(dashboard.receivables().totalReceivableAmount()).isEqualByComparingTo("0.00");
    assertThat(dashboard.payables().totalRemaining()).isEqualByComparingTo("0.00");
    assertThat(dashboard.projection().summary().projectedIncome()).isEqualByComparingTo("0.00");
    assertThat(dashboard.projection().summary().projectedExpense()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldSplitInstallmentsFromInvoicesAndKeepCardPurchaseOutOfAccountPayables()
      throws Exception {
    Fixture fx = bootstrap("split", "5000.00");
    ExpenseResponse split =
        createExpense(fx, "ACCOUNT", fx.accountId(), "Parcelado", "300.00", "2026-08-20", 3);
    List<ExpenseInstallmentResponse> installments = listInstallments(fx.token(), split.id());
    payInstallment(fx, split.id(), installments.getFirst().id(), "40.00");
    ExpenseResponse overdue = createExpense(fx, "NONE", null, "Boleto", "30.00", "2026-08-05");

    CardFx card = bootstrapCard("card-dash", "2000.00", "1000.00");
    createCardExpense(card, "180.00", "2026-08-11", 1);

    DashboardResponse accountView =
        dashboard(fx.token(), "year", "2026", "month", "8", "months", "3");
    assertThat(accountView.payables().installmentRemaining()).isEqualByComparingTo("290.00");
    assertThat(accountView.payables().invoiceRemaining()).isEqualByComparingTo("0.00");
    assertThat(accountView.payables().overdueInstallmentRemaining()).isEqualByComparingTo("30.00");
    assertThat(accountView.payables().overdueCount()).isEqualTo(1);
    assertThat(accountView.projection().months()).hasSize(3);
    assertThat(accountView.creditCards()).isEmpty();

    DashboardResponse cardView =
        dashboard(card.token(), "year", "2026", "month", "8", "months", "12");
    assertThat(cardView.payables().installmentRemaining()).isEqualByComparingTo("0.00");
    assertThat(cardView.payables().invoiceRemaining()).isEqualByComparingTo("180.00");
    assertThat(cardView.projection().summary().projectedExpense()).isEqualByComparingTo("180.00");
    assertThat(cardView.creditCards()).hasSize(1);
    DashboardCreditCardResponse cardSummary = cardView.creditCards().getFirst();
    assertThat(cardSummary.id()).isEqualTo(card.cardId());
    assertThat(cardSummary.usedLimit()).isEqualByComparingTo("180.00");
    assertThat(cardSummary.invoiceRemaining()).isEqualByComparingTo("180.00");
    assertThat(overdue.id()).isNotNull();
  }

  @Test
  void shouldKeepTransfersOutOfIncomeAndExpenseAndPreserveConsolidatedBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("transfer"));
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "1000.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "200.00");
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceAccountId":"%s","destinationAccountId":"%s","amount":300.00,"transferDate":"%s","description":"Entre contas"}
                    """
                        .formatted(source.id(), destination.id(), TODAY_TEXT)))
        .andExpect(status().isCreated());
    DashboardResponse dashboard = dashboard(token, "year", "2026", "month", "8");
    assertThat(dashboard.balance().totalBalance()).isEqualByComparingTo("1200.00");
    assertThat(dashboard.projection().summary().projectedIncome()).isEqualByComparingTo("0.00");
    assertThat(dashboard.projection().summary().projectedExpense()).isEqualByComparingTo("0.00");
    assertThat(dashboard.accounts()).hasSize(2);
  }

  @Test
  void shouldExposeReservedAmountWithoutReducingTotalBalance() throws Exception {
    Fixture fx = bootstrap("goals", "1000.00");
    FinancialGoalResponse goal = createGoal(fx.token(), fx.accountId(), "Viagem", "500.00");
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":200.00,"contributionDate":"%s","notes":"Aporte"}
                    """
                        .formatted(TODAY_TEXT)))
        .andExpect(status().isCreated());
    DashboardResponse dashboard = dashboard(fx.token(), "year", "2026", "month", "8");
    AccountBalanceResponse accountBalance = balance(fx.token(), fx.accountId());
    assertThat(dashboard.balance().totalBalance()).isEqualByComparingTo("1000.00");
    assertThat(dashboard.balance().reservedAmount()).isEqualByComparingTo("200.00");
    assertThat(dashboard.balance().availableBalance()).isEqualByComparingTo("800.00");
    DashboardAccountBalanceResponse account = dashboard.accounts().getFirst();
    assertThat(account.totalBalance()).isEqualByComparingTo(accountBalance.totalBalance());
    assertThat(account.reservedAmount()).isEqualByComparingTo(accountBalance.reservedAmount());
    assertThat(account.availableBalance()).isEqualByComparingTo(accountBalance.availableBalance());
    assertThat(dashboard.projection().summary().reservedAmount()).isEqualByComparingTo("200.00");
  }

  @Test
  void shouldIsolateUsers() throws Exception {
    Fixture owner = bootstrap("iso-a", "100.00");
    Fixture other = bootstrap("iso-b", "999.00");
    createIncome(owner, "Minha", "50.00", "2026-08-20");
    createIncome(other, "Alheia", "80.00", "2026-08-20");
    DashboardResponse mine = dashboard(owner.token(), "year", "2026", "month", "8");
    assertThat(mine.balance().totalBalance()).isEqualByComparingTo("100.00");
    assertThat(mine.receivables().futureAmount()).isEqualByComparingTo("50.00");
    assertThat(mine.accounts())
        .extracting(DashboardAccountBalanceResponse::id)
        .containsExactly(owner.accountId())
        .doesNotContain(other.accountId());
  }

  private DashboardResponse dashboard(String token, String... params) throws Exception {
    MockHttpServletRequestBuilder request =
        get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, DashboardResponse.class);
  }

  private ProjectionResponse projectAugust(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/projections")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .param("year", "2026")
                    .param("month", "8")
                    .param("size", "100"))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, ProjectionResponse.class);
  }

  private Fixture bootstrap(String prefix, String initial) throws Exception {
    String token = registerAndLogin(uniqueEmail(prefix));
    UUID incomeCategory = createIncomeCategory(token, "Salario").id();
    UUID expenseCategory = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "Nubank", "BANK_ACCOUNT", initial).id();
    return new Fixture(token, incomeCategory, expenseCategory, accountId);
  }

  private CardFx bootstrapCard(String prefix, String limit, String initial) throws Exception {
    String token = registerAndLogin(uniqueEmail(prefix));
    UUID categoryId = createExpenseCategory(token, "Cartao").id();
    UUID accountId = createAccount(token, "Banco", "BANK_ACCOUNT", initial).id();
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/credit-cards")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","holderName":"Alice","creditLimit":%s,"closingDay":10,"dueDay":20}
                        """
                            .formatted(limit)))
            .andExpect(status().isCreated())
            .andReturn();
    CreditCardResponse card = read(created, CreditCardResponse.class);
    return new CardFx(token, categoryId, accountId, card.id());
  }

  private IncomeResponse createIncome(Fixture fx, String description, String amount, String date)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","amount":%s,"expectedDate":"%s"}
                        """
                            .formatted(fx.incomeCategoryId(), description, amount, date)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private void receive(Fixture fx, UUID incomeId, String amount, String date) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"date":"%s"}
                    """
                        .formatted(fx.accountId(), amount, date)))
        .andExpect(status().isCreated());
  }

  private void reverseFirstReceipt(String token, UUID incomeId) throws Exception {
    MvcResult movements =
        mockMvc
            .perform(
                get("/api/v1/incomes/" + incomeId + "/movements")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    UUID movementId =
        UUID.fromString(
            JsonPath.read(movements.getResponse().getContentAsString(), "$.items[0].id"));
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/movements/" + movementId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
  }

  private ExpenseResponse createExpense(
      Fixture fx,
      String paymentMethod,
      UUID accountId,
      String description,
      String amount,
      String dueDate)
      throws Exception {
    return createExpense(fx, paymentMethod, accountId, description, amount, dueDate, 1);
  }

  private ExpenseResponse createExpense(
      Fixture fx,
      String paymentMethod,
      UUID accountId,
      String description,
      String amount,
      String dueDate,
      int installments)
      throws Exception {
    String accountField = accountId == null ? "" : ",\"accountId\":\"" + accountId + "\"";
    String countField = installments == 1 ? "" : ",\"installmentCount\":" + installments;
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","totalAmount":%s,"expenseDate":"2026-08-01","dueDate":"%s","paymentMethod":"%s","responsibleType":"MINE"%s%s}
                        """
                            .formatted(
                                fx.expenseCategoryId(),
                                description,
                                amount,
                                dueDate,
                                paymentMethod,
                                accountField,
                                countField)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private ExpenseResponse createCardExpense(
      CardFx fx, String amount, String expenseDate, int installments) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"Compra","totalAmount":%s,"expenseDate":"%s","dueDate":"2099-01-01","paymentMethod":"CREDIT_CARD","creditCardId":"%s","responsibleType":"MINE","installmentCount":%s}
                        """
                            .formatted(
                                fx.categoryId(), amount, expenseDate, fx.cardId(), installments)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private void payInstallment(Fixture fx, UUID expenseId, UUID installmentId, String amount)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseId + "/installments/" + installmentId + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"paymentDate":"2026-08-12"}
                    """
                        .formatted(fx.accountId(), amount)))
        .andExpect(status().isOk());
  }

  private List<ExpenseInstallmentResponse> listInstallments(String token, UUID expenseId)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/expenses/" + expenseId + "/installments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return List.of(
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), ExpenseInstallmentResponse[].class));
  }

  private FinancialGoalResponse createGoal(String token, UUID accountId, String name, String target)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/financial-goals")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"accountId":"%s","name":"%s","description":"Obs","targetAmount":%s,"targetDate":null}
                        """
                            .formatted(accountId, name, target)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, FinancialGoalResponse.class);
  }

  private CategoryResponse createIncomeCategory(String token, String name) throws Exception {
    return createCategory(token, name, "INCOME");
  }

  private CategoryResponse createExpenseCategory(String token, String name) throws Exception {
    return createCategory(token, name, "EXPENSE");
  }

  private CategoryResponse createCategory(String token, String name, String type) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"%s"}
                        """
                            .formatted(name, type)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, CategoryResponse.class);
  }

  private AccountResponse createAccount(String token, String name, String type, String initial)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"%s","initialBalance":%s}
                        """
                            .formatted(name, type, initial)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AccountResponse.class);
  }

  private AccountBalanceResponse balance(String token, UUID accountId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + accountId + "/balance")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, AccountBalanceResponse.class);
  }

  private String registerAndLogin(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Alice","email":"%s","password":"senha-segura"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"senha-segura"}
                        """
                            .formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  private <T> T read(MvcResult result, Class<T> type) throws Exception {
    return jsonMapper.readValue(result.getResponse().getContentAsString(), type);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private record Fixture(
      String token, UUID incomeCategoryId, UUID expenseCategoryId, UUID accountId) {}

  private record CardFx(String token, UUID categoryId, UUID accountId, UUID cardId) {}
}
