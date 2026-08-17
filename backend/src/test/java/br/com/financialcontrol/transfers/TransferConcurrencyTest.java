package br.com.financialcontrol.transfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.transfers.dto.TransferResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
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
class TransferConcurrencyTest {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldPreventConcurrentTransfersFromOverdrawingSourceAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("transfer-race"), "senha-segura");
    AccountResponse source = createAccount(token, "Origem", "100.00");
    AccountResponse destination = createAccount(token, "Destino", "0.00");
    String body = transferJson(source.id(), destination.id(), "100.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    MvcResult firstResult;
    MvcResult secondResult;
    try {
      Future<MvcResult> first = pool.submit(() -> transferResult(token, body, start));
      Future<MvcResult> second = pool.submit(() -> transferResult(token, body, start));
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
    String failureBody = failure.getResponse().getContentAsString();
    assertThat(JsonPath.<String>read(failureBody, "$.code")).isEqualTo("BUSINESS_RULE_VIOLATION");
    assertThat(JsonPath.<String>read(failureBody, "$.message"))
        .isEqualTo("Saldo insuficiente para realizar a operação.");

    assertThat(balance(token, source.id())).isEqualByComparingTo("0.00");
    assertThat(balance(token, destination.id())).isEqualByComparingTo("100.00");

    TransferResponse[] transfers =
        jsonMapper.readValue(
            mockMvc
                .perform(get("/api/v1/transfers").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TransferResponse[].class);
    assertThat(Arrays.stream(transfers).filter(t -> t.status() == TransferStatus.ACTIVE))
        .hasSize(1);
  }

  private MvcResult transferResult(String token, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn();
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
    return jsonMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
  }

  private BigDecimal balance(String token, UUID accountId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + accountId + "/balance")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return jsonMapper
        .readValue(result.getResponse().getContentAsString(), AccountBalanceResponse.class)
        .balance();
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

  private static String transferJson(UUID sourceId, UUID destinationId, String amount) {
    return """
        {"sourceAccountId":"%s","destinationAccountId":"%s","amount":%s,"transferDate":"%s","description":"Concorrente"}
        """
        .formatted(sourceId, destinationId, amount, LocalDate.now(FINANCIAL_ZONE));
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }
}
