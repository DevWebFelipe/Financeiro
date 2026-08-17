package br.com.financialcontrol.financial_goals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.financial_goals.dto.CreateGoalContributionResponse;
import br.com.financialcontrol.financial_goals.dto.CreateGoalRedemptionResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalResponse;
import com.jayway.jsonpath.JsonPath;
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
class FinancialGoalApiTest {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldCreateGoalOnBankAndCashAccounts() throws Exception {
    String token = registerAndLogin(uniqueEmail("create-goal"));
    AccountResponse bank = createAccount(token, "Banco", "BANK_ACCOUNT", "1000.00");
    AccountResponse cash = createAccount(token, "Carteira", "CASH", "200.00");

    FinancialGoalResponse bankGoal =
        createGoal(token, bank.id(), "Viagem", "5000.00", "2026-12-20");
    assertThat(bankGoal.status()).isEqualTo(FinancialGoalStatus.ACTIVE);
    assertThat(bankGoal.accountId()).isEqualTo(bank.id());
    assertThat(bankGoal.currentAmount()).isEqualByComparingTo("0.00");
    assertThat(bankGoal.progressPercent()).isEqualByComparingTo("0.00");
    assertThat(bankGoal.targetAmount()).isEqualByComparingTo("5000.00");

    FinancialGoalResponse cashGoal = createGoal(token, cash.id(), "Reserva", "100.00", null);
    assertThat(cashGoal.accountId()).isEqualTo(cash.id());
    assertThat(cashGoal.targetDate()).isNull();
  }

  @Test
  void shouldAllowDuplicateGoalNamesForSameUser() throws Exception {
    String token = registerAndLogin(uniqueEmail("dup-name"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "100.00");
    createGoal(token, account.id(), "Mesmo", "10.00", null);
    mockMvc
        .perform(
            post("/api/v1/financial-goals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalJson(account.id(), "Mesmo", "20.00", null)))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldRejectInvalidTargetAmountAndUnknownAccount() throws Exception {
    String token = registerAndLogin(uniqueEmail("invalid-goal"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "100.00");

    mockMvc
        .perform(
            post("/api/v1/financial-goals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalJson(account.id(), "Meta", "0.00", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post("/api/v1/financial-goals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalJson(UUID.randomUUID(), "Meta", "10.00", null)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldRejectForeignAndInactiveAccountsWhenCreatingGoal() throws Exception {
    String tokenA = registerAndLogin(uniqueEmail("owner"));
    String tokenB = registerAndLogin(uniqueEmail("other"));
    AccountResponse accountA = createAccount(tokenA, "A", "BANK_ACCOUNT", "50.00");
    AccountResponse inactive = createAccount(tokenB, "Inativa", "BANK_ACCOUNT", "0.00");
    mockMvc
        .perform(
            post("/api/v1/accounts/" + inactive.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/financial-goals")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalJson(accountA.id(), "Cruzada", "10.00", null)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/v1/financial-goals")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalJson(inactive.id(), "Inativa", "10.00", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldEditActiveGoalIncludingTargetAmountUpAndDown() throws Exception {
    String token = registerAndLogin(uniqueEmail("edit-goal"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "10000.00");
    FinancialGoalResponse created = createGoal(token, account.id(), "Notebook", "5000.00", null);
    contribute(token, created.id(), "2500.00");

    FinancialGoalResponse raised =
        updateGoal(token, created.id(), "Notebook 2", "10000.00", "2027-01-15");
    assertThat(raised.name()).isEqualTo("Notebook 2");
    assertThat(raised.targetAmount()).isEqualByComparingTo("10000.00");
    assertThat(raised.currentAmount()).isEqualByComparingTo("2500.00");
    assertThat(raised.progressPercent()).isEqualByComparingTo("25.00");
    assertThat(raised.accountId()).isEqualTo(account.id());

    FinancialGoalResponse lowered = updateGoal(token, created.id(), "Notebook 2", "2000.00", null);
    assertThat(lowered.targetAmount()).isEqualByComparingTo("2000.00");
    assertThat(lowered.currentAmount()).isEqualByComparingTo("2500.00");
    assertThat(lowered.progressPercent()).isEqualByComparingTo("125.00");
  }

  @Test
  void shouldRejectEditOnCompletedAndCancelledGoals() throws Exception {
    String token = registerAndLogin(uniqueEmail("edit-terminal"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "100.00");
    FinancialGoalResponse completed = createGoal(token, account.id(), "Done", "50.00", null);
    complete(token, completed.id());
    assertBusinessRule(putGoal(token, completed.id(), "X", "60.00"));

    FinancialGoalResponse cancelled = createGoal(token, account.id(), "Cancel", "50.00", null);
    cancel(token, cancelled.id());
    assertBusinessRule(putGoal(token, cancelled.id(), "X", "60.00"));
  }

  @Test
  void shouldContributeWithoutChangingTotalBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("contribute"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "10000.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Chile", "15000.00", null);

    CreateGoalContributionResponse created = contribute(token, goal.id(), "5000.00");
    assertThat(created.contribution().amount()).isEqualByComparingTo("5000.00");
    assertThat(created.goal().currentAmount()).isEqualByComparingTo("5000.00");
    assertThat(created.goal().progressPercent()).isEqualByComparingTo("33.33");
    assertThat(created.goal().status()).isEqualTo(FinancialGoalStatus.ACTIVE);

    AccountBalanceResponse balance = readBalance(token, account.id());
    assertThat(balance.totalBalance()).isEqualByComparingTo("10000.00");
    assertThat(balance.reservedAmount()).isEqualByComparingTo("5000.00");
    assertThat(balance.availableBalance()).isEqualByComparingTo("5000.00");
  }

  @Test
  void shouldRejectInvalidContributions() throws Exception {
    String token = registerAndLogin(uniqueEmail("bad-contrib"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "100.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Meta", "500.00", null);

    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(contributionJson("0.00", today())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(contributionJson("150.00", today())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value(FinancialGoalService.INSUFFICIENT_BALANCE));

    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(contributionJson("10.00", today().plusDays(1))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

    complete(token, goal.id());
    assertBusinessRule(postContribution(token, goal.id(), "10.00"));

    FinancialGoalResponse cancelled = createGoal(token, account.id(), "Outra", "50.00", null);
    cancel(token, cancelled.id());
    assertBusinessRule(postContribution(token, cancelled.id(), "10.00"));
  }

  @Test
  void shouldNotCompleteAutomaticallyWhenProgressReachesOrExceedsHundred() throws Exception {
    String token = registerAndLogin(uniqueEmail("no-auto"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "1000.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Alvo", "100.00", null);
    FinancialGoalResponse after = contribute(token, goal.id(), "150.00").goal();
    assertThat(after.status()).isEqualTo(FinancialGoalStatus.ACTIVE);
    assertThat(after.progressPercent()).isEqualByComparingTo("150.00");
  }

  @Test
  void shouldRedeemFromActiveAndCompletedWithoutChangingTotalOrStatus() throws Exception {
    String token = registerAndLogin(uniqueEmail("redeem"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "1000.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Reserva", "800.00", null);
    contribute(token, goal.id(), "300.00");

    CreateGoalRedemptionResponse partial = redeem(token, goal.id(), "100.00");
    assertThat(partial.goal().currentAmount()).isEqualByComparingTo("200.00");
    assertThat(partial.goal().status()).isEqualTo(FinancialGoalStatus.ACTIVE);
    assertThat(readBalance(token, account.id()).totalBalance()).isEqualByComparingTo("1000.00");
    assertThat(readBalance(token, account.id()).availableBalance()).isEqualByComparingTo("800.00");

    complete(token, goal.id());
    CreateGoalRedemptionResponse remaining = redeem(token, goal.id(), "200.00");
    assertThat(remaining.goal().status()).isEqualTo(FinancialGoalStatus.COMPLETED);
    assertThat(remaining.goal().currentAmount()).isEqualByComparingTo("0.00");
    assertThat(remaining.goal().progressPercent()).isEqualByComparingTo("0.00");
    assertThat(readBalance(token, account.id()).reservedAmount()).isEqualByComparingTo("0.00");
    assertThat(readBalance(token, account.id()).availableBalance()).isEqualByComparingTo("1000.00");
  }

  @Test
  void shouldRejectRedemptionAboveCurrentCancelledAndChosenAccount() throws Exception {
    String token = registerAndLogin(uniqueEmail("bad-redeem"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "500.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Meta", "200.00", null);
    contribute(token, goal.id(), "50.00");

    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/redemptions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redemptionJson("50.01", today())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value(FinancialGoalService.REDEMPTION_EXCEEDS_CURRENT));

    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/redemptions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":10.00,"redemptionDate":"%s","accountId":"%s"}
                    """
                        .formatted(today(), account.id())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    FinancialGoalResponse empty = createGoal(token, account.id(), "Vazia", "10.00", null);
    cancel(token, empty.id());
    assertBusinessRule(postRedemption(token, empty.id(), "1.00"));
  }

  @Test
  void shouldCompleteManuallyBelowAndAboveTarget() throws Exception {
    String token = registerAndLogin(uniqueEmail("complete"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "10000.00");
    FinancialGoalResponse below = createGoal(token, account.id(), "Promo", "5000.00", null);
    contribute(token, below.id(), "3500.00");
    FinancialGoalResponse completedBelow = complete(token, below.id());
    assertThat(completedBelow.status()).isEqualTo(FinancialGoalStatus.COMPLETED);
    assertThat(completedBelow.currentAmount()).isEqualByComparingTo("3500.00");

    FinancialGoalResponse above = createGoal(token, account.id(), "Cheia", "100.00", null);
    contribute(token, above.id(), "120.00");
    FinancialGoalResponse completedAbove = complete(token, above.id());
    assertThat(completedAbove.status()).isEqualTo(FinancialGoalStatus.COMPLETED);
    assertThat(completedAbove.progressPercent()).isEqualByComparingTo("120.00");

    assertBusinessRule(postAction(token, below.id(), "complete"));
    FinancialGoalResponse cancelled = createGoal(token, account.id(), "No", "10.00", null);
    cancel(token, cancelled.id());
    assertBusinessRule(postAction(token, cancelled.id(), "complete"));
  }

  @Test
  void shouldCancelOnlyActiveGoalWithZeroCurrentAmount() throws Exception {
    String token = registerAndLogin(uniqueEmail("cancel"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "200.00");
    FinancialGoalResponse empty = createGoal(token, account.id(), "Vazia", "50.00", null);
    FinancialGoalResponse cancelled = cancel(token, empty.id());
    assertThat(cancelled.status()).isEqualTo(FinancialGoalStatus.CANCELLED);
    assertThat(readBalance(token, account.id()).totalBalance()).isEqualByComparingTo("200.00");

    FinancialGoalResponse reserved = createGoal(token, account.id(), "Com saldo", "50.00", null);
    contribute(token, reserved.id(), "20.00");
    assertBusinessRule(postAction(token, reserved.id(), "cancel"));

    complete(token, reserved.id());
    assertBusinessRule(postAction(token, reserved.id(), "cancel"));
    assertBusinessRule(postAction(token, empty.id(), "cancel"));
  }

  @Test
  void shouldListGoalsContributionsAndRedemptionsAndIsolateUsers() throws Exception {
    String tokenA = registerAndLogin(uniqueEmail("list-a"));
    String tokenB = registerAndLogin(uniqueEmail("list-b"));
    AccountResponse accountA = createAccount(tokenA, "A", "BANK_ACCOUNT", "500.00");
    FinancialGoalResponse active = createGoal(tokenA, accountA.id(), "Ativa", "100.00", null);
    FinancialGoalResponse done = createGoal(tokenA, accountA.id(), "Feita", "100.00", null);
    complete(tokenA, done.id());
    contribute(tokenA, active.id(), "30.00");
    redeem(tokenA, active.id(), "10.00");

    mockMvc
        .perform(get("/api/v1/financial-goals").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalItems").value(2));

    mockMvc
        .perform(
            get("/api/v1/financial-goals?status=COMPLETED")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(done.id().toString()));

    mockMvc
        .perform(
            get("/api/v1/financial-goals/" + active.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].amount").value(30.00));

    mockMvc
        .perform(
            get("/api/v1/financial-goals/" + active.id() + "/redemptions")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    mockMvc
        .perform(
            get("/api/v1/financial-goals/" + active.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/financial-goals/" + active.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/financial-goals").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void shouldRejectUnauthenticatedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/financial-goals")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldCompleteActiveGoalWithZeroCurrentAmount() throws Exception {
    String token = registerAndLogin(uniqueEmail("complete-zero"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "100.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Vazia", "500.00", null);

    FinancialGoalResponse completed = complete(token, goal.id());
    assertThat(completed.status()).isEqualTo(FinancialGoalStatus.COMPLETED);
    assertThat(completed.currentAmount()).isEqualByComparingTo("0.00");
    assertThat(completed.progressPercent()).isEqualByComparingTo("0.00");
    assertThat(readBalance(token, account.id()).reservedAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldContributeUsingEntireAvailableBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("exact-available"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "100.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Meta", "500.00", null);

    CreateGoalContributionResponse created = contribute(token, goal.id(), "100.00");
    assertThat(created.goal().currentAmount()).isEqualByComparingTo("100.00");

    AccountBalanceResponse balance = readBalance(token, account.id());
    assertThat(balance.totalBalance()).isEqualByComparingTo("100.00");
    assertThat(balance.reservedAmount()).isEqualByComparingTo("100.00");
    assertThat(balance.availableBalance()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldRejectFutureRedemptionDateWithoutChangingCurrentAmount() throws Exception {
    String token = registerAndLogin(uniqueEmail("future-redeem"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "200.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Meta", "200.00", null);
    contribute(token, goal.id(), "50.00");

    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/redemptions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redemptionJson("10.00", today().plusDays(1))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value(FinancialGoalService.FUTURE_REDEMPTION_DATE));

    mockMvc
        .perform(
            get("/api/v1/financial-goals/" + goal.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentAmount").value(50.00));
    mockMvc
        .perform(
            get("/api/v1/financial-goals/" + goal.id() + "/redemptions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void shouldRejectForeignMutationsWithNotFound() throws Exception {
    String tokenA = registerAndLogin(uniqueEmail("owner-mut"));
    String tokenB = registerAndLogin(uniqueEmail("other-mut"));
    AccountResponse accountA = createAccount(tokenA, "A", "BANK_ACCOUNT", "300.00");
    FinancialGoalResponse goal = createGoal(tokenA, accountA.id(), "Privada", "200.00", null);
    contribute(tokenA, goal.id(), "40.00");

    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(contributionJson("10.00", today())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/redemptions")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redemptionJson("10.00", today())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/complete")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            get("/api/v1/financial-goals/" + goal.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.currentAmount").value(40.00));
    AccountBalanceResponse balance = readBalance(tokenA, accountA.id());
    assertThat(balance.reservedAmount()).isEqualByComparingTo("40.00");
    assertThat(balance.totalBalance()).isEqualByComparingTo("300.00");
  }

  @Test
  void shouldRejectInvalidPagination() throws Exception {
    String token = registerAndLogin(uniqueEmail("page-invalid"));

    mockMvc
        .perform(
            get("/api/v1/financial-goals?page=-1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value(FinancialGoalService.INVALID_PAGE));

    mockMvc
        .perform(
            get("/api/v1/financial-goals?size=0").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value(FinancialGoalService.INVALID_PAGE_SIZE));
  }

  @Test
  void shouldRedeemPartiallyThenFullyFromCompletedGoal() throws Exception {
    String token = registerAndLogin(uniqueEmail("completed-redeem"));
    AccountResponse account = createAccount(token, "Banco", "BANK_ACCOUNT", "1000.00");
    FinancialGoalResponse goal = createGoal(token, account.id(), "Viagem", "800.00", null);
    contribute(token, goal.id(), "300.00");
    complete(token, goal.id());

    CreateGoalRedemptionResponse partial = redeem(token, goal.id(), "100.00");
    assertThat(partial.goal().status()).isEqualTo(FinancialGoalStatus.COMPLETED);
    assertThat(partial.goal().currentAmount()).isEqualByComparingTo("200.00");
    AccountBalanceResponse afterPartial = readBalance(token, account.id());
    assertThat(afterPartial.totalBalance()).isEqualByComparingTo("1000.00");
    assertThat(afterPartial.reservedAmount()).isEqualByComparingTo("200.00");
    assertThat(afterPartial.availableBalance()).isEqualByComparingTo("800.00");

    CreateGoalRedemptionResponse remaining = redeem(token, goal.id(), "200.00");
    assertThat(remaining.goal().status()).isEqualTo(FinancialGoalStatus.COMPLETED);
    assertThat(remaining.goal().currentAmount()).isEqualByComparingTo("0.00");
    assertThat(remaining.goal().progressPercent()).isEqualByComparingTo("0.00");
    AccountBalanceResponse afterFull = readBalance(token, account.id());
    assertThat(afterFull.totalBalance()).isEqualByComparingTo("1000.00");
    assertThat(afterFull.reservedAmount()).isEqualByComparingTo("0.00");
    assertThat(afterFull.availableBalance()).isEqualByComparingTo("1000.00");
  }

  private FinancialGoalResponse createGoal(
      String token, UUID accountId, String name, String target, String targetDate)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/financial-goals")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(goalJson(accountId, name, target, targetDate)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, FinancialGoalResponse.class);
  }

  private FinancialGoalResponse updateGoal(
      String token, UUID goalId, String name, String target, String targetDate) throws Exception {
    MvcResult result =
        putGoal(token, goalId, name, target, targetDate).andExpect(status().isOk()).andReturn();
    return read(result, FinancialGoalResponse.class);
  }

  private org.springframework.test.web.servlet.ResultActions putGoal(
      String token, UUID goalId, String name, String target) throws Exception {
    return putGoal(token, goalId, name, target, null);
  }

  private org.springframework.test.web.servlet.ResultActions putGoal(
      String token, UUID goalId, String name, String target, String targetDate) throws Exception {
    String datePart = targetDate == null ? "null" : "\"" + targetDate + "\"";
    return mockMvc.perform(
        put("/api/v1/financial-goals/" + goalId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"name":"%s","description":null,"targetAmount":%s,"targetDate":%s}
                """
                    .formatted(name, target, datePart)));
  }

  private CreateGoalContributionResponse contribute(String token, UUID goalId, String amount)
      throws Exception {
    MvcResult result =
        postContribution(token, goalId, amount).andExpect(status().isCreated()).andReturn();
    return read(result, CreateGoalContributionResponse.class);
  }

  private org.springframework.test.web.servlet.ResultActions postContribution(
      String token, UUID goalId, String amount) throws Exception {
    return mockMvc.perform(
        post("/api/v1/financial-goals/" + goalId + "/contributions")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(contributionJson(amount, today())));
  }

  private CreateGoalRedemptionResponse redeem(String token, UUID goalId, String amount)
      throws Exception {
    MvcResult result =
        postRedemption(token, goalId, amount).andExpect(status().isCreated()).andReturn();
    return read(result, CreateGoalRedemptionResponse.class);
  }

  private org.springframework.test.web.servlet.ResultActions postRedemption(
      String token, UUID goalId, String amount) throws Exception {
    return mockMvc.perform(
        post("/api/v1/financial-goals/" + goalId + "/redemptions")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(redemptionJson(amount, today())));
  }

  private FinancialGoalResponse complete(String token, UUID goalId) throws Exception {
    MvcResult result = postAction(token, goalId, "complete").andExpect(status().isOk()).andReturn();
    return read(result, FinancialGoalResponse.class);
  }

  private FinancialGoalResponse cancel(String token, UUID goalId) throws Exception {
    MvcResult result = postAction(token, goalId, "cancel").andExpect(status().isOk()).andReturn();
    return read(result, FinancialGoalResponse.class);
  }

  private org.springframework.test.web.servlet.ResultActions postAction(
      String token, UUID goalId, String action) throws Exception {
    return mockMvc.perform(
        post("/api/v1/financial-goals/" + goalId + "/" + action)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  private void assertBusinessRule(org.springframework.test.web.servlet.ResultActions actions)
      throws Exception {
    actions
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
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

  private AccountBalanceResponse readBalance(String token, UUID accountId) throws Exception {
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

  private static String goalJson(UUID accountId, String name, String target, String targetDate) {
    String datePart = targetDate == null ? "null" : "\"" + targetDate + "\"";
    return """
        {"accountId":"%s","name":"%s","description":"Obs","targetAmount":%s,"targetDate":%s}
        """
        .formatted(accountId, name, target, datePart);
  }

  private static String contributionJson(String amount, LocalDate date) {
    return """
        {"amount":%s,"contributionDate":"%s","notes":"Aporte"}
        """
        .formatted(amount, date);
  }

  private static String redemptionJson(String amount, LocalDate date) {
    return """
        {"amount":%s,"redemptionDate":"%s"}
        """
        .formatted(amount, date);
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
