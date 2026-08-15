package br.com.financialcontrol.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
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
class ExpenseAdjustmentApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private ExpenseInstallmentRepository installmentRepository;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldCreateDiscountAndSurchargeWith201ShapeAndNoSideEffectsOnAmountsOrBalance()
      throws Exception {
    Fixture fx = bootstrap("create-ok");
    BigDecimal balanceBefore = balance(fx.token(), fx.accountId());
    BigDecimal totalBefore =
        expenseRepository.findById(fx.expenseId()).orElseThrow().getTotalAmount();
    BigDecimal installmentBefore =
        installmentRepository
            .findByIdAndExpense_IdAndUserId(fx.installmentId(), fx.expenseId(), fx.userId())
            .orElseThrow()
            .getAmount();

    MvcResult discountResult =
        mockMvc
            .perform(
                post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustmentJson("DISCOUNT", "10.00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.expenseId").value(fx.expenseId().toString()))
            .andExpect(jsonPath("$.installmentId").value(fx.installmentId().toString()))
            .andExpect(jsonPath("$.type").value("DISCOUNT"))
            .andExpect(jsonPath("$.amount").value(10.00))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.reversedAt").doesNotExist())
            .andReturn();

    AdjustmentResponse discount = read(discountResult, AdjustmentResponse.class);
    assertThat(discount.id().version()).isEqualTo(7);
    assertThat(discount.status()).isEqualTo(AdjustmentStatus.ACTIVE);

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("SURCHARGE", "5.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("SURCHARGE"))
        .andExpect(jsonPath("$.amount").value(5.00))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(balance(fx.token(), fx.accountId())).isEqualByComparingTo(balanceBefore);
    assertThat(expenseRepository.findById(fx.expenseId()).orElseThrow().getTotalAmount())
        .isEqualByComparingTo(totalBefore);
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(fx.installmentId(), fx.expenseId(), fx.userId())
                .orElseThrow()
                .getAmount())
        .isEqualByComparingTo(installmentBefore);
  }

  @Test
  void shouldRejectInvalidCreateAdjustmentRequests() throws Exception {
    Fixture fx = bootstrap("create-inv");

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("DISCOUNT", "0")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("DISCOUNT", "-1.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("INTEREST", "10.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"DISCOUNT","amount":10.00,"reason":"antecipacao"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldEnforceOwnershipOnCreateAndRejectCrossExpenseInstallment() throws Exception {
    Fixture owner = bootstrap("own-a");
    Fixture other = bootstrap("own-b");

    mockMvc
        .perform(
            post(adjustmentsPath(owner.expenseId(), owner.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("DISCOUNT", "1.00")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            post(adjustmentsPath(owner.expenseId(), other.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("DISCOUNT", "1.00")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            post(adjustmentsPath(other.expenseId(), owner.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("DISCOUNT", "1.00")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldAllowAdjustmentOnOpenAndPartiallyPaidInstallmentAndRejectInvalidObligation()
      throws Exception {
    Fixture fx = bootstrap("partial");

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("DISCOUNT", "20.00")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/expenses/" + fx.expenseId() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(fx.accountId(), "40.00")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/expenses/" + fx.expenseId() + "/installments/" + fx.installmentId())
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("SURCHARGE", "5.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(
            post(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adjustmentJson("DISCOUNT", "1000.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldListActiveAndReversedAsDirectArrayInCreatedAtAscOrder() throws Exception {
    Fixture fx = bootstrap("list");

    AdjustmentResponse first =
        createAdjustment(fx.token(), fx.expenseId(), fx.installmentId(), "DISCOUNT", "10.00");
    AdjustmentResponse second =
        createAdjustment(fx.token(), fx.expenseId(), fx.installmentId(), "SURCHARGE", "5.00");

    mockMvc
        .perform(
            post(reversePath(fx.expenseId(), fx.installmentId(), second.id()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());

    MvcResult listResult =
        mockMvc
            .perform(
                get(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").doesNotExist())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(first.id().toString()))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$[1].id").value(second.id().toString()))
            .andExpect(jsonPath("$[1].status").value("REVERSED"))
            .andReturn();

    List<AdjustmentResponse> items =
        jsonMapper
            .readerForListOf(AdjustmentResponse.class)
            .readValue(listResult.getResponse().getContentAsString());
    assertThat(items).hasSize(2);
    assertThat(items.get(0).createdAt()).isBeforeOrEqualTo(items.get(1).createdAt());
    if (items.get(0).createdAt().equals(items.get(1).createdAt())) {
      assertThat(items.get(0).id().toString()).isLessThan(items.get(1).id().toString());
    }
  }

  @Test
  void shouldKeepHistoryReadableAfterRefundAndCancel() throws Exception {
    Fixture refundFx = bootstrap("hist-refund");
    AdjustmentResponse refundAdj =
        createAdjustment(
            refundFx.token(), refundFx.expenseId(), refundFx.installmentId(), "DISCOUNT", "5.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refundFx.expenseId() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(refundFx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(refundFx.accountId(), "95.00")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refundFx.expenseId() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(refundFx.token())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get(adjustmentsPath(refundFx.expenseId(), refundFx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(refundFx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(refundAdj.id().toString()))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));

    Fixture cancelFx = bootstrap("hist-cancel");
    AdjustmentResponse cancelAdj =
        createAdjustment(
            cancelFx.token(), cancelFx.expenseId(), cancelFx.installmentId(), "SURCHARGE", "3.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelFx.expenseId() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(cancelFx.token())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get(adjustmentsPath(cancelFx.expenseId(), cancelFx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(cancelFx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(cancelAdj.id().toString()));
  }

  @Test
  void shouldEnforceOwnershipOnListAdjustments() throws Exception {
    Fixture owner = bootstrap("list-own");
    Fixture other = bootstrap("list-other");
    createAdjustment(owner.token(), owner.expenseId(), owner.installmentId(), "DISCOUNT", "2.00");

    mockMvc
        .perform(
            get(adjustmentsPath(owner.expenseId(), owner.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldReverseActiveAdjustmentPreserveHistoryAndRejectInvalidReverse() throws Exception {
    Fixture fx = bootstrap("reverse");
    AdjustmentResponse created =
        createAdjustment(fx.token(), fx.expenseId(), fx.installmentId(), "DISCOUNT", "15.00");

    MvcResult reverseResult =
        mockMvc
            .perform(
                post(reversePath(fx.expenseId(), fx.installmentId(), created.id()))
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(created.id().toString()))
            .andExpect(jsonPath("$.type").value("DISCOUNT"))
            .andExpect(jsonPath("$.amount").value(15.00))
            .andExpect(jsonPath("$.status").value("REVERSED"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.reversedAt").doesNotExist())
            .andReturn();

    AdjustmentResponse reversed = read(reverseResult, AdjustmentResponse.class);
    assertThat(reversed.amount()).isEqualByComparingTo("15.00");
    assertThat(reversed.type()).isEqualTo(AdjustmentType.DISCOUNT);
    // same persisted instant (DB may truncate sub-microseconds vs in-memory Instant)
    assertThat(reversed.createdAt().getEpochSecond())
        .isEqualTo(created.createdAt().getEpochSecond());
    assertThat(Math.abs(reversed.createdAt().getNano() - created.createdAt().getNano()))
        .isLessThan(1_000);

    mockMvc
        .perform(
            get(adjustmentsPath(fx.expenseId(), fx.installmentId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("REVERSED"))
        .andExpect(jsonPath("$[0].amount").value(15.00))
        .andExpect(jsonPath("$[0].type").value("DISCOUNT"));

    mockMvc
        .perform(
            post(reversePath(fx.expenseId(), fx.installmentId(), created.id()))
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldRejectReverseWithInvalidOwnershipOrWrongInstallment() throws Exception {
    Fixture owner = bootstrap("rev-own");
    Fixture other = bootstrap("rev-other");
    AdjustmentResponse adjustment =
        createAdjustment(
            owner.token(), owner.expenseId(), owner.installmentId(), "SURCHARGE", "4.00");

    mockMvc
        .perform(
            post(reversePath(owner.expenseId(), owner.installmentId(), adjustment.id()))
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            post(reversePath(owner.expenseId(), other.installmentId(), adjustment.id()))
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            post(reversePath(other.expenseId(), other.installmentId(), adjustment.id()))
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldRejectReverseAfterCancelAndRefund() throws Exception {
    Fixture cancelFx = bootstrap("rev-cancel");
    AdjustmentResponse cancelAdj =
        createAdjustment(
            cancelFx.token(), cancelFx.expenseId(), cancelFx.installmentId(), "DISCOUNT", "2.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelFx.expenseId() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(cancelFx.token())))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post(reversePath(cancelFx.expenseId(), cancelFx.installmentId(), cancelAdj.id()))
                .header(HttpHeaders.AUTHORIZATION, bearer(cancelFx.token())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

    Fixture refundFx = bootstrap("rev-refund");
    AdjustmentResponse refundAdj =
        createAdjustment(
            refundFx.token(), refundFx.expenseId(), refundFx.installmentId(), "DISCOUNT", "2.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refundFx.expenseId() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(refundFx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(refundFx.accountId(), "98.00")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refundFx.expenseId() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(refundFx.token())))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post(reversePath(refundFx.expenseId(), refundFx.installmentId(), refundAdj.id()))
                .header(HttpHeaders.AUTHORIZATION, bearer(refundFx.token())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  private AdjustmentResponse createAdjustment(
      String token, UUID expenseId, UUID installmentId, String type, String amount)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(adjustmentsPath(expenseId, installmentId))
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustmentJson(type, amount)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AdjustmentResponse.class);
  }

  private Fixture bootstrap(String prefix) throws Exception {
    String token = registerAndLogin("User", uniqueEmail(prefix), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Cat-" + prefix);
    AccountResponse account = createAccount(token, "5000.00");
    UUID userId =
        UUID.fromString(
            JsonPath.read(
                mockMvc
                    .perform(
                        get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.id"));
    ExpenseResponse expense = createExpense(token, category.id(), account.id(), "100.00");
    return new Fixture(token, userId, account.id(), expense.id(), expense.installmentId());
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
                        {"categoryId":"%s","description":"Adj","totalAmount":%s,"expenseDate":"2026-08-10","dueDate":"2026-08-20","paymentMethod":"ACCOUNT","accountId":"%s","responsibleType":"MINE"}
                        """
                            .formatted(categoryId, amount, accountId)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private CategoryResponse createExpenseCategory(String token, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"EXPENSE"}
                        """
                            .formatted(name)))
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
                        {"name":"Conta","type":"BANK_ACCOUNT","initialBalance":%s}
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

  private static String adjustmentsPath(UUID expenseId, UUID installmentId) {
    return "/api/v1/expenses/" + expenseId + "/installments/" + installmentId + "/adjustments";
  }

  private static String reversePath(UUID expenseId, UUID installmentId, UUID adjustmentId) {
    return adjustmentsPath(expenseId, installmentId) + "/" + adjustmentId + "/reverse";
  }

  private static String adjustmentJson(String type, String amount) {
    return """
        {"type":"%s","amount":%s}
        """
        .formatted(type, amount);
  }

  private static String payJson(UUID accountId, String amount) {
    return """
        {"accountId":"%s","amount":%s,"paymentDate":"2026-08-12"}
        """
        .formatted(accountId, amount);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private record Fixture(
      String token, UUID userId, UUID accountId, UUID expenseId, UUID installmentId) {}
}
