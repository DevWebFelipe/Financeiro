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
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.transfers.dto.TransferResponse;
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
class Phase14ApiTest {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldCreateBankTransferUpdateBalancesAndPreserveNetWorth() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("transfer"), "senha-segura");
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "150.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "25.00");
    BigDecimal netWorthBefore = balance(token, source.id()).add(balance(token, destination.id()));

    TransferResponse transfer =
        createTransfer(token, source.id(), destination.id(), "40.00", today(), "Reserva");

    assertThat(transfer.status()).isEqualTo(TransferStatus.ACTIVE);
    assertThat(transfer.amount()).isEqualByComparingTo("40.00");
    assertThat(balance(token, source.id())).isEqualByComparingTo("110.00");
    assertThat(balance(token, destination.id())).isEqualByComparingTo("65.00");
    assertThat(balance(token, source.id()).add(balance(token, destination.id())))
        .isEqualByComparingTo(netWorthBefore);
  }

  @Test
  void shouldRejectCashAsEitherTransferEndpoint() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("cash-transfer"), "senha-segura");
    AccountResponse bank = createAccount(token, "Banco", "BANK_ACCOUNT", "100.00");
    AccountResponse cash = createAccount(token, "Carteira", "CASH", "100.00");

    assertBusinessRule(postTransfer(token, cash.id(), bank.id(), "10.00", today(), "Cash source"));
    assertBusinessRule(
        postTransfer(token, bank.id(), cash.id(), "10.00", today(), "Cash destination"));

    assertThat(balance(token, bank.id())).isEqualByComparingTo("100.00");
    assertThat(balance(token, cash.id())).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldRejectSameAccountInsufficientBalanceAndFutureDate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("transfer-rules"), "senha-segura");
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "20.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "0.00");

    assertBusinessRule(
        postTransfer(token, source.id(), source.id(), "1.00", today(), "Mesma conta"));
    assertBusinessRule(
        postTransfer(token, source.id(), destination.id(), "20.01", today(), "Sem saldo"));
    assertBusinessRule(
        postTransfer(token, source.id(), destination.id(), "1.00", tomorrow(), "Futura"));
  }

  @Test
  void shouldAllowRetroactiveTransferDate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("retro-transfer"), "senha-segura");
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "50.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "0.00");

    TransferResponse transfer =
        createTransfer(token, source.id(), destination.id(), "10.00", yesterday(), "Retroativa");

    assertThat(transfer.transferDate()).isEqualTo(yesterday());
    assertThat(balance(token, source.id())).isEqualByComparingTo("40.00");
    assertThat(balance(token, destination.id())).isEqualByComparingTo("10.00");
  }

  @Test
  void shouldReverseActiveTransferRestoreBalancesAndRejectSecondReverse() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("reverse-transfer"), "senha-segura");
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "100.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "10.00");
    TransferResponse transfer =
        createTransfer(token, source.id(), destination.id(), "30.00", today(), "Transferência");

    mockMvc
        .perform(
            post("/api/v1/transfers/" + transfer.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVERSED"));

    assertThat(balance(token, source.id())).isEqualByComparingTo("100.00");
    assertThat(balance(token, destination.id())).isEqualByComparingTo("10.00");

    assertBusinessRule(
        post("/api/v1/transfers/" + transfer.id() + "/reverse")
            .header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  @Test
  void shouldRejectTransferReverseWhenDestinationNoLongerHasEnoughBalance() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("reverse-no-balance"), "senha-segura");
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "100.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "0.00");
    AccountResponse third = createAccount(token, "Terceira", "BANK_ACCOUNT", "0.00");
    TransferResponse first =
        createTransfer(token, source.id(), destination.id(), "50.00", today(), "Entrada");
    createTransfer(token, destination.id(), third.id(), "40.00", today(), "Outro gasto");

    assertBusinessRule(
        post("/api/v1/transfers/" + first.id() + "/reverse")
            .header(HttpHeaders.AUTHORIZATION, bearer(token)));

    assertThat(balance(token, destination.id())).isEqualByComparingTo("10.00");
    mockMvc
        .perform(
            get("/api/v1/transfers/" + first.id()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldHideTransferFromOtherUsersForGetAndReverse() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("transfer-owner-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("transfer-owner-b"), "senha-segura");
    AccountResponse source = createAccount(tokenA, "Origem", "BANK_ACCOUNT", "100.00");
    AccountResponse destination = createAccount(tokenA, "Destino", "BANK_ACCOUNT", "0.00");
    TransferResponse transfer =
        createTransfer(tokenA, source.id(), destination.id(), "10.00", today(), "Privada");

    mockMvc
        .perform(
            get("/api/v1/transfers/" + transfer.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    mockMvc
        .perform(
            post("/api/v1/transfers/" + transfer.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldCreatePositiveAndNegativeBalanceAdjustments() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("adjustments"), "senha-segura");
    AccountResponse positive = createAccount(token, "Positiva", "BANK_ACCOUNT", "100.00");
    AccountResponse negative = createAccount(token, "Negativa", "BANK_ACCOUNT", "100.00");

    BalanceAdjustmentResponse increase = createAdjustment(token, positive.id(), "130.00", today());
    BalanceAdjustmentResponse decrease = createAdjustment(token, negative.id(), "35.00", today());

    assertThat(increase.calculatedBalance()).isEqualByComparingTo("100.00");
    assertThat(increase.adjustmentAmount()).isEqualByComparingTo("30.00");
    assertThat(increase.status().name()).isEqualTo("ACTIVE");
    assertThat(decrease.calculatedBalance()).isEqualByComparingTo("100.00");
    assertThat(decrease.adjustmentAmount()).isEqualByComparingTo("-65.00");
    assertThat(balance(token, positive.id())).isEqualByComparingTo("130.00");
    assertThat(balance(token, negative.id())).isEqualByComparingTo("35.00");
  }

  @Test
  void shouldCreateZeroDifferenceBalanceAdjustment() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("zero-adjustment"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "100.00");

    BalanceAdjustmentResponse adjustment = createAdjustment(token, account.id(), "100.00", today());

    assertThat(adjustment.calculatedBalance()).isEqualByComparingTo("100.00");
    assertThat(adjustment.reportedBalance()).isEqualByComparingTo("100.00");
    assertThat(adjustment.adjustmentAmount()).isEqualByComparingTo("0.00");
    assertThat(adjustment.status().name()).isEqualTo("ACTIVE");
    assertThat(balance(token, account.id())).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldDefaultOmittedAdjustmentDateToFinancialToday() throws Exception {
    String token =
        registerAndLogin("Alice", uniqueEmail("default-adjustment-date"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "100.00");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts/" + account.id() + "/balance-adjustments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reportedBalance\":110.00}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.adjustmentDate").value(today().toString()))
            .andReturn();

    BalanceAdjustmentResponse adjustment = read(result, BalanceAdjustmentResponse.class);
    assertThat(adjustment.adjustmentDate()).isEqualTo(today());
    assertThat(adjustment.adjustmentAmount()).isEqualByComparingTo("10.00");
  }

  @Test
  void shouldRejectBalanceAdjustmentForInactiveAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("inactive-adjustment"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "0.00");
    mockMvc
        .perform(
            post("/api/v1/accounts/" + account.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    assertBusinessRule(
        post("/api/v1/accounts/" + account.id() + "/balance-adjustments")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(adjustmentJson("10.00", today())));
    assertThat(balance(token, account.id())).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldRejectNegativeReportedBalanceAndFutureAdjustmentDate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("adjustment-validation"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "100.00");

    mockMvc
        .perform(
            post("/api/v1/accounts/" + account.id() + "/balance-adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("-0.01", today())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fields.reportedBalance").exists());

    assertBusinessRule(
        post("/api/v1/accounts/" + account.id() + "/balance-adjustments")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(adjustmentJson("100.00", tomorrow())));
  }

  @Test
  void shouldAllowBalanceAdjustmentForCashAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("cash-adjustment"), "senha-segura");
    AccountResponse cash = createAccount(token, "Carteira", "CASH", "20.00");

    BalanceAdjustmentResponse adjustment = createAdjustment(token, cash.id(), "25.00", today());

    assertThat(adjustment.adjustmentAmount()).isEqualByComparingTo("5.00");
    assertThat(balance(token, cash.id())).isEqualByComparingTo("25.00");
  }

  @Test
  void shouldUseAsOfBalanceForRetroactiveAdjustment() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("as-of-adjustment"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "100.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "0.00");
    createTransfer(token, account.id(), destination.id(), "30.00", today(), "Movimento posterior");

    BalanceAdjustmentResponse adjustment =
        createAdjustment(token, account.id(), "80.00", yesterday());

    assertThat(adjustment.calculatedBalance()).isEqualByComparingTo("100.00");
    assertThat(adjustment.adjustmentAmount()).isEqualByComparingTo("-20.00");
    assertThat(balance(token, account.id())).isEqualByComparingTo("50.00");
  }

  @Test
  void shouldReverseAdjustmentAndRejectSecondReverse() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("reverse-adjustment"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "100.00");
    BalanceAdjustmentResponse adjustment = createAdjustment(token, account.id(), "125.00", today());

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
    assertThat(balance(token, account.id())).isEqualByComparingTo("100.00");

    assertBusinessRule(
        post("/api/v1/accounts/"
                + account.id()
                + "/balance-adjustments/"
                + adjustment.id()
                + "/reverse")
            .header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  @Test
  void shouldApplyMultipleAdjustmentsIndependently() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("multiple-adjustments"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "100.00");

    BalanceAdjustmentResponse first = createAdjustment(token, account.id(), "120.00", today());
    BalanceAdjustmentResponse second = createAdjustment(token, account.id(), "110.00", today());

    assertThat(first.calculatedBalance()).isEqualByComparingTo("100.00");
    assertThat(first.adjustmentAmount()).isEqualByComparingTo("20.00");
    assertThat(second.calculatedBalance()).isEqualByComparingTo("120.00");
    assertThat(second.adjustmentAmount()).isEqualByComparingTo("-10.00");
    assertThat(balance(token, account.id())).isEqualByComparingTo("110.00");
  }

  @Test
  void shouldDefaultInitialBalanceToZeroAndAllowUpdatingBeforeMovements() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("initial-balance"), "senha-segura");
    AccountResponse account = createAccountWithoutInitialBalance(token, "Conta", "BANK_ACCOUNT");

    assertThat(account.initialBalance()).isEqualByComparingTo("0.00");
    assertThat(balance(token, account.id())).isEqualByComparingTo("0.00");

    mockMvc
        .perform(
            put("/api/v1/accounts/" + account.id() + "/initial-balance")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"initialBalance\":75.00}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.initialBalance").value(75.00));
    assertThat(balance(token, account.id())).isEqualByComparingTo("75.00");
  }

  @Test
  void shouldRejectInitialBalanceUpdateAfterReceivedIncome() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("initial-after-income"), "senha-segura");
    AccountResponse account = createAccount(token, "Conta", "BANK_ACCOUNT", "0.00");
    CategoryResponse category = createIncomeCategory(token);
    IncomeResponse income = createIncome(token, category.id(), "100.00");
    receiveIncome(token, income.id(), account.id());

    assertBusinessRule(
        put("/api/v1/accounts/" + account.id() + "/initial-balance")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"initialBalance\":10.00}"));
  }

  @Test
  void shouldKeepInitialBalanceLockedAfterTransferIsReversed() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("initial-after-transfer"), "senha-segura");
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "100.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "0.00");
    TransferResponse transfer =
        createTransfer(token, source.id(), destination.id(), "10.00", today(), "Movimento");
    mockMvc
        .perform(
            post("/api/v1/transfers/" + transfer.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    assertBusinessRule(
        put("/api/v1/accounts/" + source.id() + "/initial-balance")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"initialBalance\":200.00}"));
  }

  @Test
  void shouldRejectDeactivationWithNonzeroBalanceAndAllowAtZero() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("deactivate"), "senha-segura");
    AccountResponse nonzero = createAccount(token, "Com saldo", "BANK_ACCOUNT", "1.00");
    AccountResponse zero = createAccount(token, "Sem saldo", "CASH", "0.00");

    assertBusinessRule(
        post("/api/v1/accounts/" + nonzero.id() + "/deactivate")
            .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    mockMvc
        .perform(
            post("/api/v1/accounts/" + zero.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  private TransferResponse createTransfer(
      String token,
      UUID sourceId,
      UUID destinationId,
      String amount,
      LocalDate transferDate,
      String description)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                postTransfer(token, sourceId, destinationId, amount, transferDate, description))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();
    return read(result, TransferResponse.class);
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postTransfer(
      String token,
      UUID sourceId,
      UUID destinationId,
      String amount,
      LocalDate transferDate,
      String description) {
    return post("/api/v1/transfers")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(transferJson(sourceId, destinationId, amount, transferDate, description));
  }

  private BalanceAdjustmentResponse createAdjustment(
      String token, UUID accountId, String reportedBalance, LocalDate adjustmentDate)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts/" + accountId + "/balance-adjustments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustmentJson(reportedBalance, adjustmentDate)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, BalanceAdjustmentResponse.class);
  }

  private AccountResponse createAccount(
      String token, String name, String type, String initialBalance) throws Exception {
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
                            .formatted(name, type, initialBalance)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AccountResponse.class);
  }

  private AccountResponse createAccountWithoutInitialBalance(String token, String name, String type)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"%s"}
                        """
                            .formatted(name, type)))
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

  private CategoryResponse createIncomeCategory(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Salário-%s","type":"INCOME"}
                        """
                            .formatted(UUID.randomUUID().toString().substring(0, 8))))
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
                        {"categoryId":"%s","description":"Salário","amount":%s,"expectedDate":"%s"}
                        """
                            .formatted(categoryId, amount, today())))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private void receiveIncome(String token, UUID incomeId, UUID accountId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":100.00,"date":"%s"}
                    """
                        .formatted(accountId, today())))
        .andExpect(status().isCreated());
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

  private void assertBusinessRule(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
      throws Exception {
    mockMvc
        .perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  private <T> T read(MvcResult result, Class<T> type) throws Exception {
    return jsonMapper.readValue(result.getResponse().getContentAsString(), type);
  }

  private static String transferJson(
      UUID sourceId,
      UUID destinationId,
      String amount,
      LocalDate transferDate,
      String description) {
    return """
        {"sourceAccountId":"%s","destinationAccountId":"%s","amount":%s,"transferDate":"%s","description":"%s"}
        """
        .formatted(sourceId, destinationId, amount, transferDate, description);
  }

  private static String adjustmentJson(String reportedBalance, LocalDate adjustmentDate) {
    return """
        {"reportedBalance":%s,"adjustmentDate":"%s"}
        """
        .formatted(reportedBalance, adjustmentDate);
  }

  private static LocalDate today() {
    return LocalDate.now(FINANCIAL_ZONE);
  }

  private static LocalDate yesterday() {
    return today().minusDays(1);
  }

  private static LocalDate tomorrow() {
    return today().plusDays(1);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }
}
