package br.com.financialcontrol.expenses;

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
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
class ExpenseApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private ExpenseInstallmentRepository expenseInstallmentRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldCreateAccountExpenseOpenWithoutPaymentOrBalanceChange() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("acc-create"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        createExpenseJson(
                            category.id(),
                            "Luz",
                            "150.00",
                            "2026-08-10",
                            "2026-08-20",
                            "ACCOUNT",
                            account.id(),
                            "MINE",
                            null,
                            "23793381286000000000000000000000000000000000")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.paymentMethod").value("ACCOUNT"))
            .andExpect(jsonPath("$.accountId").value(account.id().toString()))
            .andExpect(jsonPath("$.overdue").value(false))
            .andExpect(jsonPath("$.installmentId").exists())
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andReturn();

    ExpenseResponse body = read(result, ExpenseResponse.class);
    assertThat(body.totalAmount()).isEqualByComparingTo("150.00");
    assertThat(body.barcode()).isEqualTo("23793381286000000000000000000000000000000000");
    assertThat(body.id().version()).isEqualTo(7);

    Expense saved = expenseRepository.findById(body.id()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(ExpenseStatus.OPEN);
    assertThat(saved.getUserId().version()).isEqualTo(7);

    ExpenseInstallment installment = requireInstallment(saved.getId(), saved.getUserId());
    assertThat(installment.getId().version()).isEqualTo(7);
    assertThat(installment.getInstallmentNumber()).isEqualTo(1);
    assertThat(installment.getTotalInstallments()).isEqualTo(1);
    assertThat(installment.getAmount()).isEqualByComparingTo("150.00");
    assertThat(installment.getStatus()).isEqualTo(ExpenseStatus.OPEN);
    assertThat(
            paymentRepository.findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(
                saved.getId(), saved.getUserId()))
        .isEmpty();
    assertThat(balance(token, account.id())).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldCreateNoneExpenseWithNullAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("none-create"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");

    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        category.id(),
                        "Luz",
                        "80.00",
                        "2026-08-10",
                        "2026-08-20",
                        "NONE",
                        null,
                        "OTHER",
                        "Vizinho",
                        null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.paymentMethod").value("NONE"))
        .andExpect(jsonPath("$.accountId").value((Object) null))
        .andExpect(jsonPath("$.responsibleType").value("OTHER"))
        .andExpect(jsonPath("$.responsibleName").value("Vizinho"));
  }

  @Test
  void shouldRejectInvalidExpenseCreation() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("inv-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("inv-b"), "senha-segura");
    CategoryResponse expenseCategory = createExpenseCategory(tokenA, "Moradia");
    CategoryResponse incomeCategory = createCategory(tokenA, "Salário", "INCOME");
    CategoryResponse foreign = createExpenseCategory(tokenB, "Outro");
    CategoryResponse inactive = createExpenseCategory(tokenA, "Inativa");
    mockMvc
        .perform(
            post("/api/v1/categories/" + inactive.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk());
    AccountResponse account = createAccount(tokenA, "1500.00");

    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        expenseCategory.id(),
                        "Cartão",
                        "10.00",
                        "2026-08-10",
                        "2026-08-20",
                        "CREDIT_CARD",
                        null,
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        expenseCategory.id(),
                        "Luz",
                        "10.00",
                        "2026-08-10",
                        "2026-08-20",
                        "ACCOUNT",
                        null,
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        expenseCategory.id(),
                        "Luz",
                        "10.00",
                        "2026-08-10",
                        "2026-08-20",
                        "NONE",
                        account.id(),
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        incomeCategory.id(),
                        "Luz",
                        "10.00",
                        "2026-08-10",
                        "2026-08-20",
                        "NONE",
                        null,
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        inactive.id(),
                        "Luz",
                        "10.00",
                        "2026-08-10",
                        "2026-08-20",
                        "NONE",
                        null,
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        foreign.id(),
                        "Luz",
                        "10.00",
                        "2026-08-10",
                        "2026-08-20",
                        "NONE",
                        null,
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        expenseCategory.id(),
                        "Luz",
                        "10.00",
                        "2026-08-10",
                        "2026-08-20",
                        "NONE",
                        null,
                        "OTHER",
                        null,
                        null)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldPayAccountExpenseInFullAndReduceBalance() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("pay-full"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse expense =
        createExpense(token, category.id(), "Luz", "150.00", "ACCOUNT", account.id());

    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(null, "150.00", "2026-08-12")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.overdue").value(false));

    Expense saved = expenseRepository.findById(expense.id()).orElseThrow();
    ExpenseInstallment installment = requireInstallment(saved.getId(), saved.getUserId());
    assertThat(installment.getStatus()).isEqualTo(ExpenseStatus.PAID);
    List<Payment> payments =
        paymentRepository.findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(
            saved.getId(), saved.getUserId());
    assertThat(payments).hasSize(1);
    assertThat(payments.getFirst().getType()).isNull();
    mockMvc
        .perform(
            get("/api/v1/payments/" + payments.getFirst().getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value(account.id().toString()))
        .andExpect(jsonPath("$.type").doesNotExist());
    assertThat(balance(token, account.id())).isEqualByComparingTo("1350.00");
  }

  @Test
  void shouldPayPartiallyThenFullyWithMultiplePayments() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("pay-multi"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse expense =
        createExpense(token, category.id(), "Luz", "100.00", "ACCOUNT", account.id());

    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(account.id(), "40.00", "2026-08-12")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(account.id(), "60.00", "2026-08-13")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));

    assertThat(balance(token, account.id())).isEqualByComparingTo("1400.00");
  }

  @Test
  void shouldRejectInvalidPayments() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("pay-inv-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("pay-inv-b"), "senha-segura");
    CategoryResponse category = createExpenseCategory(tokenA, "Moradia");
    AccountResponse account = createAccount(tokenA, "1500.00");
    AccountResponse otherAccount = createAccount(tokenA, "900.00", "Caixa");
    AccountResponse foreign = createAccount(tokenB, "500.00");
    AccountResponse inactive = createAccount(tokenA, "200.00", "Inativa");
    AccountResponse poor = createAccount(tokenA, "10.00", "Pobre");
    ExpenseResponse expense =
        createExpense(tokenA, category.id(), "Luz", "100.00", "ACCOUNT", account.id());
    ExpenseResponse poorExpense =
        createExpense(tokenA, category.id(), "Água", "100.00", "ACCOUNT", poor.id());
    ExpenseResponse inactiveExpense =
        createExpense(tokenA, category.id(), "Net", "20.00", "ACCOUNT", inactive.id());
    ExpenseResponse noneExpense =
        createExpense(tokenA, category.id(), "Pix", "20.00", "NONE", null);
    mockMvc
        .perform(
            post("/api/v1/accounts/" + inactive.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(account.id(), "100.01", "2026-08-12")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(account.id(), "0.00", "2026-08-12")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(account.id(), "-1.00", "2026-08-12")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + poorExpense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(poor.id(), "100.00", "2026-08-12")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            post("/api/v1/expenses/" + noneExpense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(foreign.id(), "10.00", "2026-08-12")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + noneExpense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(UUID.randomUUID(), "10.00", "2026-08-12")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(otherAccount.id(), "10.00", "2026-08-12")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));
    assertThat(balance(tokenA, otherAccount.id())).isEqualByComparingTo("890.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + inactiveExpense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(inactive.id(), "10.00", "2026-08-12")))
        .andExpect(status().isBadRequest());

    pay(tokenA, expense.id(), account.id(), "90.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(account.id(), "1.00", "2026-08-12")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldPayNoneExpenseWithoutFillingExpenseAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("pay-none"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse expense = createExpense(token, category.id(), "Luz", "90.00", "NONE", null);

    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(account.id(), "90.00", "2026-08-12")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.accountId").value((Object) null))
        .andExpect(jsonPath("$.paymentMethod").value("NONE"));

    Expense saved = expenseRepository.findById(expense.id()).orElseThrow();
    assertThat(saved.getAccount()).isNull();
    List<Payment> payments =
        paymentRepository.findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(
            saved.getId(), saved.getUserId());
    mockMvc
        .perform(
            get("/api/v1/payments/" + payments.getFirst().getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.accountId").value(account.id().toString()));
    assertThat(balance(token, account.id())).isEqualByComparingTo("1410.00");
  }

  @Test
  void shouldCancelOpenExpenseAndRejectOtherStatuses() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("cancel"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse open =
        createExpense(token, category.id(), "Luz", "50.00", "ACCOUNT", account.id());
    ExpenseResponse paid =
        createExpense(token, category.id(), "Água", "50.00", "ACCOUNT", account.id());
    pay(token, paid.id(), account.id(), "50.00");
    ExpenseResponse partial =
        createExpense(token, category.id(), "Gás", "50.00", "ACCOUNT", account.id());
    pay(token, partial.id(), account.id(), "20.00");
    ExpenseResponse cancelled =
        createExpense(token, category.id(), "Net", "50.00", "ACCOUNT", account.id());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
    ExpenseResponse refunded =
        createExpense(token, category.id(), "TV", "50.00", "ACCOUNT", account.id());
    pay(token, refunded.id(), account.id(), "50.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refunded.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/expenses/" + open.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    assertThat(balance(token, account.id())).isEqualByComparingTo("1430.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + paid.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + partial.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refunded.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRefundPaidAndPartialPreservingPaymentsAndRestoringBalance() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("refund"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse paid =
        createExpense(token, category.id(), "Luz", "100.00", "ACCOUNT", account.id());
    pay(token, paid.id(), account.id(), "100.00");
    ExpenseResponse partial =
        createExpense(token, category.id(), "Água", "80.00", "ACCOUNT", account.id());
    pay(token, partial.id(), account.id(), "30.00");
    assertThat(balance(token, account.id())).isEqualByComparingTo("1370.00");

    mockMvc
        .perform(
            post("/api/v1/expenses/" + paid.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REFUNDED"));
    mockMvc
        .perform(
            post("/api/v1/expenses/" + partial.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REFUNDED"));

    assertThat(balance(token, account.id())).isEqualByComparingTo("1500.00");
    mockMvc
        .perform(
            get("/api/v1/expenses/" + paid.id() + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].type").doesNotExist());
    Expense saved = expenseRepository.findById(paid.id()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(ExpenseStatus.REFUNDED);
    assertThat(requireInstallment(saved.getId(), saved.getUserId()).getStatus())
        .isEqualTo(ExpenseStatus.REFUNDED);

    PaymentResponse payment =
        read(
            mockMvc
                .perform(
                    get("/api/v1/expenses/" + paid.id() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn(),
            PaymentResponse[].class)[0];
    mockMvc
        .perform(
            get("/api/v1/payments/" + payment.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").doesNotExist())
        .andExpect(jsonPath("$.amount").value(100.00));
    mockMvc
        .perform(
            post("/api/v1/expenses/" + paid.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectInvalidRefunds() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("refund-inv"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse open =
        createExpense(token, category.id(), "Luz", "40.00", "ACCOUNT", account.id());
    ExpenseResponse cancelled =
        createExpense(token, category.id(), "Água", "40.00", "ACCOUNT", account.id());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/expenses/" + open.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldUpdateOpenExpenseAndKeepInstallmentConsistent() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("put-open"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse expense =
        createExpense(token, category.id(), "Luz", "150.00", "ACCOUNT", account.id());

    mockMvc
        .perform(
            put("/api/v1/expenses/" + expense.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        category.id(),
                        "Energia",
                        "199.90",
                        "2026-08-11",
                        "2026-09-01",
                        "ACCOUNT",
                        account.id(),
                        "GIULIA",
                        null,
                        "123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Energia"))
        .andExpect(jsonPath("$.responsibleType").value("GIULIA"));

    Expense saved = expenseRepository.findById(expense.id()).orElseThrow();
    ExpenseInstallment installment = requireInstallment(saved.getId(), saved.getUserId());
    assertThat(saved.getTotalAmount()).isEqualByComparingTo("199.90");
    assertThat(saved.getDueDate()).isEqualTo("2026-09-01");
    assertThat(installment.getAmount()).isEqualByComparingTo("199.90");
    assertThat(installment.getDueDate()).isEqualTo("2026-09-01");
  }

  @Test
  void shouldRejectUpdateAfterPayment() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("put-paid"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse expense =
        createExpense(token, category.id(), "Luz", "100.00", "ACCOUNT", account.id());
    pay(token, expense.id(), account.id(), "40.00");

    mockMvc
        .perform(
            put("/api/v1/expenses/" + expense.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        category.id(),
                        "Hack",
                        "1.00",
                        "2026-08-10",
                        "2026-08-20",
                        "ACCOUNT",
                        account.id(),
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldExposeDerivedOverdue() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("overdue"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse future =
        createExpense(
            token, category.id(), "Futura", "10.00", "2099-01-01", "ACCOUNT", account.id());
    ExpenseResponse openPast =
        createExpense(
            token, category.id(), "Atrasada", "10.00", "2020-01-01", "ACCOUNT", account.id());
    ExpenseResponse partialPast =
        createExpense(
            token, category.id(), "Parcial", "10.00", "2020-01-01", "ACCOUNT", account.id());
    pay(token, partialPast.id(), account.id(), "4.00");
    ExpenseResponse paidPast =
        createExpense(token, category.id(), "Paga", "10.00", "2020-01-01", "ACCOUNT", account.id());
    pay(token, paidPast.id(), account.id(), "10.00");
    ExpenseResponse cancelledPast =
        createExpense(
            token, category.id(), "Cancelada", "10.00", "2020-01-01", "ACCOUNT", account.id());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelledPast.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
    ExpenseResponse refundedPast =
        createExpense(
            token, category.id(), "Estornada", "10.00", "2020-01-01", "ACCOUNT", account.id());
    pay(token, refundedPast.id(), account.id(), "10.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refundedPast.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/expenses/" + future.id()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.overdue").value(false));
    mockMvc
        .perform(
            get("/api/v1/expenses/" + openPast.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.overdue").value(true));
    mockMvc
        .perform(
            get("/api/v1/expenses/" + partialPast.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.overdue").value(true));
    mockMvc
        .perform(
            get("/api/v1/expenses/" + paidPast.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.overdue").value(false));
    mockMvc
        .perform(
            get("/api/v1/expenses/" + cancelledPast.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.overdue").value(false));
    mockMvc
        .perform(
            get("/api/v1/expenses/" + refundedPast.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.overdue").value(false));
  }

  @Test
  void shouldIsolateExpensesAndPaymentsBetweenUsers() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("iso-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("iso-b"), "senha-segura");
    CategoryResponse categoryA = createExpenseCategory(tokenA, "Moradia");
    CategoryResponse categoryB = createExpenseCategory(tokenB, "Moradia");
    AccountResponse accountA = createAccount(tokenA, "1500.00");
    AccountResponse accountB = createAccount(tokenB, "1500.00");
    ExpenseResponse expenseA =
        createExpense(tokenA, categoryA.id(), "Luz", "100.00", "ACCOUNT", accountA.id());
    pay(tokenA, expenseA.id(), accountA.id(), "100.00");
    Payment paymentA =
        paymentRepository
            .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(
                expenseA.id(), expenseRepository.findById(expenseA.id()).orElseThrow().getUserId())
            .getFirst();

    mockMvc
        .perform(
            get("/api/v1/expenses/" + expenseA.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/v1/expenses/" + expenseA.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createExpenseJson(
                        categoryB.id(),
                        "Hack",
                        "1.00",
                        "2026-08-10",
                        "2026-08-20",
                        "ACCOUNT",
                        accountB.id(),
                        "MINE",
                        null,
                        null)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseA.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(accountB.id(), "1.00", "2026-08-12")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseA.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseA.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/expenses/" + expenseA.id() + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/payments/" + paymentA.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/expenses").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(0));
  }

  @Test
  void shouldRejectUnauthenticatedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/expenses")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/v1/expenses").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldListExpensesWithPaginationAndFilters() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("list"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    createExpense(token, category.id(), "Luz", "10.00", "ACCOUNT", account.id());
    createExpense(token, category.id(), "Água", "20.00", "NONE", null);

    mockMvc
        .perform(
            get("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("page", "0")
                .param("size", "1")
                .param("status", "OPEN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalItems").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));
    mockMvc
        .perform(
            get("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("paymentMethod", "NONE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.items[0].paymentMethod").value("NONE"));
  }

  @Test
  void shouldPreventConcurrentPaymentsFromExceedingDueAmount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("race"), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, "1500.00");
    ExpenseResponse expense =
        createExpense(token, category.id(), "Luz", "100.00", "ACCOUNT", account.id());
    String payBody = payJson(account.id(), "100.00", "2026-08-12");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first = pool.submit(() -> payStatus(token, expense.id(), payBody, start));
      Future<Integer> second = pool.submit(() -> payStatus(token, expense.id(), payBody, start));
      start.countDown();
      int statusA = first.get(30, TimeUnit.SECONDS);
      int statusB = second.get(30, TimeUnit.SECONDS);
      assertThat(List.of(statusA, statusB)).contains(200);
      assertThat(statusA == 200 ? statusB : statusA).isEqualTo(400);
    } finally {
      pool.shutdownNow();
    }
    Expense saved = expenseRepository.findById(expense.id()).orElseThrow();
    List<Payment> payments =
        paymentRepository.findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(
            saved.getId(), saved.getUserId());
    BigDecimal total =
        payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total).isEqualByComparingTo("100.00");
    assertThat(saved.getStatus()).isEqualTo(ExpenseStatus.PAID);
    assertThat(balance(token, account.id())).isEqualByComparingTo("1400.00");
  }

  private int payStatus(String token, UUID expenseId, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private ExpenseInstallment requireInstallment(UUID expenseId, UUID userId) {
    return expenseInstallmentRepository
        .findByExpense_IdAndUserIdAndInstallmentNumber(expenseId, userId, 1)
        .orElseThrow();
  }

  private ExpenseResponse createExpense(
      String token,
      UUID categoryId,
      String description,
      String amount,
      String paymentMethod,
      UUID accountId)
      throws Exception {
    return createExpense(
        token, categoryId, description, amount, "2026-08-20", paymentMethod, accountId);
  }

  private ExpenseResponse createExpense(
      String token,
      UUID categoryId,
      String description,
      String amount,
      String dueDate,
      String paymentMethod,
      UUID accountId)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        createExpenseJson(
                            categoryId,
                            description,
                            amount,
                            "2026-08-10",
                            dueDate,
                            paymentMethod,
                            accountId,
                            "MINE",
                            null,
                            null)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private void pay(String token, UUID expenseId, UUID accountId, String amount) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(accountId, amount, "2026-08-12")))
        .andExpect(status().isOk());
  }

  private CategoryResponse createExpenseCategory(String token, String name) throws Exception {
    return createCategory(token, name, "EXPENSE");
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
    return createAccount(token, initialBalance, "Nubank");
  }

  private AccountResponse createAccount(String token, String initialBalance, String name)
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

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private static String createExpenseJson(
      UUID categoryId,
      String description,
      String amount,
      String expenseDate,
      String dueDate,
      String paymentMethod,
      UUID accountId,
      String responsibleType,
      String responsibleName,
      String barcode) {
    String accountField = accountId == null ? "" : ",\"accountId\":\"%s\"".formatted(accountId);
    String nameField =
        responsibleName == null ? "" : ",\"responsibleName\":\"%s\"".formatted(responsibleName);
    String barcodeField = barcode == null ? "" : ",\"barcode\":\"%s\"".formatted(barcode);
    return """
        {"categoryId":"%s","description":"%s","totalAmount":%s,"expenseDate":"%s","dueDate":"%s","paymentMethod":"%s","responsibleType":"%s"%s%s%s}
        """
        .formatted(
            categoryId,
            description,
            amount,
            expenseDate,
            dueDate,
            paymentMethod,
            responsibleType,
            accountField,
            nameField,
            barcodeField);
  }

  private static String payJson(UUID accountId, String amount, String paymentDate) {
    String accountField = accountId == null ? "" : "\"accountId\":\"%s\",".formatted(accountId);
    return """
        {%s"amount":%s,"paymentDate":"%s"}
        """
        .formatted(accountField, amount, paymentDate);
  }
}
