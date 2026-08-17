package br.com.financialcontrol.transfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.balance_adjustments.dto.BalanceAdjustmentResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
class Phase14Rn010aApiTest {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldKeepInitialBalanceLockedAfterReceivedIncomeIsReversed() throws Exception {
    String token = registerAndLogin(uniqueEmail("rn010a-income"));
    AccountResponse account = createAccount(token, "Conta", "0.00");
    CategoryResponse category = createCategory(token, "Receitas", "INCOME");
    IncomeResponse income = createIncome(token, category.id(), "100.00");

    receiveIncome(token, income.id(), account.id());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EXPECTED"));

    assertInitialBalanceRejected(token, account.id(), "999.00");
    assertThat(balance(token, account.id())).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldKeepInitialBalanceLockedAfterPaymentIsReversed() throws Exception {
    String token = registerAndLogin(uniqueEmail("rn010a-payment"));
    AccountResponse account = createAccount(token, "Conta", "100.00");
    CategoryResponse category = createCategory(token, "Despesas", "EXPENSE");
    ExpenseResponse expense = createExpense(token, category.id(), account.id(), "25.00");

    payExpense(token, expense.id(), account.id(), "25.00");
    PaymentResponse payment = listPayments(token, expense.id())[0];
    mockMvc
        .perform(
            post("/api/v1/payments/" + payment.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVERSED"));

    assertInitialBalanceRejected(token, account.id(), "999.00");
    assertThat(balance(token, account.id())).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldKeepInitialBalanceLockedAfterTransferIsReversed() throws Exception {
    String token = registerAndLogin(uniqueEmail("rn010a-transfer"));
    AccountResponse source = createAccount(token, "Origem", "100.00");
    AccountResponse destination = createAccount(token, "Destino", "0.00");
    UUID transferId =
        UUID.fromString(
            JsonPath.read(
                mockMvc
                    .perform(
                        post("/api/v1/transfers")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":10.00,"transferDate":"%s","description":"RN010A"}
                                """
                                    .formatted(source.id(), destination.id(), today())))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.id"));

    mockMvc
        .perform(
            post("/api/v1/transfers/" + transferId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    assertInitialBalanceRejected(token, source.id(), "999.00");
    assertInitialBalanceRejected(token, destination.id(), "999.00");
  }

  @Test
  void shouldKeepInitialBalanceLockedAfterBalanceAdjustmentIsReversed() throws Exception {
    String token = registerAndLogin(uniqueEmail("rn010a-adjustment"));
    AccountResponse account = createAccount(token, "Conta", "100.00");
    BalanceAdjustmentResponse adjustment = createAdjustment(token, account.id(), "125.00");

    mockMvc
        .perform(
            post("/api/v1/accounts/"
                    + account.id()
                    + "/balance-adjustments/"
                    + adjustment.id()
                    + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVERSED"));

    assertInitialBalanceRejected(token, account.id(), "999.00");
    assertThat(balance(token, account.id())).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldRejectDeactivationWhenIncomeReverseLeavesNegativeBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("negative-deactivate"));
    AccountResponse account = createAccount(token, "Conta", "0.00");
    CategoryResponse incomeCategory = createCategory(token, "Receitas", "INCOME");
    CategoryResponse expenseCategory = createCategory(token, "Despesas", "EXPENSE");
    IncomeResponse income = createIncome(token, incomeCategory.id(), "100.00");
    receiveIncome(token, income.id(), account.id());
    ExpenseResponse expense = createExpense(token, expenseCategory.id(), account.id(), "100.00");
    payExpense(token, expense.id(), account.id(), "100.00");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
    assertThat(balance(token, account.id())).isEqualByComparingTo("-100.00");

    mockMvc
        .perform(
            post("/api/v1/accounts/" + account.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
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

  private IncomeResponse createIncome(String token, UUID categoryId, String amount)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"Receita","amount":%s,"expectedDate":"%s"}
                        """
                            .formatted(categoryId, amount, today())))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private void receiveIncome(String token, UUID incomeId, UUID accountId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/receive")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","receivedDate":"%s"}
                    """
                        .formatted(accountId, today())))
        .andExpect(status().isOk());
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
                        {"categoryId":"%s","description":"Despesa","totalAmount":%s,"expenseDate":"%s","dueDate":"%s","paymentMethod":"ACCOUNT","accountId":"%s","responsibleType":"MINE"}
                        """
                            .formatted(categoryId, amount, today(), today(), accountId)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private void payExpense(String token, UUID expenseId, UUID accountId, String amount)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"paymentDate":"%s"}
                    """
                        .formatted(accountId, amount, today())))
        .andExpect(status().isOk());
  }

  private PaymentResponse[] listPayments(String token, UUID expenseId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/expenses/" + expenseId + "/payments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, PaymentResponse[].class);
  }

  private BalanceAdjustmentResponse createAdjustment(
      String token, UUID accountId, String reportedBalance) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts/" + accountId + "/balance-adjustments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"reportedBalance":%s,"adjustmentDate":"%s"}
                        """
                            .formatted(reportedBalance, today())))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, BalanceAdjustmentResponse.class);
  }

  private void assertInitialBalanceRejected(String token, UUID accountId, String amount)
      throws Exception {
    mockMvc
        .perform(
            put("/api/v1/accounts/" + accountId + "/initial-balance")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"initialBalance\":%s}".formatted(amount)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
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
