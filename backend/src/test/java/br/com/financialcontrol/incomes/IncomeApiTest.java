package br.com.financialcontrol.incomes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class IncomeApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private IncomeRepository incomeRepository;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldCreateExpectedIncomeAndReturn201() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("create"), "senha-segura");
    CategoryResponse category = createIncomeCategory(token, "Salário");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createIncomeJson(category.id(), "Salário", "5400.00", "2026-08-05")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("EXPECTED"))
            .andExpect(jsonPath("$.accountId").value((Object) null))
            .andExpect(jsonPath("$.receivedDate").value((Object) null))
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.responsibleType").doesNotExist())
            .andExpect(jsonPath("$.responsibleName").doesNotExist())
            .andReturn();

    IncomeResponse body = read(result, IncomeResponse.class);
    assertThat(body.amount()).isEqualByComparingTo("5400.00");
    Income saved = incomeRepository.findById(body.id()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(saved.getAccount()).isNull();
    assertThat(saved.getReceivedDate()).isNull();
    assertThat(saved.getResponsibleType()).isNull();
    assertThat(saved.getUserId().version()).isEqualTo(7);
  }

  @Test
  void shouldGetOwnedIncome() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("get"), "senha-segura");
    IncomeResponse created = createIncome(token, "Salário", "5400.00", "2026-08-05");

    mockMvc
        .perform(
            get("/api/v1/incomes/" + created.id()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(created.id().toString()))
        .andExpect(jsonPath("$.status").value("EXPECTED"));
  }

  @Test
  void shouldListIncomesPaginatedOrderedByCreatedAtAsc() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("list"), "senha-segura");
    IncomeResponse first = createIncome(token, "Primeira", "100.00", "2026-08-10");
    IncomeResponse second = createIncome(token, "Segunda", "200.00", "2026-08-01");

    mockMvc
        .perform(get("/api/v1/incomes").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(2))
        .andExpect(jsonPath("$.items[0].id").value(first.id().toString()))
        .andExpect(jsonPath("$.items[1].id").value(second.id().toString()));
  }

  @Test
  void shouldFilterListByExpectedDateNotReceivedDate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("filter"), "senha-segura");
    IncomeResponse inRange = createIncome(token, "No período", "100.00", "2026-08-10");
    createIncome(token, "Fora", "200.00", "2026-07-01");
    AccountResponse account = createAccount(token);
    receive(token, inRange.id(), account.id(), "2026-09-01");

    mockMvc
        .perform(
            get("/api/v1/incomes")
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.items[0].id").value(inRange.id().toString()));
  }

  @Test
  void shouldUpdateExpectedIncome() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("upd"), "senha-segura");
    CategoryResponse category = createIncomeCategory(token, "Salário");
    IncomeResponse created = createIncome(token, category.id(), "Salário", "5400.00", "2026-08-05");

    mockMvc
        .perform(
            put("/api/v1/incomes/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(category.id(), "Freelance", "1000.00", "2026-08-08")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Freelance"))
        .andExpect(jsonPath("$.status").value("EXPECTED"));
  }

  @Test
  void shouldRejectUpdateOfReceivedAndCancelledIncome() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("upd-status"), "senha-segura");
    CategoryResponse category = createIncomeCategory(token, "Salário");
    AccountResponse account = createAccount(token);
    IncomeResponse received =
        createIncome(token, category.id(), "Recebida", "100.00", "2026-08-05");
    receive(token, received.id(), account.id(), "2026-08-06");
    IncomeResponse cancelled =
        createIncome(token, category.id(), "Cancelada", "50.00", "2026-08-05");
    cancel(token, cancelled.id());

    mockMvc
        .perform(
            put("/api/v1/incomes/" + received.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(category.id(), "X", "10.00", "2026-08-05")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            put("/api/v1/incomes/" + cancelled.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(category.id(), "X", "10.00", "2026-08-05")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldReceiveIncomeIncreaseBalanceAndReverseWithoutCancelling() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("cycle"), "senha-segura");
    AccountResponse account = createAccount(token, "200.00");
    IncomeResponse created = createIncome(token, "Salário", "1000.00", "2026-08-05");

    assertThat(balance(token, account.id())).isEqualByComparingTo("200.00");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(account.id(), "2026-08-06")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.accountId").value(account.id().toString()))
        .andExpect(jsonPath("$.receivedDate").value("2026-08-06"));

    assertThat(balance(token, account.id())).isEqualByComparingTo("1200.00");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EXPECTED"))
        .andExpect(jsonPath("$.accountId").value((Object) null))
        .andExpect(jsonPath("$.receivedDate").value((Object) null));

    Income afterReverse = incomeRepository.findById(created.id()).orElseThrow();
    assertThat(afterReverse.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(afterReverse.getStatus()).isNotEqualTo(IncomeStatus.CANCELLED);
    assertThat(afterReverse.getAccount()).isNull();
    assertThat(afterReverse.getReceivedDate()).isNull();
    assertThat(balance(token, account.id())).isEqualByComparingTo("200.00");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(account.id(), "2026-08-07")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.receivedDate").value("2026-08-07"));
    assertThat(balance(token, account.id())).isEqualByComparingTo("1200.00");
  }

  @Test
  void shouldAllowNegativeBalanceAfterReverse() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("neg"), "senha-segura");
    AccountResponse account = createAccount(token, "-800.00");
    IncomeResponse created = createIncome(token, "Salário", "1000.00", "2026-08-05");
    receive(token, created.id(), account.id(), "2026-08-06");
    assertThat(balance(token, account.id())).isEqualByComparingTo("200.00");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EXPECTED"));

    assertThat(balance(token, account.id())).isEqualByComparingTo("-800.00");
  }

  @Test
  void shouldCancelExpectedIncomeWithoutChangingBalance() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("cancel"), "senha-segura");
    AccountResponse account = createAccount(token, "1500.00");
    IncomeResponse created = createIncome(token, "Freelance", "1000.00", "2026-08-05");
    BigDecimal before = balance(token, account.id());

    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    assertThat(balance(token, account.id())).isEqualByComparingTo(before);
    assertThat(incomeRepository.findById(created.id())).isPresent();

    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(account.id(), "2026-08-06")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectCancelOfReceivedIncome() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("no-direct-cancel"), "senha-segura");
    AccountResponse account = createAccount(token);
    IncomeResponse created = createIncome(token, "Salário", "100.00", "2026-08-05");
    receive(token, created.id(), account.id(), "2026-08-06");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + created.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

    assertThat(incomeRepository.findById(created.id()).orElseThrow().getStatus())
        .isEqualTo(IncomeStatus.RECEIVED);
  }

  @Test
  void shouldRejectInvalidTransitions() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("invalid"), "senha-segura");
    AccountResponse account = createAccount(token);
    CategoryResponse category = createIncomeCategory(token, "Salário");
    IncomeResponse expected =
        createIncome(token, category.id(), "Esperada", "100.00", "2026-08-05");
    IncomeResponse received =
        createIncome(token, category.id(), "Recebida", "100.00", "2026-08-05");
    receive(token, received.id(), account.id(), "2026-08-06");
    IncomeResponse cancelled =
        createIncome(token, category.id(), "Cancelada", "100.00", "2026-08-05");
    cancel(token, cancelled.id());

    mockMvc
        .perform(
            post("/api/v1/incomes/" + received.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(account.id(), "2026-08-07")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + cancelled.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(account.id(), "2026-08-07")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + expected.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + cancelled.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldIsolateIncomesBetweenUsers() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("iso-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("iso-b"), "senha-segura");
    IncomeResponse incomeA = createIncome(tokenA, "Salário A", "100.00", "2026-08-05");
    AccountResponse accountB = createAccount(tokenB);
    CategoryResponse categoryB = createIncomeCategory(tokenB, "Salário");

    mockMvc
        .perform(
            get("/api/v1/incomes/" + incomeA.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/v1/incomes/" + incomeA.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(categoryB.id(), "Hack", "1.00", "2026-08-05")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeA.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(accountB.id(), "2026-08-06")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeA.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeA.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/incomes").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(0));
  }

  @Test
  void shouldRejectWrongCategoryAndInactiveOrForeignAccount() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("val-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("val-b"), "senha-segura");
    CategoryResponse expense = createCategory(tokenA, "Mercado", "EXPENSE");
    CategoryResponse foreignIncome = createIncomeCategory(tokenB, "Salário B");
    CategoryResponse inactive = createIncomeCategory(tokenA, "Inativa");
    mockMvc
        .perform(
            post("/api/v1/categories/" + inactive.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk());
    AccountResponse foreignAccount = createAccount(tokenB);
    AccountResponse inactiveAccount = createAccount(tokenA, "0.00");
    mockMvc
        .perform(
            post("/api/v1/accounts/" + inactiveAccount.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk());
    CategoryResponse valid = createIncomeCategory(tokenA, "Salário");
    IncomeResponse income = createIncome(tokenA, valid.id(), "Ok", "100.00", "2026-08-05");

    mockMvc
        .perform(
            post("/api/v1/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(expense.id(), "X", "10.00", "2026-08-05")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(foreignIncome.id(), "X", "10.00", "2026-08-05")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(inactive.id(), "X", "10.00", "2026-08-05")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(foreignAccount.id(), "2026-08-06")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(inactiveAccount.id(), "2026-08-06")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectUnknownJsonPropertiesAndUnauthorizedAccess() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("json"), "senha-segura");
    CategoryResponse category = createIncomeCategory(token, "Salário");

    mockMvc
        .perform(
            post("/api/v1/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":"%s","description":"Salário","amount":100.00,"expectedDate":"2026-08-05","userId":"%s"}
                    """
                        .formatted(category.id(), UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            post("/api/v1/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":"%s","description":"Salário","amount":100.00,"expectedDate":"2026-08-05","status":"RECEIVED"}
                    """
                        .formatted(category.id())))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":"%s","description":"Salário","amount":100.00,"expectedDate":"2026-08-05","responsibleType":"MINE"}
                    """
                        .formatted(category.id())))
        .andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/incomes")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/incomes/" + UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/incomes/not-a-uuid").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void expectedIncomeMustNotChangeBalance() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("expected-bal"), "senha-segura");
    AccountResponse account = createAccount(token, "1500.00");
    createIncome(token, "Futuro", "5400.00", "2026-08-05");
    assertThat(balance(token, account.id())).isEqualByComparingTo("1500.00");
  }

  private IncomeResponse createIncome(
      String token, String description, String amount, String expectedDate) throws Exception {
    CategoryResponse category =
        createIncomeCategory(token, "Cat-" + UUID.randomUUID().toString().substring(0, 8));
    return createIncome(token, category.id(), description, amount, expectedDate);
  }

  private IncomeResponse createIncome(
      String token, UUID categoryId, String description, String amount, String expectedDate)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createIncomeJson(categoryId, description, amount, expectedDate)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private void receive(String token, UUID incomeId, UUID accountId, String receivedDate)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiveJson(accountId, receivedDate)))
        .andExpect(status().isOk());
  }

  private void cancel(String token, UUID incomeId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
  }

  private CategoryResponse createIncomeCategory(String token, String name) throws Exception {
    return createCategory(token, name, "INCOME");
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

  private AccountResponse createAccount(String token) throws Exception {
    return createAccount(token, "1500.00");
  }

  private AccountResponse createAccount(String token, String initialBalance) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","type":"BANK_ACCOUNT","initialBalance":%s}
                        """
                            .formatted(initialBalance)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AccountResponse.class);
  }

  private BigDecimal balance(String token, UUID accountId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + accountId + "/balance")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, AccountBalanceResponse.class).balance();
  }

  private String registerAndLogin(String name, String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"%s","email":"%s","password":"%s"}
                    """
                        .formatted(name, email, password)))
        .andExpect(status().isCreated());
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"%s"}
                        """
                            .formatted(email, password)))
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

  private static String createIncomeJson(
      UUID categoryId, String description, String amount, String expectedDate) {
    return """
        {"categoryId":"%s","description":"%s","amount":%s,"expectedDate":"%s"}
        """
        .formatted(categoryId, description, amount, expectedDate);
  }

  private static String receiveJson(UUID accountId, String receivedDate) {
    return """
        {"accountId":"%s","receivedDate":"%s"}
        """
        .formatted(accountId, receivedDate);
  }
}
