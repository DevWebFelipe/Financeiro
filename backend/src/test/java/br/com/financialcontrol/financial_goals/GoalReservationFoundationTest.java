package br.com.financialcontrol.financial_goals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountRepository;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.accounts.AccountType;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class GoalReservationFoundationTest {

  private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private AccountService accountService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private FinancialGoalRepository financialGoalRepository;
  @Autowired private GoalContributionRepository goalContributionRepository;
  @Autowired private GoalRedemptionRepository goalRedemptionRepository;

  @Test
  void shouldDeriveCurrentAmountFromContributionsMinusRedemptions() {
    Account account = persistAccount("1000.00");
    FinancialGoal goal = persistGoal(account, FinancialGoalStatus.ACTIVE, "5000.00");

    assertThat(accountService.calculateGoalCurrentAmount(goal.getId(), account.getUserId(), null))
        .isEqualByComparingTo("0.00");

    persistContribution(goal, "300.00", LocalDate.of(2026, 8, 1));
    assertThat(accountService.calculateGoalCurrentAmount(goal.getId(), account.getUserId(), null))
        .isEqualByComparingTo("300.00");

    persistContribution(goal, "200.00", LocalDate.of(2026, 8, 2));
    persistRedemption(goal, "150.00", LocalDate.of(2026, 8, 3));
    assertThat(accountService.calculateGoalCurrentAmount(goal.getId(), account.getUserId(), null))
        .isEqualByComparingTo("350.00");
    assertThat(financialGoalRepository.findById(goal.getId()).orElseThrow())
        .extracting("targetAmount")
        .isEqualTo(new BigDecimal("5000.00"));
  }

  @Test
  void shouldReserveCompletedGoalUntilRedeemedAndIgnoreZeroCompleted() {
    Account account = persistAccount("1000.00");
    FinancialGoal completedWithMoney =
        persistGoal(account, FinancialGoalStatus.COMPLETED, "5000.00");
    persistContribution(completedWithMoney, "400.00", LocalDate.of(2026, 8, 1));
    FinancialGoal completedZero = persistGoal(account, FinancialGoalStatus.COMPLETED, "1000.00");
    persistContribution(completedZero, "50.00", LocalDate.of(2026, 8, 1));
    persistRedemption(completedZero, "50.00", LocalDate.of(2026, 8, 2));

    assertThat(accountService.calculateReservedAmount(account)).isEqualByComparingTo("400.00");
    assertThat(accountService.calculateCurrentBalance(account)).isEqualByComparingTo("1000.00");
    assertThat(accountService.calculateAvailableBalance(account)).isEqualByComparingTo("600.00");
  }

  @Test
  void shouldKeepTotalBalanceUnchangedWhenContributingAndRedeeming() {
    Account account = persistAccount("10000.00");
    FinancialGoal goal = persistGoal(account, FinancialGoalStatus.ACTIVE, "6000.00");

    persistContribution(goal, "6000.00", LocalDate.of(2026, 8, 10));
    assertThat(accountService.calculateCurrentBalance(account)).isEqualByComparingTo("10000.00");
    assertThat(accountService.calculateReservedAmount(account)).isEqualByComparingTo("6000.00");
    assertThat(accountService.calculateAvailableBalance(account)).isEqualByComparingTo("4000.00");

    persistRedemption(goal, "2000.00", LocalDate.of(2026, 8, 11));
    assertThat(accountService.calculateCurrentBalance(account)).isEqualByComparingTo("10000.00");
    assertThat(accountService.calculateReservedAmount(account)).isEqualByComparingTo("4000.00");
    assertThat(accountService.calculateAvailableBalance(account)).isEqualByComparingTo("6000.00");
  }

  @Test
  void shouldApplyContributionAndRedemptionOnlyOnOrAfterFinancialDate() {
    Account account = persistAccount("1000.00");
    FinancialGoal goal = persistGoal(account, FinancialGoalStatus.ACTIVE, "500.00");
    persistContribution(goal, "300.00", LocalDate.of(2026, 8, 10));
    persistRedemption(goal, "100.00", LocalDate.of(2026, 8, 20));

    assertThat(accountService.calculateReservedAmountAsOf(account, LocalDate.of(2026, 8, 9)))
        .isEqualByComparingTo("0.00");
    assertThat(accountService.calculateReservedAmountAsOf(account, LocalDate.of(2026, 8, 10)))
        .isEqualByComparingTo("300.00");
    assertThat(accountService.calculateReservedAmountAsOf(account, LocalDate.of(2026, 8, 19)))
        .isEqualByComparingTo("300.00");
    assertThat(accountService.calculateReservedAmountAsOf(account, LocalDate.of(2026, 8, 20)))
        .isEqualByComparingTo("200.00");
    assertThat(accountService.calculateAvailableBalanceAsOf(account, LocalDate.of(2026, 8, 10)))
        .isEqualByComparingTo("700.00");
    assertThat(accountService.calculateBalanceAsOf(account, LocalDate.of(2026, 8, 10)))
        .isEqualByComparingTo("1000.00");
  }

  @Test
  void shouldTreatContributionAndRedemptionAsFinancialMovementsForInitialBalance()
      throws Exception {
    String token = registerAndLogin(uniqueEmail("rn010a-goal"));
    AccountResponse created = createAccount(token, "Conta", "100.00");
    Account account = accountRepository.findById(created.id()).orElseThrow();
    FinancialGoal goal = persistGoal(account, FinancialGoalStatus.ACTIVE, "50.00");
    persistContribution(goal, "10.00", LocalDate.of(2026, 8, 10));

    assertThat(accountService.hasFinancialMovements(account)).isTrue();
    mockMvc
        .perform(
            put("/api/v1/accounts/" + account.getId() + "/initial-balance")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"initialBalance\":999.00}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

    String tokenRedeem = registerAndLogin(uniqueEmail("rn010a-redeem"));
    AccountResponse redeemAccount = createAccount(tokenRedeem, "Conta", "80.00");
    Account stored = accountRepository.findById(redeemAccount.id()).orElseThrow();
    FinancialGoal redeemGoal = persistGoal(stored, FinancialGoalStatus.ACTIVE, "50.00");
    persistContribution(redeemGoal, "10.00", LocalDate.of(2026, 8, 10));
    persistRedemption(redeemGoal, "10.00", LocalDate.of(2026, 8, 11));
    mockMvc
        .perform(
            put("/api/v1/accounts/" + stored.getId() + "/initial-balance")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenRedeem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"initialBalance\":1.00}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldRejectDeactivateWhenAccountHasReservedAmount() throws Exception {
    String token = registerAndLogin(uniqueEmail("deactivate-reserved"));
    AccountResponse created = createAccount(token, "Conta", "0.00");
    Account account = accountRepository.findById(created.id()).orElseThrow();
    FinancialGoal goal = persistGoal(account, FinancialGoalStatus.ACTIVE, "50.00");
    persistContribution(goal, "25.00", LocalDate.of(2026, 8, 10));

    mockMvc
        .perform(
            post("/api/v1/accounts/" + account.getId() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldRejectTransferThatExceedsAvailableButNotTotalBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("available-limit"));
    AccountResponse source = createAccount(token, "Origem", "10000.00");
    AccountResponse destination = createAccount(token, "Destino", "0.00");
    Account account = accountRepository.findById(source.id()).orElseThrow();
    FinancialGoal goal = persistGoal(account, FinancialGoalStatus.ACTIVE, "6000.00");
    persistContribution(goal, "6000.00", LocalDate.of(2026, 8, 10));

    AccountBalanceResponse balance = readBalance(token, source.id());
    assertThat(balance.totalBalance()).isEqualByComparingTo("10000.00");
    assertThat(balance.reservedAmount()).isEqualByComparingTo("6000.00");
    assertThat(balance.availableBalance()).isEqualByComparingTo("4000.00");
    assertThat(balance.balance()).isEqualByComparingTo("10000.00");

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson(source.id(), destination.id(), "5000.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson(source.id(), destination.id(), "4000.00")))
        .andExpect(status().isCreated());

    AccountBalanceResponse after = readBalance(token, source.id());
    assertThat(after.totalBalance()).isEqualByComparingTo("6000.00");
    assertThat(after.reservedAmount()).isEqualByComparingTo("6000.00");
    assertThat(after.availableBalance()).isEqualByComparingTo("0.00");
  }

  private Account persistAccount(String initialBalance) {
    User user = new User();
    user.setId(UuidV7.create());
    user.setName("User");
    user.setEmail(uniqueEmail("goal-user"));
    user.setPasswordHash("not-a-real-hash");
    user.setActive(true);
    user.setCreatedAt(NOW);
    user.setUpdatedAt(NOW);
    userRepository.saveAndFlush(user);

    Account account = new Account();
    account.setId(UuidV7.create());
    account.setUserId(user.getId());
    account.setName("Conta");
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal(initialBalance));
    account.setInitialBalanceLocked(false);
    account.setActive(true);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);
    return accountRepository.saveAndFlush(account);
  }

  private FinancialGoal persistGoal(Account account, FinancialGoalStatus status, String target) {
    FinancialGoal goal = new FinancialGoal();
    goal.setId(UuidV7.create());
    goal.setUserId(account.getUserId());
    goal.setAccount(account);
    goal.setName("Meta");
    goal.setTargetAmount(new BigDecimal(target));
    goal.setStatus(status);
    goal.setCreatedAt(NOW);
    goal.setUpdatedAt(NOW);
    return financialGoalRepository.saveAndFlush(goal);
  }

  private void persistContribution(FinancialGoal goal, String amount, LocalDate date) {
    GoalContribution contribution = new GoalContribution();
    contribution.setId(UuidV7.create());
    contribution.setUserId(goal.getUserId());
    contribution.setGoal(goal);
    contribution.setAmount(new BigDecimal(amount));
    contribution.setContributionDate(date);
    contribution.setCreatedAt(NOW);
    goalContributionRepository.saveAndFlush(contribution);
  }

  private void persistRedemption(FinancialGoal goal, String amount, LocalDate date) {
    GoalRedemption redemption = new GoalRedemption();
    redemption.setId(UuidV7.create());
    redemption.setUserId(goal.getUserId());
    redemption.setGoal(goal);
    redemption.setAmount(new BigDecimal(amount));
    redemption.setRedemptionDate(date);
    redemption.setCreatedAt(NOW);
    goalRedemptionRepository.saveAndFlush(redemption);
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

  private static String transferJson(UUID sourceId, UUID destinationId, String amount) {
    LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
    return """
        {
          "sourceAccountId":"%s",
          "destinationAccountId":"%s",
          "amount":%s,
          "transferDate":"%s",
          "description":"Teste"
        }
        """
        .formatted(sourceId, destinationId, amount, today);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }
}
