package br.com.financialcontrol.transfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class Phase14ConcurrencyExtraTest {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldSerializeInitialBalanceUpdateAgainstFirstTransfer() throws Exception {
    String token = registerAndLogin(uniqueEmail("initial-transfer-race"));
    AccountResponse source = createAccount(token, "Origem", "100.00");
    AccountResponse destination = createAccount(token, "Destino", "0.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult updateResult;
    MvcResult transferResult;
    try {
      Future<MvcResult> update =
          pool.submit(
              () ->
                  performAfterStart(
                      put("/api/v1/accounts/" + source.id() + "/initial-balance")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("{\"initialBalance\":999.00}"),
                      start));
      Future<MvcResult> transfer =
          pool.submit(
              () ->
                  performAfterStart(
                      post("/api/v1/transfers")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(transferJson(source.id(), destination.id(), "10.00")),
                      start));
      start.countDown();
      updateResult = update.get(30, TimeUnit.SECONDS);
      transferResult = transfer.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    assertThat(transferResult.getResponse().getStatus()).isEqualTo(201);
    assertThat(updateResult.getResponse().getStatus()).isIn(200, 400);
    AccountResponse persisted = getAccount(token, source.id());
    if (updateResult.getResponse().getStatus() == 200) {
      assertThat(persisted.initialBalance()).isEqualByComparingTo("999.00");
      assertThat(balance(token, source.id())).isEqualByComparingTo("989.00");
    } else {
      assertThat(persisted.initialBalance()).isEqualByComparingTo("100.00");
      assertThat(balance(token, source.id())).isEqualByComparingTo("90.00");
    }
    assertThat(balance(token, destination.id())).isEqualByComparingTo("10.00");

    mockMvc
        .perform(
            put("/api/v1/accounts/" + source.id() + "/initial-balance")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"initialBalance\":777.00}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldSerializePaymentAndTransferWithoutOverdrawingAccount() throws Exception {
    String token = registerAndLogin(uniqueEmail("payment-transfer-race"));
    AccountResponse source = createAccount(token, "Origem", "1000.00");
    AccountResponse destination = createAccount(token, "Destino", "0.00");
    CategoryResponse category = createExpenseCategory(token);
    ExpenseResponse expense = createExpense(token, category.id(), source.id(), "700.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult paymentResult;
    MvcResult transferResult;
    try {
      Future<MvcResult> payment =
          pool.submit(
              () ->
                  performAfterStart(
                      post("/api/v1/expenses/" + expense.id() + "/pay")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(paymentJson(source.id(), "700.00")),
                      start));
      Future<MvcResult> transfer =
          pool.submit(
              () ->
                  performAfterStart(
                      post("/api/v1/transfers")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(transferJson(source.id(), destination.id(), "700.00")),
                      start));
      start.countDown();
      paymentResult = payment.get(30, TimeUnit.SECONDS);
      transferResult = transfer.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    List<Integer> statuses =
        List.of(paymentResult.getResponse().getStatus(), transferResult.getResponse().getStatus());
    assertThat(statuses).contains(400);
    assertThat(statuses.stream().filter(status -> status == 200 || status == 201)).hasSize(1);
    assertThat(balance(token, source.id())).isEqualByComparingTo("300.00");
    assertThat(balance(token, source.id())).isNotNegative();
  }

  @Test
  void shouldAvoidDeadlockForOppositeDirectionTransfers() throws Exception {
    String token = registerAndLogin(uniqueEmail("opposite-transfer-race"));
    AccountResponse accountA = createAccount(token, "Conta A", "100.00");
    AccountResponse accountB = createAccount(token, "Conta B", "100.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult resultA;
    MvcResult resultB;
    try {
      Future<MvcResult> transferA =
          pool.submit(
              () ->
                  performAfterStart(
                      post("/api/v1/transfers")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(transferJson(accountA.id(), accountB.id(), "70.00")),
                      start));
      Future<MvcResult> transferB =
          pool.submit(
              () ->
                  performAfterStart(
                      post("/api/v1/transfers")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(transferJson(accountB.id(), accountA.id(), "70.00")),
                      start));
      start.countDown();
      resultA = transferA.get(30, TimeUnit.SECONDS);
      resultB = transferB.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    assertThat(resultA.getResponse().getStatus()).isIn(201, 400);
    assertThat(resultB.getResponse().getStatus()).isIn(201, 400);
    BigDecimal balanceA = balance(token, accountA.id());
    BigDecimal balanceB = balance(token, accountB.id());
    assertThat(balanceA).isNotNegative();
    assertThat(balanceB).isNotNegative();
    assertThat(balanceA.add(balanceB)).isEqualByComparingTo("200.00");
  }

  @Test
  void shouldAllowExactlyOneOfTwoConcurrentFullBalanceTransfers() throws Exception {
    String token = registerAndLogin(uniqueEmail("full-balance-race"));
    AccountResponse source = createAccount(token, "Origem", "100.00");
    AccountResponse destinationA = createAccount(token, "Destino A", "0.00");
    AccountResponse destinationB = createAccount(token, "Destino B", "0.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult resultA;
    MvcResult resultB;
    try {
      Future<MvcResult> transferA =
          pool.submit(
              () ->
                  performAfterStart(
                      post("/api/v1/transfers")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(transferJson(source.id(), destinationA.id(), "100.00")),
                      start));
      Future<MvcResult> transferB =
          pool.submit(
              () ->
                  performAfterStart(
                      post("/api/v1/transfers")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(transferJson(source.id(), destinationB.id(), "100.00")),
                      start));
      start.countDown();
      resultA = transferA.get(30, TimeUnit.SECONDS);
      resultB = transferB.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    assertThat(List.of(resultA.getResponse().getStatus(), resultB.getResponse().getStatus()))
        .containsExactlyInAnyOrder(201, 400);
    assertThat(balance(token, source.id())).isEqualByComparingTo("0.00");
    assertThat(balance(token, destinationA.id()).add(balance(token, destinationB.id())))
        .isEqualByComparingTo("100.00");
  }

  private MvcResult performAfterStart(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc.perform(request).andReturn();
  }

  private AccountResponse createAccount(String token, String name, String initialBalance)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"BANK_ACCOUNT","initialBalance":%s}
                        """
                            .formatted(name, initialBalance)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AccountResponse.class);
  }

  private AccountResponse getAccount(String token, UUID accountId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + accountId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, AccountResponse.class);
  }

  private CategoryResponse createExpenseCategory(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Despesas\",\"type\":\"EXPENSE\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, CategoryResponse.class);
  }

  private ExpenseResponse createExpense(
      String token, UUID categoryId, UUID accountId, String amount) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"Concorrente","totalAmount":%s,"expenseDate":"%s","dueDate":"%s","paymentMethod":"ACCOUNT","accountId":"%s","responsibleType":"MINE"}
                        """
                            .formatted(categoryId, amount, today(), today(), accountId)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
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

  private static String transferJson(UUID sourceId, UUID destinationId, String amount) {
    return """
        {"sourceAccountId":"%s","destinationAccountId":"%s","amount":%s,"transferDate":"%s","description":"Concorrente"}
        """
        .formatted(sourceId, destinationId, amount, today());
  }

  private static String paymentJson(UUID accountId, String amount) {
    return """
        {"accountId":"%s","amount":%s,"paymentDate":"%s"}
        """
        .formatted(accountId, amount, today());
  }

  private static LocalDate today() {
    return LocalDate.now(FINANCIAL_ZONE);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }
}
