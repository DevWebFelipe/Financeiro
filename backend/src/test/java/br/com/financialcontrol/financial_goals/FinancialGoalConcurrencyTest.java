package br.com.financialcontrol.financial_goals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalResponse;
import com.jayway.jsonpath.JsonPath;
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
class FinancialGoalConcurrencyTest {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldPreventConcurrentContributionsFromExceedingAvailableBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("contrib-race"));
    AccountResponse account = createAccount(token, "Banco", "100.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Corrida", "500.00");
    String body = contributionJson("100.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult firstResult;
    MvcResult secondResult;
    try {
      Future<MvcResult> first = pool.submit(() -> contributeResult(token, goal.id(), body, start));
      Future<MvcResult> second = pool.submit(() -> contributeResult(token, goal.id(), body, start));
      start.countDown();
      firstResult = first.get(30, TimeUnit.SECONDS);
      secondResult = second.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    List<Integer> statuses =
        List.of(firstResult.getResponse().getStatus(), secondResult.getResponse().getStatus());
    assertThat(statuses).containsExactlyInAnyOrder(201, 400);

    MvcResult failure = firstResult.getResponse().getStatus() == 400 ? firstResult : secondResult;
    assertThat(JsonPath.<String>read(failure.getResponse().getContentAsString(), "$.code"))
        .isEqualTo("BUSINESS_RULE_VIOLATION");
    assertThat(JsonPath.<String>read(failure.getResponse().getContentAsString(), "$.message"))
        .isEqualTo(FinancialGoalService.INSUFFICIENT_BALANCE);

    AccountBalanceResponse balance = readBalance(token, account.id());
    assertThat(balance.totalBalance()).isEqualByComparingTo("100.00");
    assertThat(balance.reservedAmount()).isEqualByComparingTo("100.00");
    assertThat(balance.availableBalance()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldPreventConcurrentRedemptionsFromExceedingCurrentAmount() throws Exception {
    String token = registerAndLogin(uniqueEmail("redeem-race"));
    AccountResponse account = createAccount(token, "Banco", "200.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Corrida", "200.00");
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(contributionJson("80.00")))
        .andExpect(status().isCreated());

    String body = redemptionJson("80.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult firstResult;
    MvcResult secondResult;
    try {
      Future<MvcResult> first = pool.submit(() -> redeemResult(token, goal.id(), body, start));
      Future<MvcResult> second = pool.submit(() -> redeemResult(token, goal.id(), body, start));
      start.countDown();
      firstResult = first.get(30, TimeUnit.SECONDS);
      secondResult = second.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    List<Integer> statuses =
        List.of(firstResult.getResponse().getStatus(), secondResult.getResponse().getStatus());
    assertThat(statuses).containsExactlyInAnyOrder(201, 400);

    MvcResult failure = firstResult.getResponse().getStatus() == 400 ? firstResult : secondResult;
    assertThat(JsonPath.<String>read(failure.getResponse().getContentAsString(), "$.code"))
        .isEqualTo("BUSINESS_RULE_VIOLATION");
    assertThat(JsonPath.<String>read(failure.getResponse().getContentAsString(), "$.message"))
        .isEqualTo(FinancialGoalService.REDEMPTION_EXCEEDS_CURRENT);

    AccountBalanceResponse balance = readBalance(token, account.id());
    assertThat(balance.totalBalance()).isEqualByComparingTo("200.00");
    assertThat(balance.reservedAmount()).isEqualByComparingTo("0.00");
    assertThat(balance.availableBalance()).isEqualByComparingTo("200.00");
  }

  @Test
  void shouldKeepConsistencyWhenContributeAndRedeemConcurrently() throws Exception {
    String token = registerAndLogin(uniqueEmail("contrib-redeem-race"));
    AccountResponse account = createAccount(token, "Banco", "180.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Corrida", "500.00");
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(contributionJson("80.00")))
        .andExpect(status().isCreated());

    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    MvcResult contributeResult;
    MvcResult redeemResult;
    try {
      Future<MvcResult> contribute =
          pool.submit(() -> contributeResult(token, goal.id(), contributionJson("100.00"), start));
      Future<MvcResult> redeem =
          pool.submit(() -> redeemResult(token, goal.id(), redemptionJson("80.00"), start));
      start.countDown();
      contributeResult = contribute.get(30, TimeUnit.SECONDS);
      redeemResult = redeem.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    assertThat(contributeResult.getResponse().getStatus()).isEqualTo(201);
    assertThat(redeemResult.getResponse().getStatus()).isEqualTo(201);

    FinancialGoalResponse after = readGoal(token, goal.id());
    assertThat(after.currentAmount()).isEqualByComparingTo("100.00");
    assertThat(after.status()).isEqualTo(FinancialGoalStatus.ACTIVE);

    AccountBalanceResponse balance = readBalance(token, account.id());
    assertThat(balance.totalBalance()).isEqualByComparingTo("180.00");
    assertThat(balance.reservedAmount()).isEqualByComparingTo("100.00");
    assertThat(balance.availableBalance()).isEqualByComparingTo("80.00");
  }

  @Test
  void shouldSerializeTwoGoalsCompetingForSameAvailableBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("two-goals-race"));
    AccountResponse account = createAccount(token, "Banco", "100.00");
    FinancialGoalResponse first = createGoal(token, account.id(), "Primeira", "500.00");
    FinancialGoalResponse second = createGoal(token, account.id(), "Segunda", "500.00");
    String body = contributionJson("100.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult firstResult;
    MvcResult secondResult;
    try {
      Future<MvcResult> firstCall =
          pool.submit(() -> contributeResult(token, first.id(), body, start));
      Future<MvcResult> secondCall =
          pool.submit(() -> contributeResult(token, second.id(), body, start));
      start.countDown();
      firstResult = firstCall.get(30, TimeUnit.SECONDS);
      secondResult = secondCall.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    List<Integer> statuses =
        List.of(firstResult.getResponse().getStatus(), secondResult.getResponse().getStatus());
    assertThat(statuses).containsExactlyInAnyOrder(201, 400);

    MvcResult failure = firstResult.getResponse().getStatus() == 400 ? firstResult : secondResult;
    assertThat(JsonPath.<String>read(failure.getResponse().getContentAsString(), "$.code"))
        .isEqualTo("BUSINESS_RULE_VIOLATION");
    assertThat(JsonPath.<String>read(failure.getResponse().getContentAsString(), "$.message"))
        .isEqualTo(FinancialGoalService.INSUFFICIENT_BALANCE);

    AccountBalanceResponse balance = readBalance(token, account.id());
    assertThat(balance.totalBalance()).isEqualByComparingTo("100.00");
    assertThat(balance.reservedAmount()).isEqualByComparingTo("100.00");
    assertThat(balance.availableBalance()).isEqualByComparingTo("0.00");

    FinancialGoalResponse firstAfter = readGoal(token, first.id());
    FinancialGoalResponse secondAfter = readGoal(token, second.id());
    assertThat(firstAfter.currentAmount().add(secondAfter.currentAmount()))
        .isEqualByComparingTo("100.00");
  }

  private MvcResult contributeResult(String token, UUID goalId, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goalId + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn();
  }

  private MvcResult redeemResult(String token, UUID goalId, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goalId + "/redemptions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn();
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
                        {"accountId":"%s","name":"%s","targetAmount":%s}
                        """
                            .formatted(accountId, name, target)))
            .andExpect(status().isCreated())
            .andReturn();
    return jsonMapper.readValue(
        result.getResponse().getContentAsString(), FinancialGoalResponse.class);
  }

  private AccountResponse createAccount(String token, String name, String initial)
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
                            .formatted(name, initial)))
            .andExpect(status().isCreated())
            .andReturn();
    return jsonMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
  }

  private AccountBalanceResponse readBalance(String token, UUID accountId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + accountId + "/balance")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return jsonMapper.readValue(
        result.getResponse().getContentAsString(), AccountBalanceResponse.class);
  }

  private FinancialGoalResponse readGoal(String token, UUID goalId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/financial-goals/" + goalId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return jsonMapper.readValue(
        result.getResponse().getContentAsString(), FinancialGoalResponse.class);
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

  private static String contributionJson(String amount) {
    return """
        {"amount":%s,"contributionDate":"%s"}
        """
        .formatted(amount, LocalDate.now(FINANCIAL_ZONE));
  }

  private static String redemptionJson(String amount) {
    return """
        {"amount":%s,"redemptionDate":"%s"}
        """
        .formatted(amount, LocalDate.now(FINANCIAL_ZONE));
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }
}
