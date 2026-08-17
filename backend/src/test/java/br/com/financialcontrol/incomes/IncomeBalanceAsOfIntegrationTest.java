package br.com.financialcontrol.incomes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountRepository;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class IncomeBalanceAsOfIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private AccountService accountService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private IncomeMovementRepository incomeMovementRepository;

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-08-17T15:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Test
  void shouldCalculateBalanceAsOfUsingMovementDateNotIncomeHeader() throws Exception {
    String token = registerAndLogin(uniqueEmail("as-of-rn240"));
    AccountResponse account = createAccount(token, "100.00");
    CategoryResponse category = createIncomeCategory(token, "Salário");
    IncomeResponse income = createIncome(token, category.id(), "Salário", "1000.00", "2026-08-05");
    Account stored = accountRepository.findById(account.id()).orElseThrow();

    receipt(token, income.id(), account.id(), "300.00", "2026-08-10");
    receipt(token, income.id(), account.id(), "200.00", "2026-08-15");

    assertThat(accountService.calculateBalanceAsOf(stored, LocalDate.of(2026, 8, 12)))
        .isEqualByComparingTo("400.00");
    assertThat(accountService.calculateBalanceAsOf(stored, LocalDate.of(2026, 8, 15)))
        .isEqualByComparingTo("600.00");
    assertThat(accountService.calculateCurrentBalance(stored)).isEqualByComparingTo("600.00");
  }

  @Test
  void shouldExcludeReversedReceiptFromBalanceAsOf() throws Exception {
    String token = registerAndLogin(uniqueEmail("as-of-reverse"));
    AccountResponse account = createAccount(token, "50.00");
    CategoryResponse category = createIncomeCategory(token, "Salário");
    IncomeResponse income = createIncome(token, category.id(), "Salário", "500.00", "2026-08-05");
    Account stored = accountRepository.findById(account.id()).orElseThrow();

    receipt(token, income.id(), account.id(), "500.00", "2026-08-10");
    assertThat(accountService.calculateCurrentBalance(stored)).isEqualByComparingTo("550.00");

    UUID movementId = firstReceiptMovementId(token, income.id());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/movements/" + movementId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    assertThat(accountService.calculateCurrentBalance(stored)).isEqualByComparingTo("50.00");
    assertThat(accountService.calculateBalanceAsOf(stored, LocalDate.of(2026, 8, 10)))
        .isEqualByComparingTo("50.00");
    assertThat(
            incomeMovementRepository.findAll().stream()
                .filter(m -> m.getIncome().getId().equals(income.id()))
                .filter(m -> m.getType() == IncomeMovementType.RECEIPT)
                .filter(m -> m.getStatus() == IncomeMovementStatus.REVERSED)
                .count())
        .isEqualTo(1);
  }

  private UUID firstReceiptMovementId(String token, UUID incomeId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/incomes/" + incomeId + "/movements")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return UUID.fromString(
        JsonPath.read(result.getResponse().getContentAsString(), "$.items[0].id"));
  }

  private void receipt(String token, UUID incomeId, UUID accountId, String amount, String date)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptJson(accountId, amount, date)))
        .andExpect(status().isCreated());
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
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","amount":%s,"expectedDate":"%s"}
                        """
                            .formatted(categoryId, description, amount, expectedDate)))
            .andExpect(status().isCreated())
            .andReturn();
    return jsonMapper.readValue(result.getResponse().getContentAsString(), IncomeResponse.class);
  }

  private CategoryResponse createIncomeCategory(String token, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"INCOME"}
                        """
                            .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    return jsonMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
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
    return jsonMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
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

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private static String receiptJson(UUID accountId, String amount, String date) {
    return """
        {"accountId":"%s","amount":%s,"date":"%s"}
        """
        .formatted(accountId, amount, date);
  }
}
