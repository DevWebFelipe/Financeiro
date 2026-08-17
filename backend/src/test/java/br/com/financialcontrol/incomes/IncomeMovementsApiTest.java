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
import br.com.financialcontrol.incomes.dto.IncomeMovementPageResponse;
import br.com.financialcontrol.incomes.dto.IncomeMovementResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
class IncomeMovementsApiTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private IncomeRepository incomeRepository;
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
  void shouldCreateAccrualWithoutChangingBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("accrual"));
    AccountResponse account = createAccount(token, "500.00");
    IncomeResponse income = createIncome(token, "Bonus", "100.00", "2026-08-05");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/accruals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accrualJson("20.00", "2026-08-10")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("ACCRUAL"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.amount").value(20.00))
        .andExpect(jsonPath("$.accountId").value((Object) null));

    assertThat(balance(token, account.id())).isEqualByComparingTo("500.00");
    assertThat(incomeRepository.findById(income.id()).orElseThrow().getStatus())
        .isEqualTo(IncomeStatus.EXPECTED);
  }

  @Test
  void shouldReceiptPartiallyAndFullyUpdatingStatus() throws Exception {
    String token = registerAndLogin(uniqueEmail("receipt"));
    AccountResponse account = createAccount(token, "100.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");

    receipt(token, income.id(), account.id(), "70.00", "2026-08-10");
    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(balance(token, account.id())).isEqualByComparingTo("170.00");

    receipt(token, income.id(), account.id(), "30.00", "2026-08-12");
    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.RECEIVED);
    assertThat(balance(token, account.id())).isEqualByComparingTo("200.00");
  }

  @Test
  void shouldReceiptOnTwoDifferentAccounts() throws Exception {
    String token = registerAndLogin(uniqueEmail("two-accounts"));
    AccountResponse accountA = createAccount(token, "0.00");
    AccountResponse accountB = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Split", "100.00", "2026-08-05");

    receipt(token, income.id(), accountA.id(), "40.00", "2026-08-10");
    receipt(token, income.id(), accountB.id(), "60.00", "2026-08-11");

    assertThat(balance(token, accountA.id())).isEqualByComparingTo("40.00");
    assertThat(balance(token, accountB.id())).isEqualByComparingTo("60.00");
    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.RECEIVED);
  }

  @Test
  void shouldRejectOverReceipt() throws Exception {
    String token = registerAndLogin(uniqueEmail("over"));
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptJson(account.id(), "101.00", "2026-08-10")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldRejectConcurrentReceiptsExceedingRemaining() throws Exception {
    String token = registerAndLogin(uniqueEmail("concurrency"));
    AccountResponse account = createAccount(token, "1000.00");
    IncomeResponse income = createIncome(token, "Concorrente", "100.00", "2026-08-05");
    String bodyA = receiptJson(account.id(), "70.00", "2026-08-10");
    String bodyB = receiptJson(account.id(), "50.00", "2026-08-10");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    AtomicInteger successes = new AtomicInteger();
    try {
      Future<Integer> first = pool.submit(() -> receiptStatus(token, income.id(), bodyA, start));
      Future<Integer> second = pool.submit(() -> receiptStatus(token, income.id(), bodyB, start));
      start.countDown();
      int statusA = first.get(30, TimeUnit.SECONDS);
      int statusB = second.get(30, TimeUnit.SECONDS);
      if (statusA == 201) {
        successes.incrementAndGet();
      }
      if (statusB == 201) {
        successes.incrementAndGet();
      }
      assertThat(successes.get()).isEqualTo(1);
      assertThat(List.of(statusA, statusB)).contains(400);
    } finally {
      pool.shutdownNow();
    }

    BigDecimal activeTotal =
        incomeMovementRepository.findAll().stream()
            .filter(m -> m.getIncome().getId().equals(income.id()))
            .filter(m -> m.getType() == IncomeMovementType.RECEIPT)
            .filter(m -> m.getStatus() == IncomeMovementStatus.ACTIVE)
            .map(IncomeMovement::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(activeTotal).isIn(new BigDecimal("70.00"), new BigDecimal("50.00"));
    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(balance(token, account.id()))
        .isEqualByComparingTo(new BigDecimal("1000.00").add(activeTotal));
  }

  @Test
  void shouldReopenReceivedAfterAccrual() throws Exception {
    String token = registerAndLogin(uniqueEmail("accrual-after-received"));
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "100.00", "2026-08-10");
    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.RECEIVED);

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/accruals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accrualJson("10.00", "2026-08-11")))
        .andExpect(status().isCreated());

    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.EXPECTED);
  }

  @Test
  void shouldReverseReceiptUsingMovementAccountEvenWhenInactive() throws Exception {
    String token = registerAndLogin(uniqueEmail("reverse-inactive"));
    CategoryResponse expenseCategory =
        createCategory(token, "Desp-" + UUID.randomUUID().toString().substring(0, 8), "EXPENSE");
    AccountResponse account = createAccount(token, "100.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "100.00", "2026-08-10");
    assertThat(balance(token, account.id())).isEqualByComparingTo("200.00");

    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":"%s","description":"Zera saldo","totalAmount":200.00,"expenseDate":"2026-08-10","dueDate":"2026-08-10","paymentMethod":"ACCOUNT","accountId":"%s","responsibleType":"MINE"}
                    """
                        .formatted(expenseCategory.id(), account.id())))
        .andExpect(status().isCreated())
        .andReturn();
    UUID expenseId =
        UUID.fromString(
            JsonPath.read(
                mockMvc
                    .perform(
                        get("/api/v1/expenses").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.items[0].id"));
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":200.00,"paymentDate":"2026-08-10"}
                    """
                        .formatted(account.id())))
        .andExpect(status().isOk());
    assertThat(balance(token, account.id())).isEqualByComparingTo("0.00");

    mockMvc
        .perform(
            post("/api/v1/accounts/" + account.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    UUID movementId = firstMovement(token, income.id()).id();
    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/movements/" + movementId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVERSED"));

    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.EXPECTED);
    assertThat(balance(token, account.id())).isEqualByComparingTo("-100.00");
  }

  @Test
  void shouldAllowNegativeBalanceAfterReceiptReverse() throws Exception {
    String token = registerAndLogin(uniqueEmail("negative-reverse"));
    AccountResponse account = createAccount(token, "-50.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "100.00", "2026-08-10");
    assertThat(balance(token, account.id())).isEqualByComparingTo("50.00");

    reverseFirstReceipt(token, income.id());
    assertThat(balance(token, account.id())).isEqualByComparingTo("-50.00");
  }

  @Test
  void shouldReverseAccrualAndRejectWhenRemainingWouldBeNegative() throws Exception {
    String token = registerAndLogin(uniqueEmail("reverse-accrual"));
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "80.00", "2026-08-10");
    reverseFirstReceipt(token, income.id());

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/accruals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accrualJson("30.00", "2026-08-11")))
        .andExpect(status().isCreated());

    receipt(token, income.id(), account.id(), "130.00", "2026-08-12");
    assertThat(getIncome(token, income.id()).status()).isEqualTo(IncomeStatus.RECEIVED);

    IncomeMovementResponse accrual =
        listMovements(token, income.id()).items().stream()
            .filter(m -> m.type() == IncomeMovementType.ACCRUAL)
            .filter(m -> m.status() == IncomeMovementStatus.ACTIVE)
            .findFirst()
            .orElseThrow();

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/movements/" + accrual.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldRejectConcurrentReverseOfSameMovement() throws Exception {
    String token = registerAndLogin(uniqueEmail("concurrent-reverse"));
    AccountResponse account = createAccount(token, "1000.00");
    IncomeResponse income = createIncome(token, "Concorrente reverse", "100.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "100.00", "2026-08-10");
    UUID movementId = firstMovement(token, income.id()).id();
    String path = "/api/v1/incomes/" + income.id() + "/movements/" + movementId + "/reverse";
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    AtomicInteger successes = new AtomicInteger();
    try {
      Future<Integer> first = pool.submit(() -> reverseStatus(token, path, start));
      Future<Integer> second = pool.submit(() -> reverseStatus(token, path, start));
      start.countDown();
      int statusA = first.get(30, TimeUnit.SECONDS);
      int statusB = second.get(30, TimeUnit.SECONDS);
      if (statusA == 200) {
        successes.incrementAndGet();
      }
      if (statusB == 200) {
        successes.incrementAndGet();
      }
      assertThat(successes.get()).isEqualTo(1);
      assertThat(List.of(statusA, statusB)).contains(400);
    } finally {
      pool.shutdownNow();
    }

    assertThat(
            incomeMovementRepository.findAll().stream()
                .filter(m -> m.getId().equals(movementId))
                .findFirst()
                .orElseThrow()
                .getStatus())
        .isEqualTo(IncomeMovementStatus.REVERSED);
    assertThat(balance(token, account.id())).isEqualByComparingTo("1000.00");
  }

  @Test
  void shouldRejectDuplicateReverse() throws Exception {
    String token = registerAndLogin(uniqueEmail("double-reverse"));
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Salário", "50.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "50.00", "2026-08-10");
    UUID movementId = firstMovement(token, income.id()).id();

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/movements/" + movementId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/movements/" + movementId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldApplyCancelMatrixFromD73() throws Exception {
    String token = registerAndLogin(uniqueEmail("cancel-matrix"));
    AccountResponse account = createAccount(token, "0.00");

    IncomeResponse plain = createIncome(token, "Plain", "100.00", "2026-08-05");
    cancel(token, plain.id());

    IncomeResponse withActiveReceipt =
        createIncome(token, "Active receipt", "100.00", "2026-08-05");
    receipt(token, withActiveReceipt.id(), account.id(), "30.00", "2026-08-10");
    mockMvc
        .perform(
            post("/api/v1/incomes/" + withActiveReceipt.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());

    IncomeResponse fullyReceived = createIncome(token, "Received", "100.00", "2026-08-05");
    receipt(token, fullyReceived.id(), account.id(), "100.00", "2026-08-10");
    mockMvc
        .perform(
            post("/api/v1/incomes/" + fullyReceived.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());

    IncomeResponse reversedOnly = createIncome(token, "Reversed only", "100.00", "2026-08-05");
    receipt(token, reversedOnly.id(), account.id(), "100.00", "2026-08-10");
    reverseFirstReceipt(token, reversedOnly.id());
    cancel(token, reversedOnly.id());
    assertThat(getIncome(token, reversedOnly.id()).status()).isEqualTo(IncomeStatus.CANCELLED);
  }

  @Test
  void shouldBlockMovementsOnCancelledIncome() throws Exception {
    String token = registerAndLogin(uniqueEmail("cancelled-movements"));
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Cancelada", "100.00", "2026-08-05");
    cancel(token, income.id());

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/accruals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accrualJson("10.00", "2026-08-10")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptJson(account.id(), "10.00", "2026-08-10")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectAmountUpdateAfterAnyMovement() throws Exception {
    String token = registerAndLogin(uniqueEmail("amount-lock"));
    CategoryResponse category = createIncomeCategory(token, "Salário");
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "100.00", "2026-08-10");
    reverseFirstReceipt(token, income.id());

    mockMvc
        .perform(
            put("/api/v1/incomes/" + income.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createIncomeJson(category.id(), "Salário", "90.00", "2026-08-05")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldAcceptResponsibleOnCreateAndUpdate() throws Exception {
    String token = registerAndLogin(uniqueEmail("responsible"));
    CategoryResponse category = createIncomeCategory(token, "Salário");
    MvcResult created =
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
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.responsibleType").value("MINE"))
            .andReturn();

    IncomeResponse income = read(created, IncomeResponse.class);
    mockMvc
        .perform(
            put("/api/v1/incomes/" + income.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":"%s","description":"Salário","amount":100.00,"expectedDate":"2026-08-05","responsibleType":"OTHER","responsibleName":"Joao"}
                    """
                        .formatted(category.id())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responsibleType").value("OTHER"))
        .andExpect(jsonPath("$.responsibleName").value("Joao"));
  }

  @Test
  void shouldRejectFutureMovementDateAndAllowRetroactiveDate() throws Exception {
    String token = registerAndLogin(uniqueEmail("dates"));
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-20");

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptJson(account.id(), "10.00", "2026-08-18")))
        .andExpect(status().isBadRequest());

    receipt(token, income.id(), account.id(), "10.00", "2020-01-01");
    assertThat(balance(token, account.id())).isEqualByComparingTo("10.00");
  }

  @Test
  void shouldListMovementsPaginatedAndOrdered() throws Exception {
    String token = registerAndLogin(uniqueEmail("list-movements"));
    AccountResponse account = createAccount(token, "0.00");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "40.00", "2026-08-10");
    receipt(token, income.id(), account.id(), "20.00", "2026-08-12");

    IncomeMovementPageResponse page = listMovements(token, income.id());
    assertThat(page.totalItems()).isEqualTo(2);
    assertThat(page.items()).hasSize(2);
    assertThat(page.items().getFirst().movementDate())
        .isBeforeOrEqualTo(page.items().get(1).movementDate());
  }

  @Test
  void shouldKeepBackfilledReceiptWithoutDuplicatingBalance() throws Exception {
    String token = registerAndLogin(uniqueEmail("backfill"));
    AccountResponse account = createAccount(token, "200.00");
    IncomeResponse income = createIncome(token, "Legado", "300.00", "2026-08-05");
    receipt(token, income.id(), account.id(), "300.00", "2026-08-06");

    long receiptCount =
        incomeMovementRepository.findAll().stream()
            .filter(m -> m.getIncome().getId().equals(income.id()))
            .filter(m -> m.getType() == IncomeMovementType.RECEIPT)
            .filter(m -> m.getStatus() == IncomeMovementStatus.ACTIVE)
            .count();
    assertThat(receiptCount).isEqualTo(1);
    assertThat(balance(token, account.id())).isEqualByComparingTo("500.00");
  }

  @Test
  void shouldRejectUnauthorizedUnknownPropertyAndInvalidUuid() throws Exception {
    String token = registerAndLogin(uniqueEmail("security"));
    CategoryResponse category = createIncomeCategory(token, "Salário");
    IncomeResponse income = createIncome(token, "Salário", "100.00", "2026-08-05");

    mockMvc
        .perform(get("/api/v1/incomes/" + income.id() + "/movements"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptJson(UUID.randomUUID(), "10.00", "2026-08-10")))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/v1/incomes/not-a-uuid/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptJson(UUID.randomUUID(), "10.00", "2026-08-10")))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/incomes/" + income.id() + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":10.00,"date":"2026-08-10","userId":"%s"}
                    """
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            get("/api/v1/incomes/" + UUID.randomUUID() + "/movements")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound());
  }

  private IncomeMovementPageResponse listMovements(String token, UUID incomeId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/incomes/" + incomeId + "/movements")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, IncomeMovementPageResponse.class);
  }

  private IncomeMovementResponse firstMovement(String token, UUID incomeId) throws Exception {
    return listMovements(token, incomeId).items().getFirst();
  }

  private void reverseFirstReceipt(String token, UUID incomeId) throws Exception {
    IncomeMovementResponse movement =
        listMovements(token, incomeId).items().stream()
            .filter(m -> m.type() == IncomeMovementType.RECEIPT)
            .filter(m -> m.status() == IncomeMovementStatus.ACTIVE)
            .findFirst()
            .orElseThrow();
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/movements/" + movement.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
  }

  private IncomeResponse getIncome(String token, UUID incomeId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/incomes/" + incomeId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, IncomeResponse.class);
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

  private int receiptStatus(String token, UUID incomeId, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private int reverseStatus(String token, String path, CountDownLatch start) throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(post(path).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private void cancel(String token, UUID incomeId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
  }

  private IncomeResponse createIncome(
      String token, String description, String amount, String expectedDate) throws Exception {
    CategoryResponse category =
        createIncomeCategory(token, "Cat-" + UUID.randomUUID().toString().substring(0, 8));
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createIncomeJson(category.id(), description, amount, expectedDate)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
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
    return read(result, CategoryResponse.class);
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

  private static String createIncomeJson(
      UUID categoryId, String description, String amount, String expectedDate) {
    return """
        {"categoryId":"%s","description":"%s","amount":%s,"expectedDate":"%s"}
        """
        .formatted(categoryId, description, amount, expectedDate);
  }

  private static String receiptJson(UUID accountId, String amount, String date) {
    return """
        {"accountId":"%s","amount":%s,"date":"%s"}
        """
        .formatted(accountId, amount, date);
  }

  private static String accrualJson(String amount, String date) {
    return """
        {"amount":%s,"date":"%s"}
        """
        .formatted(amount, date);
  }
}
