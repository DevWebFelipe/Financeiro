package br.com.financialcontrol.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.CategoryType;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementResponse;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.incomes.dto.IncomeMovementResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.reports.dto.CardReportItemResponse;
import br.com.financialcontrol.reports.dto.CardReportPurchaseResponse;
import br.com.financialcontrol.reports.dto.CardReportResponse;
import br.com.financialcontrol.reports.dto.CashFlowItemResponse;
import br.com.financialcontrol.reports.dto.CashFlowResponse;
import br.com.financialcontrol.reports.dto.CategoryReportItemResponse;
import br.com.financialcontrol.reports.dto.CategoryReportResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportItemResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportResponse;
import br.com.financialcontrol.reports.dto.IncomeReportItemResponse;
import br.com.financialcontrol.reports.dto.IncomeReportResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportPurchaseResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportItemResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
class ReportsApiTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private CreditCardInvoiceService invoiceService;
  @Autowired private CreditCardInvoiceRepository invoiceRepository;

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-08-17T15:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Test
  void shouldRejectUnauthenticatedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/reports/expenses")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/incomes")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/categories")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/responsibles")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/cards")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/reports/invoices/" + UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/cash-flow")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/expenses/pdf")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/incomes/pdf")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/categories/pdf")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/responsibles/pdf")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/cards/pdf")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/reports/cash-flow/pdf")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/reports/invoices/" + UUID.randomUUID() + "/pdf"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectUnknownAndInvalidExpenseReportParams() throws Exception {
    Fixture fx = bootstrap("invalid-params");
    String token = fx.token();
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("foo", "bar"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-31")
                .param("endDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2025-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("sort", "totalAmount"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("direction", "up"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldReturnEmptyExpenseReportForCurrentMonthByDefault() throws Exception {
    Fixture fx = bootstrap("empty");
    ExpenseReportResponse report = expenses(fx.token());
    assertThat(report.period().startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(report.period().endDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(report.items()).isEmpty();
    assertThat(report.page()).isZero();
    assertThat(report.size()).isEqualTo(20);
    assertThat(report.totalItems()).isZero();
    assertThat(report.totalPages()).isZero();
    assertThat(report.summary().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(report.summary().periodObligation()).isEqualByComparingTo("0.00");
    assertThat(report.summary().periodPaid()).isEqualByComparingTo("0.00");
    assertThat(report.summary().periodRemaining()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldReportAccountExpenseByDueDateNotExpenseDate() throws Exception {
    Fixture fx = bootstrap("account-due");
    ExpenseResponse expense =
        createExpense(
            fx,
            "Aluguel",
            "1500.00",
            "2026-07-15",
            "2026-08-10",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    pay(fx.token(), expense.id(), fx.accountId(), "500.00");

    ExpenseReportResponse july =
        expenses(fx.token(), "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items()).isEmpty();
    assertThat(july.summary().periodOriginal()).isEqualByComparingTo("0.00");

    ExpenseReportResponse august =
        expenses(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.totalItems()).isEqualTo(1);
    ExpenseReportItemResponse item = august.items().getFirst();
    assertThat(item.id()).isEqualTo(expense.id());
    assertThat(item.paymentMethod().name()).isEqualTo("ACCOUNT");
    assertThat(item.origin()).isEqualTo(ExpenseReportOrigin.PURCHASE);
    assertThat(item.periodOriginal()).isEqualByComparingTo("1500.00");
    assertThat(item.periodDiscount()).isEqualByComparingTo("0.00");
    assertThat(item.periodSurcharge()).isEqualByComparingTo("0.00");
    assertThat(item.periodObligation()).isEqualByComparingTo("1500.00");
    assertThat(item.periodPaid()).isEqualByComparingTo("500.00");
    assertThat(item.periodRemaining()).isEqualByComparingTo("1000.00");
    assertThat(item.periodPaid().add(item.periodRemaining()))
        .isEqualByComparingTo(item.periodObligation());
    assertThat(item.installments()).hasSize(1);
    assertThat(item.installments().getFirst().dueDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    assertThat(august.summary().periodOriginal()).isEqualByComparingTo("1500.00");
    assertThat(august.summary().periodPaid()).isEqualByComparingTo("500.00");
    assertThat(august.summary().periodRemaining()).isEqualByComparingTo("1000.00");

    mockMvc
        .perform(
            get("/api/v1/reports/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].totalAmount").doesNotExist())
        .andExpect(jsonPath("$.items[0].original").doesNotExist())
        .andExpect(jsonPath("$.items[0].periodAccrued").doesNotExist());
  }

  @Test
  void shouldReportNoneExpenseWithoutAccount() throws Exception {
    Fixture fx = bootstrap("none");
    createExpense(
        fx, "Luz", "80.00", "2026-08-01", "2026-08-20", "NONE", null, "OTHER", "Vizinho", 1);
    ExpenseReportResponse report =
        expenses(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.items()).hasSize(1);
    assertThat(report.items().getFirst().paymentMethod().name()).isEqualTo("NONE");
    assertThat(report.items().getFirst().accountId()).isNull();
    assertThat(report.items().getFirst().periodOriginal()).isEqualByComparingTo("80.00");
    assertThat(report.items().getFirst().periodPaid()).isEqualByComparingTo("0.00");
    assertThat(report.items().getFirst().periodRemaining()).isEqualByComparingTo("80.00");
    assertThat(report.summary().periodObligation()).isEqualByComparingTo("80.00");
  }

  @Test
  void shouldKeepInstallmentIdentityForCreditCardPeriodPaid() throws Exception {
    Fixture fx = bootstrap("card-identity", "5000.00", "200.00");
    ExpenseResponse expense = createCardExpense(fx, "200.00", "2026-08-05", 1);
    ExpenseInstallmentResponse installment = listExpenseInstallments(fx, expense.id())[0];
    ExpenseReportResponse report =
        expenses(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.items()).hasSize(1);
    ExpenseReportItemResponse item = report.items().getFirst();
    assertThat(item.paymentMethod().name()).isEqualTo("CREDIT_CARD");
    assertThat(item.creditCardId()).isEqualTo(fx.cardId());
    assertThat(item.periodOriginal()).isEqualByComparingTo("200.00");
    assertThat(item.periodPaid()).isEqualByComparingTo("0.00");
    assertThat(item.periodRemaining()).isEqualByComparingTo("200.00");
    assertThat(item.periodPaid().add(item.periodRemaining()))
        .isEqualByComparingTo(item.periodObligation());
    assertThat(item.installments()).hasSize(1);
    assertThat(item.installments().getFirst().id()).isEqualTo(installment.id());
    assertThat(
            item.installments().getFirst().paid().add(item.installments().getFirst().remaining()))
        .isEqualByComparingTo(item.installments().getFirst().obligation());
  }

  @Test
  void shouldNotDuplicateExpenseWhenInstallmentsSpanThePeriod() throws Exception {
    Fixture fx = bootstrap("n-gt-1");
    ExpenseResponse expense =
        createExpense(
            fx,
            "Notebook",
            "1000.00",
            "2026-08-01",
            "2026-08-20",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            3);
    ExpenseInstallmentResponse[] installments = listExpenseInstallments(fx, expense.id());
    assertThat(installments).hasSize(3);
    payInstallment(fx.token(), expense.id(), installments[0].id(), fx.accountId(), "333.34");

    ExpenseReportResponse august =
        expenses(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.items()).hasSize(1);
    assertThat(august.items().getFirst().installments()).hasSize(1);
    assertThat(august.items().getFirst().periodOriginal()).isEqualByComparingTo("333.34");
    assertThat(august.items().getFirst().periodPaid()).isEqualByComparingTo("333.34");
    assertThat(august.items().getFirst().periodRemaining()).isEqualByComparingTo("0.00");
    assertThat(august.summary().periodOriginal()).isEqualByComparingTo("333.34");
    assertThat(august.summary().periodOriginal()).isNotEqualByComparingTo("1000.00");

    ExpenseReportResponse all =
        expenses(fx.token(), "startDate", "2026-08-01", "endDate", "2026-10-31");
    assertThat(all.items()).hasSize(1);
    assertThat(all.items().getFirst().installments()).hasSize(3);
    assertThat(all.items().getFirst().periodOriginal()).isEqualByComparingTo("1000.00");
    assertThat(all.items().getFirst().periodPaid()).isEqualByComparingTo("333.34");
    assertThat(all.items().getFirst().periodRemaining()).isEqualByComparingTo("666.66");
    assertThat(all.summary().periodOriginal()).isEqualByComparingTo("1000.00");
  }

  @Test
  void shouldKeepCancelledAndRefundedInItemsAndExcludeFromSummary() throws Exception {
    Fixture fx = bootstrap("status-excl");
    ExpenseResponse cancelled =
        createExpense(
            fx,
            "Cancelada",
            "40.00",
            "2026-08-01",
            "2026-08-10",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    ExpenseResponse refunded =
        createExpense(
            fx,
            "Estornada",
            "60.00",
            "2026-08-01",
            "2026-08-11",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    ExpenseResponse open =
        createExpense(
            fx,
            "Aberta",
            "25.00",
            "2026-08-01",
            "2026-08-12",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    pay(fx.token(), refunded.id(), fx.accountId(), "60.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refunded.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());

    ExpenseReportResponse report =
        expenses(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.items()).hasSize(3);
    assertThat(report.items())
        .extracting(ExpenseReportItemResponse::status)
        .extracting(Enum::name)
        .containsExactlyInAnyOrder("CANCELLED", "REFUNDED", "OPEN");
    assertThat(report.summary().periodOriginal()).isEqualByComparingTo("25.00");
    assertThat(report.summary().periodRemaining()).isEqualByComparingTo("25.00");
  }

  @Test
  void shouldExcludeOnlySettledAgreementInstallmentFromSummary() throws Exception {
    Fixture fx = bootstrap("agreement-partial", "20000.00", "5000.00");
    ExpenseResponse original = createCardExpense(fx, "12000.00", "2026-07-05", 12);
    ExpenseInstallmentResponse[] originalInstallments = listExpenseInstallments(fx, original.id());
    assertThat(originalInstallments).hasSize(12);
    UUID julyInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(fx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);

    AgreementResponse agreement = createAgreement(fx, julyInvoiceId, "0.00", 10, "120.00");
    assertThat(getInvoice(fx.token(), julyInvoiceId).status())
        .isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);

    ExpenseReportResponse july =
        expenses(fx.token(), "startDate", "2026-07-01", "endDate", "2026-07-31");
    ExpenseReportItemResponse julyOriginal =
        july.items().stream()
            .filter(item -> item.id().equals(original.id()))
            .findFirst()
            .orElseThrow();
    assertThat(julyOriginal.origin()).isEqualTo(ExpenseReportOrigin.PURCHASE);
    assertThat(julyOriginal.installments()).hasSize(1);
    assertThat(julyOriginal.periodOriginal()).isEqualByComparingTo("1000.00");
    assertThat(july.items()).noneMatch(item -> item.id().equals(agreement.expenseId()));
    assertThat(july.summary().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(july.summary().periodObligation()).isEqualByComparingTo("0.00");

    LocalDate secondDue = originalInstallments[1].dueDate();
    ExpenseReportResponse secondMonth =
        expenses(
            fx.token(),
            "startDate",
            secondDue.withDayOfMonth(1).toString(),
            "endDate",
            secondDue.withDayOfMonth(secondDue.lengthOfMonth()).toString());
    ExpenseReportItemResponse remainingOriginal =
        secondMonth.items().stream()
            .filter(item -> item.id().equals(original.id()))
            .findFirst()
            .orElseThrow();
    assertThat(remainingOriginal.installments()).isNotEmpty();
    assertThat(remainingOriginal.periodOriginal()).isGreaterThan(BigDecimal.ZERO);
    assertThat(secondMonth.summary().periodOriginal()).isGreaterThan(BigDecimal.ZERO);
    assertThat(secondMonth.items())
        .anyMatch(
            item ->
                item.id().equals(agreement.expenseId())
                    && item.origin() == ExpenseReportOrigin.AGREEMENT);
  }

  @Test
  void shouldIsolateUsersAndTreatForeignFiltersAsEmpty() throws Exception {
    Fixture owner = bootstrap("iso-a");
    Fixture other = bootstrap("iso-b");
    createExpense(
        owner,
        "Privada",
        "90.00",
        "2026-08-01",
        "2026-08-15",
        "ACCOUNT",
        owner.accountId(),
        "MINE",
        null,
        1);

    ExpenseReportResponse otherView =
        expenses(other.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(otherView.items()).isEmpty();
    assertThat(otherView.summary().periodOriginal()).isEqualByComparingTo("0.00");

    ExpenseReportResponse foreignCategory =
        expenses(
            owner.token(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "categoryId",
            other.categoryId().toString());
    assertThat(foreignCategory.items()).isEmpty();
    assertThat(foreignCategory.summary().periodOriginal()).isEqualByComparingTo("0.00");

    ExpenseReportResponse foreignAccount =
        expenses(
            owner.token(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "accountId",
            other.accountId().toString());
    assertThat(foreignAccount.items()).isEmpty();
  }

  @Test
  void shouldApplyDocumentedFiltersAndKeepSummaryIndependentOfPage() throws Exception {
    Fixture fx = bootstrap("filters");
    createExpense(
        fx, "A", "100.00", "2026-08-01", "2026-08-10", "ACCOUNT", fx.accountId(), "MINE", null, 1);
    createExpense(
        fx,
        "B",
        "200.00",
        "2026-08-01",
        "2026-08-11",
        "ACCOUNT",
        fx.accountId(),
        "GIULIA",
        null,
        1);
    createExpense(fx, "C", "50.00", "2026-08-01", "2026-08-12", "NONE", null, "MINE", null, 1);

    ExpenseReportResponse mine =
        expenses(
            fx.token(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "responsibleType",
            "MINE",
            "paymentMethod",
            "ACCOUNT");
    assertThat(mine.items()).hasSize(1);
    assertThat(mine.summary().periodOriginal()).isEqualByComparingTo("100.00");

    ExpenseReportResponse page0 =
        expenses(
            fx.token(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "page",
            "0",
            "size",
            "1",
            "sort",
            "periodObligation",
            "direction",
            "desc");
    assertThat(page0.items()).hasSize(1);
    assertThat(page0.totalItems()).isEqualTo(3);
    assertThat(page0.totalPages()).isEqualTo(3);
    assertThat(page0.summary().periodOriginal()).isEqualByComparingTo("350.00");
    assertThat(page0.items().getFirst().periodObligation()).isEqualByComparingTo("200.00");
  }

  @Test
  void shouldRejectUnknownAndInvalidIncomeReportParams() throws Exception {
    Fixture fx = bootstrap("income-invalid");
    String token = fx.token();
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "PAID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("foo", "bar"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("startDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("startDate", "2026-08-31")
                .param("endDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("startDate", "2025-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldReportExpectedIncomesWithOfficialQuartetAndExcludeOutOfPeriod() throws Exception {
    Fixture fx = bootstrap("income-expected");
    UUID incomeCategoryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse inPeriod =
        createIncome(fx.token(), incomeCategoryId, "Salário", "5400.00", "2026-08-05");
    IncomeResponse outOfPeriod =
        createIncome(fx.token(), incomeCategoryId, "Fora", "900.00", "2026-07-10");
    createAccrual(fx.token(), inPeriod.id(), "100.00", "2026-08-06");

    IncomeReportResponse august =
        incomes(
            fx.token(), "dateType", "EXPECTED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.dateType()).isEqualTo(ReportDateType.EXPECTED);
    assertThat(august.items()).hasSize(1);
    IncomeReportItemResponse item = august.items().getFirst();
    assertThat(item.id()).isEqualTo(inPeriod.id());
    assertThat(item.amount()).isEqualByComparingTo("5400.00");
    assertThat(item.accruedAmount()).isEqualByComparingTo("100.00");
    assertThat(item.receivedAmount()).isEqualByComparingTo("0.00");
    assertThat(item.remainingAmount()).isEqualByComparingTo("5500.00");
    assertThat(item.periodReceivedAmount()).isNull();
    assertThat(item.amount().add(item.accruedAmount()).subtract(item.receivedAmount()))
        .isEqualByComparingTo(item.remainingAmount());
    assertThat(august.summary().amount()).isEqualByComparingTo("5400.00");
    assertThat(august.summary().accruedAmount()).isEqualByComparingTo("100.00");
    assertThat(august.summary().receivedAmount()).isEqualByComparingTo("0.00");
    assertThat(august.summary().remainingAmount()).isEqualByComparingTo("5500.00");
    assertThat(august.summary().periodReceivedAmount()).isNull();

    mockMvc
        .perform(
            get("/api/v1/reports/incomes")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("dateType", "EXPECTED")
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].periodReceivedAmount").doesNotExist())
        .andExpect(jsonPath("$.items[0].periodRemainingAmount").doesNotExist())
        .andExpect(jsonPath("$.summary.periodReceivedAmount").doesNotExist());

    IncomeReportResponse july =
        incomes(
            fx.token(), "dateType", "EXPECTED", "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items())
        .extracting(IncomeReportItemResponse::id)
        .containsExactly(outOfPeriod.id());
  }

  @Test
  void shouldKeepExpectedAccountIdAsActiveReceiptExistenceAndAllowEmpty() throws Exception {
    Fixture fx = bootstrap("income-d78a");
    AccountResponse otherAccount = createAccount(fx.token(), "1000.00", "Caixa");
    UUID incomeCategoryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse withoutReceipt =
        createIncome(fx.token(), incomeCategoryId, "Sem recibo", "800.00", "2026-08-10");
    IncomeResponse withReceipt =
        createIncome(fx.token(), incomeCategoryId, "Com recibo", "1200.00", "2026-08-12");
    receive(fx.token(), withReceipt.id(), fx.accountId(), "200.00", "2026-07-15");

    IncomeReportResponse empty =
        incomes(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "accountId",
            otherAccount.id().toString());
    assertThat(empty.items()).isEmpty();
    assertThat(empty.summary().amount()).isEqualByComparingTo("0.00");

    IncomeReportResponse filtered =
        incomes(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "accountId",
            fx.accountId().toString());
    assertThat(filtered.items()).hasSize(1);
    assertThat(filtered.items().getFirst().id()).isEqualTo(withReceipt.id());
    assertThat(filtered.items().getFirst().id()).isNotEqualTo(withoutReceipt.id());
    assertThat(filtered.items().getFirst().receivedAmount()).isEqualByComparingTo("200.00");
    assertThat(filtered.items().getFirst().remainingAmount()).isEqualByComparingTo("1000.00");
    assertThat(filtered.items().getFirst().periodReceivedAmount()).isNull();
  }

  @Test
  void shouldReportReceivedPeriodAmountWithoutReusingGlobalReceivedAmount() throws Exception {
    Fixture fx = bootstrap("income-received");
    UUID incomeCategoryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse income =
        createIncome(fx.token(), incomeCategoryId, "Salário", "5400.00", "2026-08-05");
    receive(fx.token(), income.id(), fx.accountId(), "2000.00", "2026-07-10");
    IncomeMovementResponse augustReceipt =
        receive(fx.token(), income.id(), fx.accountId(), "3400.00", "2026-08-12");

    IncomeReportResponse july =
        incomes(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items()).hasSize(1);
    assertThat(july.items().getFirst().periodReceivedAmount()).isEqualByComparingTo("2000.00");
    assertThat(july.items().getFirst().receivedAmount()).isEqualByComparingTo("5400.00");
    assertThat(july.summary().periodReceivedAmount()).isEqualByComparingTo("2000.00");
    assertThat(july.summary().amount()).isNull();

    IncomeReportResponse august =
        incomes(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.items()).hasSize(1);
    assertThat(august.items().getFirst().periodReceivedAmount()).isEqualByComparingTo("3400.00");
    assertThat(august.items().getFirst().receivedAmount()).isEqualByComparingTo("5400.00");
    assertThat(august.items().getFirst().remainingAmount()).isEqualByComparingTo("0.00");
    assertThat(august.summary().periodReceivedAmount()).isEqualByComparingTo("3400.00");

    IncomeReportResponse june =
        incomes(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-06-01", "endDate", "2026-06-30");
    assertThat(june.items()).isEmpty();
    assertThat(june.summary().periodReceivedAmount()).isEqualByComparingTo("0.00");

    reverseMovement(fx.token(), income.id(), augustReceipt.id());
    IncomeReportResponse afterReverse =
        incomes(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(afterReverse.items()).isEmpty();
    IncomeReportResponse julyAfterReverse =
        incomes(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(julyAfterReverse.items().getFirst().periodReceivedAmount())
        .isEqualByComparingTo("2000.00");
    assertThat(julyAfterReverse.items().getFirst().receivedAmount())
        .isEqualByComparingTo("2000.00");
  }

  @Test
  void shouldSumMultipleActiveReceiptsInTheSamePeriod() throws Exception {
    Fixture fx = bootstrap("income-multi-receipt");
    UUID incomeCategoryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse income =
        createIncome(fx.token(), incomeCategoryId, "Salário", "3000.00", "2026-08-01");
    receive(fx.token(), income.id(), fx.accountId(), "1000.00", "2026-08-05");
    receive(fx.token(), income.id(), fx.accountId(), "500.00", "2026-08-16");
    IncomeReportResponse report =
        incomes(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.items()).hasSize(1);
    assertThat(report.items().getFirst().periodReceivedAmount()).isEqualByComparingTo("1500.00");
    assertThat(report.items().getFirst().receivedAmount()).isEqualByComparingTo("1500.00");
    assertThat(report.items().getFirst().remainingAmount()).isEqualByComparingTo("1500.00");
    assertThat(report.summary().periodReceivedAmount()).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldExcludeCancelledFromIncomeSummaryAndIsolateUsers() throws Exception {
    Fixture owner = bootstrap("income-iso-a");
    Fixture other = bootstrap("income-iso-b");
    UUID ownerCategory = createIncomeCategory(owner.token(), "Salário").id();
    createIncome(
        other.token(),
        createIncomeCategory(other.token(), "Outro").id(),
        "Alheia",
        "999.00",
        "2026-08-08");
    IncomeResponse cancelled =
        createIncome(owner.token(), ownerCategory, "Cancelada", "400.00", "2026-08-09");
    IncomeResponse open =
        createIncome(owner.token(), ownerCategory, "Aberta", "250.00", "2026-08-10");
    cancelIncome(owner.token(), cancelled.id());

    IncomeReportResponse ownerReport =
        incomes(
            owner.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(ownerReport.items()).hasSize(2);
    assertThat(ownerReport.summary().amount()).isEqualByComparingTo("250.00");

    IncomeReportResponse otherView =
        incomes(
            other.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "accountId",
            owner.accountId().toString());
    assertThat(otherView.items()).isEmpty();

    IncomeReportResponse foreignAccount =
        incomes(
            owner.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "accountId",
            other.accountId().toString());
    assertThat(foreignAccount.items()).isEmpty();
    assertThat(open.id()).isNotNull();
  }

  @Test
  void shouldKeepIncomeSummaryIndependentOfPage() throws Exception {
    Fixture fx = bootstrap("income-page");
    UUID incomeCategoryId = createIncomeCategory(fx.token(), "Salário").id();
    createIncome(fx.token(), incomeCategoryId, "A", "100.00", "2026-08-01");
    createIncome(fx.token(), incomeCategoryId, "B", "200.00", "2026-08-02");
    createIncome(fx.token(), incomeCategoryId, "C", "50.00", "2026-08-03");
    IncomeReportResponse page0 =
        incomes(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "page",
            "0",
            "size",
            "1",
            "sort",
            "amount",
            "direction",
            "desc");
    assertThat(page0.items()).hasSize(1);
    assertThat(page0.totalItems()).isEqualTo(3);
    assertThat(page0.totalPages()).isEqualTo(3);
    assertThat(page0.items().getFirst().amount()).isEqualByComparingTo("200.00");
    assertThat(page0.summary().amount()).isEqualByComparingTo("350.00");
    assertThat(page0.summary().remainingAmount()).isEqualByComparingTo("350.00");
  }

  @Test
  void shouldRejectUnknownAndInvalidCategoryReportParams() throws Exception {
    Fixture fx = bootstrap("category-invalid");
    String token = fx.token();
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "PAID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("nature", "EXPENSE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("foo", "bar"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("startDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("startDate", "2026-08-31")
                .param("endDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("startDate", "2025-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldReturnEmptyCategoryReportWithoutInventingUnusedCategories() throws Exception {
    Fixture fx = bootstrap("category-empty");
    CategoryReportResponse report =
        categories(
            fx.token(), "dateType", "EXPECTED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.dateType()).isEqualTo(ReportDateType.EXPECTED);
    assertThat(report.period().startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(report.period().endDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(report.items()).isEmpty();
    assertThat(report.totalItems()).isZero();
    assertThat(report.totalPages()).isZero();
    assertThat(report.summary().expense().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(report.summary().expense().periodObligation()).isEqualByComparingTo("0.00");
    assertThat(report.summary().income().amount()).isEqualByComparingTo("0.00");
    assertThat(report.summary().income().remainingAmount()).isEqualByComparingTo("0.00");
    assertThat(report.summary().income().periodReceivedAmount()).isNull();
  }

  @Test
  void shouldReportExpenseAndIncomeCategoriesTogetherWithoutMixingMetrics() throws Exception {
    Fixture fx = bootstrap("category-mixed");
    UUID foodId = createExpenseCategory(fx.token(), "Alimentação").id();
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    UUID sameNameIncomeId = createIncomeCategory(fx.token(), "Moradia").id();
    ExpenseResponse rent =
        createExpense(
            fx,
            "Aluguel",
            "1500.00",
            "2026-07-15",
            "2026-08-10",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    pay(fx.token(), rent.id(), fx.accountId(), "500.00");
    createExpense(
        fx,
        foodId,
        "Mercado",
        "80.00",
        "2026-08-01",
        "2026-08-20",
        "ACCOUNT",
        fx.accountId(),
        "MINE",
        null,
        1);
    createExpense(
        fx,
        foodId,
        "Padaria",
        "20.00",
        "2026-08-01",
        "2026-08-21",
        "ACCOUNT",
        fx.accountId(),
        "MINE",
        null,
        1);
    IncomeResponse salary = createIncome(fx.token(), salaryId, "Salário", "5400.00", "2026-08-05");
    createAccrual(fx.token(), salary.id(), "100.00", "2026-08-06");
    createIncome(fx.token(), sameNameIncomeId, "Aluguel recebido", "900.00", "2026-08-12");

    CategoryReportResponse july =
        categories(
            fx.token(), "dateType", "EXPECTED", "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items()).isEmpty();

    CategoryReportResponse august =
        categories(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "name");
    assertThat(august.items()).hasSize(4);
    assertThat(august.items())
        .extracting(CategoryReportItemResponse::name)
        .containsExactly("Alimentação", "Moradia", "Moradia", "Salário");

    CategoryReportItemResponse food = itemByCategory(august, foodId);
    assertThat(food.type()).isEqualTo(CategoryType.EXPENSE);
    assertThat(food.periodOriginal()).isEqualByComparingTo("100.00");
    assertThat(food.periodObligation()).isEqualByComparingTo("100.00");
    assertThat(food.amount()).isNull();
    assertThat(food.periodReceivedAmount()).isNull();

    CategoryReportItemResponse housingExpense = itemByCategory(august, fx.categoryId());
    assertThat(housingExpense.type()).isEqualTo(CategoryType.EXPENSE);
    assertThat(housingExpense.name()).isEqualTo("Moradia");
    assertThat(housingExpense.periodOriginal()).isEqualByComparingTo("1500.00");
    assertThat(housingExpense.periodPaid()).isEqualByComparingTo("500.00");
    assertThat(housingExpense.periodRemaining()).isEqualByComparingTo("1000.00");
    assertThat(housingExpense.periodPaid().add(housingExpense.periodRemaining()))
        .isEqualByComparingTo(housingExpense.periodObligation());
    assertThat(housingExpense.amount()).isNull();

    CategoryReportItemResponse housingIncome = itemByCategory(august, sameNameIncomeId);
    assertThat(housingIncome.type()).isEqualTo(CategoryType.INCOME);
    assertThat(housingIncome.name()).isEqualTo("Moradia");
    assertThat(housingIncome.amount()).isEqualByComparingTo("900.00");
    assertThat(housingIncome.remainingAmount()).isEqualByComparingTo("900.00");
    assertThat(housingIncome.periodOriginal()).isNull();
    assertThat(housingIncome.periodReceivedAmount()).isNull();

    CategoryReportItemResponse salaryItem = itemByCategory(august, salaryId);
    assertThat(salaryItem.type()).isEqualTo(CategoryType.INCOME);
    assertThat(salaryItem.amount()).isEqualByComparingTo("5400.00");
    assertThat(salaryItem.accruedAmount()).isEqualByComparingTo("100.00");
    assertThat(salaryItem.receivedAmount()).isEqualByComparingTo("0.00");
    assertThat(salaryItem.remainingAmount()).isEqualByComparingTo("5500.00");
    assertThat(salaryItem.periodReceivedAmount()).isNull();

    assertThat(august.summary().expense().periodOriginal()).isEqualByComparingTo("1600.00");
    assertThat(august.summary().expense().periodPaid()).isEqualByComparingTo("500.00");
    assertThat(august.summary().expense().periodRemaining()).isEqualByComparingTo("1100.00");
    assertThat(august.summary().income().amount()).isEqualByComparingTo("6300.00");
    assertThat(august.summary().income().accruedAmount()).isEqualByComparingTo("100.00");
    assertThat(august.summary().income().remainingAmount()).isEqualByComparingTo("6400.00");
    assertThat(august.summary().income().periodReceivedAmount()).isNull();

    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("dateType", "EXPECTED")
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].periodRemainingAmount").doesNotExist())
        .andExpect(jsonPath("$.summary.income.periodReceivedAmount").doesNotExist())
        .andExpect(jsonPath("$.summary.income.periodRemainingAmount").doesNotExist());
  }

  @Test
  void shouldReportReceivedCategoryIncomeWithPeriodReceivedAmount() throws Exception {
    Fixture fx = bootstrap("category-received");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    createExpense(
        fx,
        "Aluguel",
        "1500.00",
        "2026-08-01",
        "2026-08-10",
        "ACCOUNT",
        fx.accountId(),
        "MINE",
        null,
        1);
    IncomeResponse salary = createIncome(fx.token(), salaryId, "Salário", "5400.00", "2026-07-05");
    receive(fx.token(), salary.id(), fx.accountId(), "2000.00", "2026-07-10");
    receive(fx.token(), salary.id(), fx.accountId(), "3400.00", "2026-08-12");

    CategoryReportResponse august =
        categories(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.dateType()).isEqualTo(ReportDateType.RECEIVED);
    assertThat(august.items()).hasSize(2);
    CategoryReportItemResponse expenseItem = itemByCategory(august, fx.categoryId());
    assertThat(expenseItem.type()).isEqualTo(CategoryType.EXPENSE);
    assertThat(expenseItem.periodOriginal()).isEqualByComparingTo("1500.00");
    CategoryReportItemResponse incomeItem = itemByCategory(august, salaryId);
    assertThat(incomeItem.type()).isEqualTo(CategoryType.INCOME);
    assertThat(incomeItem.amount()).isEqualByComparingTo("5400.00");
    assertThat(incomeItem.receivedAmount()).isEqualByComparingTo("5400.00");
    assertThat(incomeItem.remainingAmount()).isEqualByComparingTo("0.00");
    assertThat(incomeItem.periodReceivedAmount()).isEqualByComparingTo("3400.00");
    assertThat(incomeItem.periodOriginal()).isNull();
    assertThat(august.summary().expense().periodOriginal()).isEqualByComparingTo("1500.00");
    assertThat(august.summary().income().periodReceivedAmount()).isEqualByComparingTo("3400.00");
    assertThat(august.summary().income().amount()).isNull();

    mockMvc
        .perform(
            get("/api/v1/reports/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("dateType", "RECEIVED")
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.income.periodReceivedAmount").value(3400.00))
        .andExpect(jsonPath("$.summary.income.amount").doesNotExist())
        .andExpect(jsonPath("$.summary.income.periodRemainingAmount").doesNotExist())
        .andExpect(jsonPath("$.items[?(@.type=='INCOME')].periodRemainingAmount").isEmpty());
  }

  @Test
  void shouldKeepCreditCardCategoryIdentityAndExcludeCancelledFromSummary() throws Exception {
    Fixture fx = bootstrap("category-card", "5000.00", "200.00");
    ExpenseResponse cardExpense = createCardExpense(fx, "200.00", "2026-08-05", 1);
    ExpenseInstallmentResponse installment = listExpenseInstallments(fx, cardExpense.id())[0];
    ExpenseResponse cancelled =
        createExpense(
            fx,
            "Cancelada",
            "40.00",
            "2026-08-01",
            "2026-08-10",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    ExpenseResponse refunded =
        createExpense(
            fx,
            "Estornada",
            "60.00",
            "2026-08-01",
            "2026-08-11",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    pay(fx.token(), refunded.id(), fx.accountId(), "60.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refunded.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());

    CategoryReportResponse report =
        categories(
            fx.token(), "dateType", "EXPECTED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.items()).hasSize(1);
    CategoryReportItemResponse item = report.items().getFirst();
    assertThat(item.categoryId()).isEqualTo(fx.categoryId());
    assertThat(item.periodOriginal()).isEqualByComparingTo("300.00");
    assertThat(item.periodPaid().add(item.periodRemaining()))
        .isEqualByComparingTo(item.periodObligation());
    assertThat(report.summary().expense().periodOriginal()).isEqualByComparingTo("200.00");
    assertThat(report.summary().expense().periodPaid())
        .isEqualByComparingTo(
            report
                .summary()
                .expense()
                .periodObligation()
                .subtract(report.summary().expense().periodRemaining()));
    assertThat(installment.id()).isNotNull();
  }

  @Test
  void shouldExcludeOnlySettledAgreementInstallmentFromCategorySummary() throws Exception {
    Fixture fx = bootstrap("category-agreement", "20000.00", "5000.00");
    ExpenseResponse original = createCardExpense(fx, "12000.00", "2026-07-05", 12);
    ExpenseInstallmentResponse[] originalInstallments = listExpenseInstallments(fx, original.id());
    UUID julyInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(fx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);
    AgreementResponse agreement = createAgreement(fx, julyInvoiceId, "0.00", 10, "120.00");
    assertThat(getInvoice(fx.token(), julyInvoiceId).status())
        .isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);

    CategoryReportResponse july =
        categories(
            fx.token(), "dateType", "EXPECTED", "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items()).hasSize(1);
    assertThat(july.items().getFirst().periodOriginal()).isEqualByComparingTo("1000.00");
    assertThat(july.summary().expense().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(july.summary().expense().periodObligation()).isEqualByComparingTo("0.00");

    LocalDate secondDue = originalInstallments[1].dueDate();
    CategoryReportResponse secondMonth =
        categories(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            secondDue.withDayOfMonth(1).toString(),
            "endDate",
            secondDue.withDayOfMonth(secondDue.lengthOfMonth()).toString());
    assertThat(secondMonth.items()).hasSize(1);
    assertThat(secondMonth.items().getFirst().periodOriginal()).isGreaterThan(BigDecimal.ZERO);
    assertThat(secondMonth.summary().expense().periodOriginal()).isGreaterThan(BigDecimal.ZERO);
    assertThat(agreement.expenseId()).isNotNull();
  }

  @Test
  void shouldSortPaginateAndKeepCategorySummaryIndependentOfPage() throws Exception {
    Fixture fx = bootstrap("category-page");
    UUID foodId = createExpenseCategory(fx.token(), "Alimentação").id();
    UUID transportId = createExpenseCategory(fx.token(), "Transporte").id();
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    createExpense(
        fx,
        foodId,
        "Mercado",
        "10.00",
        "2026-08-01",
        "2026-08-10",
        "ACCOUNT",
        fx.accountId(),
        "MINE",
        null,
        1);
    createExpense(
        fx,
        transportId,
        "Uber",
        "20.00",
        "2026-08-01",
        "2026-08-11",
        "ACCOUNT",
        fx.accountId(),
        "MINE",
        null,
        1);
    createIncome(fx.token(), salaryId, "Salário", "100.00", "2026-08-05");

    CategoryReportResponse byName =
        categories(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "name");
    assertThat(byName.items())
        .extracting(CategoryReportItemResponse::name)
        .containsExactly("Alimentação", "Salário", "Transporte");

    CategoryReportResponse byType =
        categories(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "type");
    assertThat(byType.items())
        .extracting(CategoryReportItemResponse::type)
        .containsExactly(CategoryType.EXPENSE, CategoryType.EXPENSE, CategoryType.INCOME);
    List<UUID> expenseIds =
        byType.items().stream()
            .filter(item -> item.type() == CategoryType.EXPENSE)
            .map(CategoryReportItemResponse::categoryId)
            .toList();
    assertThat(expenseIds).isSorted();

    CategoryReportResponse page0 =
        categories(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "name",
            "page",
            "0",
            "size",
            "1");
    assertThat(page0.items()).hasSize(1);
    assertThat(page0.items().getFirst().name()).isEqualTo("Alimentação");
    assertThat(page0.totalItems()).isEqualTo(3);
    assertThat(page0.totalPages()).isEqualTo(3);
    assertThat(page0.summary().expense().periodOriginal()).isEqualByComparingTo("30.00");
    assertThat(page0.summary().income().amount()).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldIsolateCategoryReportBetweenUsers() throws Exception {
    Fixture owner = bootstrap("category-iso-a");
    Fixture other = bootstrap("category-iso-b");
    createExpense(
        owner,
        "Privada",
        "90.00",
        "2026-08-01",
        "2026-08-15",
        "ACCOUNT",
        owner.accountId(),
        "MINE",
        null,
        1);
    createIncome(
        owner.token(),
        createIncomeCategory(owner.token(), "Salário").id(),
        "Salário",
        "5400.00",
        "2026-08-05");

    CategoryReportResponse otherView =
        categories(
            other.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(otherView.items()).isEmpty();
    assertThat(otherView.summary().expense().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(otherView.summary().income().amount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldRejectUnknownAndInvalidResponsibleReportParams() throws Exception {
    Fixture fx = bootstrap("resp-invalid");
    String token = fx.token();
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("nature", "CARD"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("nature", "EXPENSE")
                .param("dateType", "EXPECTED"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("nature", "INCOME"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("foo", "bar"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("nature", "EXPENSE")
                .param("startDate", "2026-08-31")
                .param("endDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("nature", "EXPENSE")
                .param("startDate", "2025-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/responsibles")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "EXPECTED")
                .param("sort", "name"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnEmptyResponsibleReportAndDefaultNatureToBoth() throws Exception {
    Fixture fx = bootstrap("resp-empty");
    ResponsibleReportResponse both =
        responsibles(
            fx.token(), "dateType", "EXPECTED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(both.nature()).isEqualTo(ReportNature.BOTH);
    assertThat(both.dateType()).isEqualTo(ReportDateType.EXPECTED);
    assertThat(both.items()).isEmpty();
    assertThat(both.summary().expense().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(both.summary().income().amount()).isEqualByComparingTo("0.00");

    ResponsibleReportResponse expenseOnly =
        responsibles(
            fx.token(), "nature", "EXPENSE", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(expenseOnly.nature()).isEqualTo(ReportNature.EXPENSE);
    assertThat(expenseOnly.dateType()).isNull();
    assertThat(expenseOnly.items()).isEmpty();
    assertThat(expenseOnly.summary().expense().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(expenseOnly.summary().income()).isNull();
  }

  @Test
  void shouldGroupResponsiblesByNatureWithoutMixingMetrics() throws Exception {
    Fixture fx = bootstrap("resp-group");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    ExpenseResponse rent =
        createExpense(
            fx,
            "Aluguel",
            "800.00",
            "2026-07-15",
            "2026-08-10",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    pay(fx.token(), rent.id(), fx.accountId(), "800.00");
    createExpense(
        fx,
        "Luz",
        "90.00",
        "2026-08-01",
        "2026-08-20",
        "ACCOUNT",
        fx.accountId(),
        "GIULIA",
        null,
        1);
    createExpense(
        fx, "João", "40.00", "2026-08-01", "2026-08-21", "NONE", null, "OTHER", "João", 1);
    createExpense(
        fx, "Maria", "50.00", "2026-08-01", "2026-08-22", "NONE", null, "OTHER", "Maria", 1);
    IncomeResponse salary =
        createIncome(fx.token(), salaryId, "Salário", "5400.00", "2026-08-05", "MINE", null);
    createAccrual(fx.token(), salary.id(), "100.00", "2026-08-06");
    createIncome(fx.token(), salaryId, "Sem responsável", "200.00", "2026-08-12");

    ResponsibleReportResponse july =
        responsibles(
            fx.token(), "nature", "EXPENSE", "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items()).isEmpty();

    ResponsibleReportResponse expenses =
        responsibles(
            fx.token(), "nature", "EXPENSE", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(expenses.nature()).isEqualTo(ReportNature.EXPENSE);
    assertThat(expenses.dateType()).isNull();
    assertThat(expenses.items())
        .extracting(ResponsibleReportItemResponse::key)
        .containsExactly("GIULIA", "MINE", "OTHER/João", "OTHER/Maria");
    assertThat(itemByKey(expenses, "MINE").expense().periodOriginal())
        .isEqualByComparingTo("800.00");
    assertThat(itemByKey(expenses, "MINE").income()).isNull();
    assertThat(itemByKey(expenses, "GIULIA").expense().periodOriginal())
        .isEqualByComparingTo("90.00");
    assertThat(itemByKey(expenses, "OTHER/João").responsibleType())
        .isEqualTo(ResponsibleType.OTHER);
    assertThat(itemByKey(expenses, "OTHER/João").responsibleName()).isEqualTo("João");
    assertThat(itemByKey(expenses, "OTHER/Maria").responsibleName()).isEqualTo("Maria");
    assertThat(expenses.summary().expense().periodOriginal()).isEqualByComparingTo("980.00");
    assertThat(expenses.summary().income()).isNull();

    ResponsibleReportResponse incomesExpected =
        responsibles(
            fx.token(),
            "nature",
            "INCOME",
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(incomesExpected.items()).hasSize(2);
    ResponsibleReportItemResponse mineIncome = itemByKey(incomesExpected, "MINE");
    assertThat(mineIncome.expense()).isNull();
    assertThat(mineIncome.income().amount()).isEqualByComparingTo("5400.00");
    assertThat(mineIncome.income().accruedAmount()).isEqualByComparingTo("100.00");
    assertThat(mineIncome.income().receivedAmount()).isEqualByComparingTo("0.00");
    assertThat(mineIncome.income().remainingAmount()).isEqualByComparingTo("5500.00");
    assertThat(mineIncome.income().periodReceivedAmount()).isNull();
    ResponsibleReportItemResponse unassigned = itemByKey(incomesExpected, "UNASSIGNED");
    assertThat(unassigned.responsibleType()).isNull();
    assertThat(unassigned.responsibleName()).isNull();
    assertThat(unassigned.income().amount()).isEqualByComparingTo("200.00");
    assertThat(incomesExpected.summary().expense()).isNull();
    assertThat(incomesExpected.summary().income().amount()).isEqualByComparingTo("5600.00");

    ResponsibleReportResponse both =
        responsibles(
            fx.token(),
            "nature",
            "BOTH",
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(both.items()).hasSize(5);
    ResponsibleReportItemResponse mineBoth = itemByKey(both, "MINE");
    assertThat(mineBoth.expense().periodOriginal()).isEqualByComparingTo("800.00");
    assertThat(mineBoth.income().amount()).isEqualByComparingTo("5400.00");
    assertThat(both.summary().expense().periodOriginal()).isEqualByComparingTo("980.00");
    assertThat(both.summary().income().remainingAmount()).isEqualByComparingTo("5700.00");
  }

  @Test
  void shouldReportReceivedResponsibleIncomeAndKeepCreditCardIdentity() throws Exception {
    Fixture fx = bootstrap("resp-received", "5000.00", "5000.00");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    ExpenseResponse cardExpense = createCardExpense(fx, "200.00", "2026-08-05", 1);
    IncomeResponse salary =
        createIncome(fx.token(), salaryId, "Salário", "5400.00", "2026-07-05", "MINE", null);
    receive(fx.token(), salary.id(), fx.accountId(), "2000.00", "2026-07-10");
    receive(fx.token(), salary.id(), fx.accountId(), "3400.00", "2026-08-12");

    ResponsibleReportResponse received =
        responsibles(
            fx.token(),
            "nature",
            "INCOME",
            "dateType",
            "RECEIVED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    ResponsibleReportItemResponse incomeItem = itemByKey(received, "MINE");
    assertThat(incomeItem.income().periodReceivedAmount()).isEqualByComparingTo("3400.00");
    assertThat(incomeItem.income().amount()).isNull();
    assertThat(received.summary().income().periodReceivedAmount()).isEqualByComparingTo("3400.00");
    assertThat(received.summary().expense()).isNull();

    ResponsibleReportResponse bothReceived =
        responsibles(
            fx.token(), "dateType", "RECEIVED", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(bothReceived.nature()).isEqualTo(ReportNature.BOTH);
    ResponsibleReportItemResponse mine = itemByKey(bothReceived, "MINE");
    assertThat(mine.expense().periodOriginal()).isEqualByComparingTo("200.00");
    assertThat(mine.expense().periodPaid().add(mine.expense().periodRemaining()))
        .isEqualByComparingTo(mine.expense().periodObligation());
    assertThat(mine.income().periodReceivedAmount()).isEqualByComparingTo("3400.00");
    assertThat(cardExpense.id()).isNotNull();
  }

  @Test
  void shouldKeepCancelledOutOfResponsibleSummaryAndPreserveAgreementGrain() throws Exception {
    Fixture fx = bootstrap("resp-status", "20000.00", "5000.00");
    ExpenseResponse cancelled =
        createExpense(
            fx,
            "Cancelada",
            "40.00",
            "2026-08-01",
            "2026-08-10",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    ExpenseResponse refunded =
        createExpense(
            fx,
            "Estornada",
            "60.00",
            "2026-08-01",
            "2026-08-11",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    pay(fx.token(), refunded.id(), fx.accountId(), "60.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refunded.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    ExpenseResponse open =
        createExpense(
            fx,
            "Aberta",
            "25.00",
            "2026-08-01",
            "2026-08-12",
            "ACCOUNT",
            fx.accountId(),
            "GIULIA",
            null,
            1);

    ResponsibleReportResponse august =
        responsibles(
            fx.token(), "nature", "EXPENSE", "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(itemByKey(august, "MINE").expense().periodOriginal()).isEqualByComparingTo("100.00");
    assertThat(august.summary().expense().periodOriginal()).isEqualByComparingTo("25.00");
    assertThat(itemByKey(august, "GIULIA").expense().periodOriginal())
        .isEqualByComparingTo("25.00");
    assertThat(open.id()).isNotNull();

    Fixture agreementFx = bootstrap("resp-agreement", "20000.00", "5000.00");
    ExpenseResponse original = createCardExpense(agreementFx, "12000.00", "2026-07-05", 12);
    ExpenseInstallmentResponse[] originalInstallments =
        listExpenseInstallments(agreementFx, original.id());
    UUID julyInvoiceId = invoiceByClosing(agreementFx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(agreementFx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);
    createAgreement(agreementFx, julyInvoiceId, "0.00", 10, "120.00");

    ResponsibleReportResponse july =
        responsibles(
            agreementFx.token(),
            "nature",
            "EXPENSE",
            "startDate",
            "2026-07-01",
            "endDate",
            "2026-07-31");
    assertThat(july.items()).hasSize(1);
    assertThat(july.items().getFirst().expense().periodOriginal()).isEqualByComparingTo("1000.00");
    assertThat(july.summary().expense().periodOriginal()).isEqualByComparingTo("0.00");

    LocalDate secondDue = originalInstallments[1].dueDate();
    ResponsibleReportResponse secondMonth =
        responsibles(
            agreementFx.token(),
            "nature",
            "EXPENSE",
            "startDate",
            secondDue.withDayOfMonth(1).toString(),
            "endDate",
            secondDue.withDayOfMonth(secondDue.lengthOfMonth()).toString());
    assertThat(secondMonth.items()).hasSize(1);
    assertThat(secondMonth.summary().expense().periodOriginal()).isGreaterThan(BigDecimal.ZERO);
  }

  @Test
  void shouldSortPaginateAndKeepResponsibleSummaryIndependentOfPage() throws Exception {
    Fixture fx = bootstrap("resp-sort");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    createExpense(
        fx,
        "Giulia",
        "10.00",
        "2026-08-01",
        "2026-08-10",
        "ACCOUNT",
        fx.accountId(),
        "GIULIA",
        null,
        1);
    createExpense(
        fx,
        "Mine",
        "20.00",
        "2026-08-01",
        "2026-08-11",
        "ACCOUNT",
        fx.accountId(),
        "MINE",
        null,
        1);
    createIncome(fx.token(), salaryId, "Livre", "100.00", "2026-08-05");

    ResponsibleReportResponse byType =
        responsibles(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "responsibleType");
    assertThat(byType.items())
        .extracting(ResponsibleReportItemResponse::key)
        .containsExactly("GIULIA", "MINE", "UNASSIGNED");

    ResponsibleReportResponse byTypeDesc =
        responsibles(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "responsibleType",
            "direction",
            "desc");
    assertThat(byTypeDesc.items())
        .extracting(ResponsibleReportItemResponse::key)
        .containsExactly("UNASSIGNED", "MINE", "GIULIA");

    ResponsibleReportResponse byName =
        responsibles(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "responsibleName",
            "direction",
            "asc");
    assertThat(byName.items())
        .extracting(ResponsibleReportItemResponse::key)
        .containsExactly("GIULIA", "MINE", "UNASSIGNED");

    ResponsibleReportResponse page0 =
        responsibles(
            fx.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "page",
            "0",
            "size",
            "1");
    assertThat(page0.items()).hasSize(1);
    assertThat(page0.items().getFirst().key()).isEqualTo("GIULIA");
    assertThat(page0.totalItems()).isEqualTo(3);
    assertThat(page0.totalPages()).isEqualTo(3);
    assertThat(page0.summary().expense().periodOriginal()).isEqualByComparingTo("30.00");
    assertThat(page0.summary().income().amount()).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldIsolateResponsibleReportBetweenUsers() throws Exception {
    Fixture owner = bootstrap("resp-iso-a");
    Fixture other = bootstrap("resp-iso-b");
    createExpense(
        owner,
        "Privada",
        "90.00",
        "2026-08-01",
        "2026-08-15",
        "ACCOUNT",
        owner.accountId(),
        "MINE",
        null,
        1);
    createIncome(
        owner.token(),
        createIncomeCategory(owner.token(), "Salário").id(),
        "Salário",
        "5400.00",
        "2026-08-05",
        "MINE",
        null);

    ResponsibleReportResponse otherView =
        responsibles(
            other.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(otherView.items()).isEmpty();
    assertThat(otherView.summary().expense().periodOriginal()).isEqualByComparingTo("0.00");
    assertThat(otherView.summary().income().amount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldRejectUnknownAndInvalidCardReportParams() throws Exception {
    Fixture fx = bootstrap("card-invalid");
    String token = fx.token();
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("foo", "bar"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("creditDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("usedLimit", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("responsibleType", "MINE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-31")
                .param("endDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2025-08-01")
                .param("endDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnEmptyCardReportWhenThereAreNoFacts() throws Exception {
    Fixture fx = bootstrap("card-empty");
    CardReportResponse report =
        cards(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.period().startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(report.period().endDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(report.items()).isEmpty();
    assertThat(report.totalItems()).isZero();
    assertThat(report.totalPages()).isZero();
    assertThat(report.page()).isZero();
    assertThat(report.size()).isEqualTo(20);
    assertThat(report.summary().purchaseAmount()).isEqualByComparingTo("0.00");
    assertThat(report.summary().invoiceAmount()).isEqualByComparingTo("0.00");
    assertThat(report.summary().paidAmount()).isEqualByComparingTo("0.00");
    assertThat(report.summary().creditAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldUseDefaultCurrentMonthAndIncludeOwnedCardOnly() throws Exception {
    Fixture owner = bootstrap("card-owned");
    Fixture other = bootstrap("card-other");
    ExpenseResponse purchase = createCardExpense(owner, "200.00", "2026-08-05", 1);
    createCardExpense(other, "90.00", "2026-08-05", 1);

    CardReportResponse defaultPeriod = cards(owner.token());
    assertThat(defaultPeriod.period().startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(defaultPeriod.period().endDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(defaultPeriod.items()).hasSize(1);
    CardReportItemResponse item = defaultPeriod.items().getFirst();
    assertThat(item.creditCardId()).isEqualTo(owner.cardId());
    assertThat(item.name()).isEqualTo("Nubank");
    assertThat(item.holderName()).isEqualTo("Ederson");
    assertThat(item.lastFourDigits()).isEqualTo("9999");
    assertThat(item.active()).isTrue();
    assertThat(item.purchases()).hasSize(1);
    assertThat(item.purchases().getFirst().expenseId()).isEqualTo(purchase.id());
    assertThat(item.purchases().getFirst().expenseDate()).isEqualTo(LocalDate.of(2026, 8, 5));
    assertThat(item.purchases().getFirst().original()).isEqualByComparingTo("200.00");
    assertThat(defaultPeriod.summary().purchaseAmount()).isEqualByComparingTo("200.00");

    CardReportResponse otherView =
        cards(other.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(otherView.items()).hasSize(1);
    assertThat(otherView.items().getFirst().creditCardId()).isEqualTo(other.cardId());
    assertThat(otherView.items().getFirst().purchases().getFirst().expenseId())
        .isNotEqualTo(purchase.id());
    assertThat(otherView.summary().purchaseAmount()).isEqualByComparingTo("90.00");
  }

  @Test
  void shouldListInstallmentPurchaseOnceWithoutDuplicatingOriginal() throws Exception {
    Fixture fx = bootstrap("card-purchase-once", "20000.00", "5000.00");
    ExpenseResponse purchase = createCardExpense(fx, "12000.00", "2026-08-02", 12);

    CardReportResponse report =
        cards(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.items()).hasSize(1);
    CardReportItemResponse item = report.items().getFirst();
    assertThat(item.purchases()).hasSize(1);
    CardReportPurchaseResponse line = item.purchases().getFirst();
    assertThat(line.expenseId()).isEqualTo(purchase.id());
    assertThat(line.original()).isEqualByComparingTo("12000.00");
    assertThat(line.totalInstallments()).isEqualTo(12);
    assertThat(line.installments()).hasSize(12);
    assertThat(line.installments().getFirst().installmentNumber()).isEqualTo(1);
    assertThat(line.installments().stream().map(installment -> installment.amount()))
        .allMatch(amount -> amount.compareTo(new BigDecimal("1000.00")) == 0);
    assertThat(item.summary().purchaseAmount()).isEqualByComparingTo("12000.00");
    assertThat(report.summary().purchaseAmount()).isEqualByComparingTo("12000.00");
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].usedLimit").doesNotExist())
        .andExpect(jsonPath("$.items[0].availableLimit").doesNotExist())
        .andExpect(jsonPath("$.summary.usedLimit").doesNotExist())
        .andExpect(jsonPath("$.items[0].purchases[0].purchaseDate").doesNotExist());
  }

  @Test
  void shouldIncludeInvoiceByClosingDateAndKeepDueDateOnObject() throws Exception {
    Fixture fx = bootstrap("card-closing", "5000.00", "5000.00");
    CreditCardResponse delayed =
        createCard(fx.token(), "Visa", "Giulia", "4321", "5000.00", 31, 10);
    createCardExpense(fx, delayed.id(), "300.00", "2026-07-05", 1);
    CreditCardInvoiceResponse julyInvoice =
        invoiceByClosing(fx, delayed.id(), LocalDate.of(2026, 7, 31));
    assertThat(julyInvoice.dueDate()).isEqualTo(LocalDate.of(2026, 8, 10));

    CardReportResponse july = cards(fx.token(), "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items()).hasSize(1);
    assertThat(july.items().getFirst().creditCardId()).isEqualTo(delayed.id());
    assertThat(july.items().getFirst().purchases()).hasSize(1);
    assertThat(july.items().getFirst().purchases().getFirst().original())
        .isEqualByComparingTo("300.00");
    assertThat(july.items().getFirst().invoices()).hasSize(1);
    assertThat(july.items().getFirst().invoices().getFirst().id()).isEqualTo(julyInvoice.id());
    assertThat(july.items().getFirst().invoices().getFirst().closingDate())
        .isEqualTo(LocalDate.of(2026, 7, 31));
    assertThat(july.items().getFirst().invoices().getFirst().dueDate())
        .isEqualTo(LocalDate.of(2026, 8, 10));
    assertThat(july.summary().invoiceAmount())
        .isEqualByComparingTo(july.items().getFirst().invoices().getFirst().totalAmount());

    CardReportResponse august =
        cards(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.items()).isEmpty();
    assertThat(august.summary().invoiceAmount()).isEqualByComparingTo("0.00");
    assertThat(august.summary().purchaseAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldIncludeInvoicePaymentByPaymentDate() throws Exception {
    Fixture fx = bootstrap("card-pay");
    createCardExpense(fx, "200.00", "2026-07-05", 1);
    UUID julyInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(fx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);
    payInvoice(fx, julyInvoiceId, "50.00", "2026-08-12");

    CardReportResponse july = cards(fx.token(), "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items().getFirst().payments()).isEmpty();
    assertThat(july.summary().paidAmount()).isEqualByComparingTo("0.00");
    assertThat(july.items().getFirst().invoices()).hasSize(1);

    CardReportResponse august =
        cards(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.items()).hasSize(1);
    assertThat(august.items().getFirst().purchases()).isEmpty();
    assertThat(august.items().getFirst().invoices()).isEmpty();
    assertThat(august.items().getFirst().payments()).hasSize(1);
    assertThat(august.items().getFirst().payments().getFirst().invoiceId())
        .isEqualTo(julyInvoiceId);
    assertThat(august.items().getFirst().payments().getFirst().paymentDate())
        .isEqualTo(LocalDate.of(2026, 8, 12));
    assertThat(august.items().getFirst().payments().getFirst().amount())
        .isEqualByComparingTo("50.00");
    assertThat(august.summary().paidAmount()).isEqualByComparingTo("50.00");
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].payments[0].paymentDate").value("2026-08-12"));
  }

  @Test
  void shouldIncludeCreditApplicationByCreatedAt() throws Exception {
    Fixture fx = bootstrap("card-credit");
    createCardExpense(fx, "80.00", "2026-07-05", 1);
    UUID julyInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(fx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);
    createManualCredit(fx, "30.00");

    CardReportResponse july = cards(fx.token(), "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(july.items().getFirst().credits()).isEmpty();
    assertThat(july.summary().creditAmount()).isEqualByComparingTo("0.00");
    assertThat(july.items().getFirst().invoices()).hasSize(1);

    CardReportResponse august =
        cards(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(august.items()).hasSize(1);
    assertThat(august.items().getFirst().credits()).hasSize(1);
    assertThat(august.items().getFirst().credits().getFirst().invoiceId()).isEqualTo(julyInvoiceId);
    assertThat(august.items().getFirst().credits().getFirst().amount())
        .isEqualByComparingTo("30.00");
    assertThat(august.items().getFirst().credits().getFirst().createdAt()).isNotNull();
    assertThat(august.summary().creditAmount()).isEqualByComparingTo("30.00");
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].credits[0].creditDate").doesNotExist())
        .andExpect(jsonPath("$.items[0].credits[0].createdAt").exists())
        .andExpect(jsonPath("$.items[0].credits[0].creditId").exists())
        .andExpect(jsonPath("$.items[0].credits[0].installmentId").exists());
  }

  @Test
  void shouldKeepInstallmentAndInvoiceAdjustmentsSeparated() throws Exception {
    Fixture fx = bootstrap("card-adj");
    ExpenseResponse expense = createCardExpense(fx, "200.00", "2026-08-05", 1);
    ExpenseInstallmentResponse installment = listExpenseInstallments(fx, expense.id())[0];
    createInstallmentAdjustment(fx, expense.id(), installment.id(), "10.00");
    CreditCardInvoiceResponse invoice = invoiceByClosing(fx, LocalDate.of(2026, 8, 10));
    createInvoiceAdjustment(fx, invoice.id(), "5.00");

    CardReportResponse report =
        cards(fx.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    assertThat(report.items()).hasSize(1);
    assertThat(report.items().getFirst().installmentAdjustments()).hasSize(1);
    assertThat(report.items().getFirst().installmentAdjustments().getFirst().expenseId())
        .isEqualTo(expense.id());
    assertThat(report.items().getFirst().installmentAdjustments().getFirst().installmentId())
        .isEqualTo(installment.id());
    assertThat(report.items().getFirst().installmentAdjustments().getFirst().amount())
        .isEqualByComparingTo("10.00");
    assertThat(report.items().getFirst().invoiceAdjustments()).hasSize(1);
    assertThat(report.items().getFirst().invoiceAdjustments().getFirst().invoiceId())
        .isEqualTo(invoice.id());
    assertThat(report.items().getFirst().invoiceAdjustments().getFirst().amount())
        .isEqualByComparingTo("5.00");
    mockMvc
        .perform(
            get("/api/v1/reports/cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].adjustments").doesNotExist())
        .andExpect(jsonPath("$.items[0].installmentAdjustments[0].adjustmentDate").doesNotExist())
        .andExpect(jsonPath("$.items[0].invoiceAdjustments[0].adjustmentDate").doesNotExist())
        .andExpect(jsonPath("$.items[0].installmentAdjustments[0].createdAt").exists())
        .andExpect(jsonPath("$.items[0].invoiceAdjustments[0].createdAt").exists());
  }

  @Test
  void shouldKeepOriginalPurchaseWhenInvoiceIsSettledByAgreement() throws Exception {
    Fixture fx = bootstrap("card-agreement", "20000.00", "5000.00");
    ExpenseResponse original = createCardExpense(fx, "12000.00", "2026-07-05", 12);
    UUID julyInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(fx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);
    AgreementResponse agreement = createAgreement(fx, julyInvoiceId, "0.00", 10, "120.00");
    assertThat(getInvoice(fx.token(), julyInvoiceId).status())
        .isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);

    CardReportResponse july = cards(fx.token(), "startDate", "2026-07-01", "endDate", "2026-07-31");
    CardReportPurchaseResponse originalLine =
        july.items().getFirst().purchases().stream()
            .filter(item -> item.expenseId().equals(original.id()))
            .findFirst()
            .orElseThrow();
    assertThat(originalLine.original()).isEqualByComparingTo("12000.00");
    assertThat(originalLine.installments()).hasSize(12);
    CardReportPurchaseResponse agreementLine =
        july.items().getFirst().purchases().stream()
            .filter(item -> item.expenseId().equals(agreement.expenseId()))
            .findFirst()
            .orElseThrow();
    assertThat(agreementLine.expenseId()).isNotEqualTo(original.id());
    assertThat(agreementLine.original()).isEqualByComparingTo("1200.00");
    assertThat(july.items().getFirst().purchases()).hasSize(2);
    assertThat(july.summary().purchaseAmount()).isEqualByComparingTo("13200.00");
    assertThat(july.items().getFirst().invoices()).hasSize(1);
    assertThat(july.items().getFirst().invoices().getFirst().id()).isEqualTo(julyInvoiceId);
    assertThat(july.items().getFirst().invoices().getFirst().status())
        .isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
  }

  @Test
  void shouldPaginateCardsWithoutChangingSummary() throws Exception {
    Fixture fx = bootstrap("card-page", "5000.00", "5000.00");
    CreditCardResponse second = createCard(fx.token(), "Alpha", "Ana", "1111", "5000.00", 10, 20);
    createCardExpense(fx, "40.00", "2026-08-05", 1);
    createCardExpense(fx, second.id(), "60.00", "2026-08-05", 1);

    CardReportResponse page0 =
        cards(
            fx.token(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "name",
            "page",
            "0",
            "size",
            "1");
    CardReportResponse page1 =
        cards(
            fx.token(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "sort",
            "name",
            "page",
            "1",
            "size",
            "1");
    assertThat(page0.items()).hasSize(1);
    assertThat(page1.items()).hasSize(1);
    assertThat(page0.totalItems()).isEqualTo(2);
    assertThat(page0.totalPages()).isEqualTo(2);
    assertThat(page0.items().getFirst().creditCardId()).isEqualTo(second.id());
    assertThat(page1.items().getFirst().creditCardId()).isEqualTo(fx.cardId());
    assertThat(page0.summary().purchaseAmount()).isEqualByComparingTo("100.00");
    assertThat(page1.summary().purchaseAmount()).isEqualByComparingTo("100.00");
    assertThat(page0.summary()).isEqualTo(page1.summary());

    CardReportResponse filtered =
        cards(
            fx.token(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "creditCardId",
            second.id().toString());
    assertThat(filtered.items()).hasSize(1);
    assertThat(filtered.items().getFirst().creditCardId()).isEqualTo(second.id());
    assertThat(filtered.summary().purchaseAmount()).isEqualByComparingTo("60.00");
  }

  @Test
  void shouldRejectUnknownAndInvalidInvoiceReportParams() throws Exception {
    Fixture fx = bootstrap("inv-invalid");
    createCardExpense(fx, "50.00", "2026-08-05", 1);
    UUID invoiceId = invoiceByClosing(fx, LocalDate.of(2026, 8, 10)).id();
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + invoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + invoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("page", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + invoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("size", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + invoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + invoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("creditDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnNotFoundForMissingOrForeignInvoiceReport() throws Exception {
    Fixture owner = bootstrap("inv-404-a");
    Fixture other = bootstrap("inv-404-b");
    createCardExpense(owner, "80.00", "2026-08-05", 1);
    UUID ownerInvoiceId = invoiceByClosing(owner, LocalDate.of(2026, 8, 10)).id();
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + ownerInvoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldReportInvoiceWithOfficialHeaderAndSinglePurchaseLine() throws Exception {
    Fixture fx = bootstrap("inv-header", "20000.00", "5000.00");
    ExpenseResponse purchase = createCardExpense(fx, "12000.00", "2026-08-02", 12);
    UUID invoiceId = invoiceByClosing(fx, LocalDate.of(2026, 8, 10)).id();
    CreditCardInvoiceResponse official = getInvoice(fx.token(), invoiceId);

    InvoiceReportResponse report = invoiceReport(fx.token(), invoiceId);
    assertThat(report.invoiceId()).isEqualTo(invoiceId);
    assertThat(report.card().name()).isEqualTo("Nubank");
    assertThat(report.card().holderName()).isEqualTo("Ederson");
    assertThat(report.invoice().closingDate()).isEqualTo(official.closingDate());
    assertThat(report.invoice().dueDate()).isEqualTo(official.dueDate());
    assertThat(report.invoice().status()).isEqualTo(official.status());
    assertThat(report.invoice().totalAmount()).isEqualByComparingTo(official.totalAmount());
    assertThat(report.invoice().paidAmount()).isEqualByComparingTo(official.paidAmount());
    assertThat(report.invoice().remainingAmount()).isEqualByComparingTo(official.remainingAmount());
    assertThat(report.purchases()).hasSize(1);
    InvoiceReportPurchaseResponse line = report.purchases().getFirst();
    assertThat(line.expenseId()).isEqualTo(purchase.id());
    assertThat(line.original()).isEqualByComparingTo("12000.00");
    assertThat(line.installmentNumber()).isEqualTo(1);
    assertThat(line.totalInstallments()).isEqualTo(12);
    assertThat(line.categoryName()).isEqualTo("Moradia");
    assertThat(report.byCategory()).hasSize(1);
    assertThat(report.byCategory().getFirst().original()).isEqualByComparingTo("12000.00");
    assertThat(report.byResponsible()).hasSize(1);
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + invoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.creditLimit").doesNotExist())
        .andExpect(jsonPath("$.invoice.creditLimit").doesNotExist())
        .andExpect(jsonPath("$.adjustments").doesNotExist());
  }

  @Test
  void shouldFilterInvoicePurchasesByResponsibleWithoutRecalculatingHeader() throws Exception {
    Fixture fx = bootstrap("inv-resp");
    createCardExpense(
        fx, fx.cardId(), fx.categoryId(), "Compra minha", "80.00", "2026-08-05", 1, "MINE", null);
    createCardExpense(
        fx,
        fx.cardId(),
        fx.categoryId(),
        "Compra Giulia",
        "40.00",
        "2026-08-05",
        1,
        "GIULIA",
        null);
    createCardExpense(
        fx, fx.cardId(), fx.categoryId(), "Compra João", "25.00", "2026-08-05", 1, "OTHER", "João");
    UUID invoiceId = invoiceByClosing(fx, LocalDate.of(2026, 8, 10)).id();
    CreditCardInvoiceResponse official = getInvoice(fx.token(), invoiceId);

    InvoiceReportResponse full = invoiceReport(fx.token(), invoiceId);
    assertThat(full.purchases()).hasSize(3);
    assertThat(full.invoice().totalAmount()).isEqualByComparingTo(official.totalAmount());

    InvoiceReportResponse mine = invoiceReport(fx.token(), invoiceId, "responsibleType", "MINE");
    assertThat(mine.purchases()).hasSize(1);
    assertThat(mine.purchases().getFirst().description()).isEqualTo("Compra minha");
    assertThat(mine.byResponsible()).hasSize(1);
    assertThat(mine.invoice().totalAmount()).isEqualByComparingTo(official.totalAmount());
    assertThat(mine.invoice().paidAmount()).isEqualByComparingTo(official.paidAmount());
    assertThat(mine.invoice().remainingAmount()).isEqualByComparingTo(official.remainingAmount());

    InvoiceReportResponse joao =
        invoiceReport(fx.token(), invoiceId, "responsibleType", "OTHER", "responsibleName", "João");
    assertThat(joao.purchases()).hasSize(1);
    assertThat(joao.purchases().getFirst().responsibleName()).isEqualTo("João");

    InvoiceReportResponse empty =
        invoiceReport(fx.token(), invoiceId, "responsibleType", "EDERSON");
    assertThat(empty.purchases()).isEmpty();
    assertThat(empty.byCategory()).isEmpty();
    assertThat(empty.byResponsible()).isEmpty();
    assertThat(empty.installmentAdjustments()).isEmpty();
    assertThat(empty.invoiceId()).isEqualTo(invoiceId);
    assertThat(empty.invoice().totalAmount()).isEqualByComparingTo(official.totalAmount());
    assertThat(empty.invoice().remainingAmount()).isEqualByComparingTo(official.remainingAmount());
  }

  @Test
  void shouldKeepInvoiceCreditsAdjustmentsAllocationsAndAgreementGrain() throws Exception {
    Fixture fx = bootstrap("inv-facts", "20000.00", "5000.00");
    ExpenseResponse original = createCardExpense(fx, "12000.00", "2026-07-05", 12);
    UUID julyInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(fx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);
    payInvoice(fx, julyInvoiceId, "50.00", "2026-08-12");
    createInvoiceAdjustment(fx, julyInvoiceId, "10.00");
    ExpenseInstallmentResponse julyInstallment = listExpenseInstallments(fx, original.id())[0];
    createInstallmentAdjustment(fx, original.id(), julyInstallment.id(), "5.00");
    createManualCredit(fx, "20.00");
    AgreementResponse agreement = createAgreement(fx, julyInvoiceId, "0.00", 10, "120.00");
    CreditCardInvoiceResponse official = getInvoice(fx.token(), julyInvoiceId);
    assertThat(official.status()).isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);

    InvoiceReportResponse july = invoiceReport(fx.token(), julyInvoiceId);
    assertThat(july.invoice().status()).isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
    assertThat(july.invoice().totalAmount()).isEqualByComparingTo(official.totalAmount());
    assertThat(july.invoice().paidAmount()).isEqualByComparingTo(official.paidAmount());
    assertThat(july.invoice().remainingAmount()).isEqualByComparingTo(official.remainingAmount());
    assertThat(july.purchases()).hasSize(1);
    assertThat(july.purchases().getFirst().expenseId()).isEqualTo(original.id());
    assertThat(july.purchases().getFirst().original()).isEqualByComparingTo("12000.00");
    assertThat(july.purchases()).noneMatch(item -> item.expenseId().equals(agreement.expenseId()));
    assertThat(july.payments()).hasSize(1);
    assertThat(july.payments().getFirst().paymentDate()).isEqualTo(LocalDate.of(2026, 8, 12));
    assertThat(july.credits()).hasSize(1);
    assertThat(july.credits().getFirst().createdAt()).isNotNull();
    assertThat(july.installmentAdjustments()).hasSize(1);
    assertThat(july.invoiceAdjustments()).hasSize(1);
    assertThat(july.allocations())
        .anyMatch(item -> item.type() == InvoiceReportAllocationType.PAYMENT);
    assertThat(july.allocations())
        .anyMatch(item -> item.type() == InvoiceReportAllocationType.CREDIT);
    assertThat(july.allocations())
        .anyMatch(item -> item.type() == InvoiceReportAllocationType.SETTLEMENT);
    assertThat(july.allocations())
        .anyMatch(item -> item.type() == InvoiceReportAllocationType.INVOICE_ADJUSTMENT);

    UUID augustInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 8, 10)).id();
    InvoiceReportResponse august = invoiceReport(fx.token(), augustInvoiceId);
    assertThat(august.purchases())
        .anyMatch(
            item ->
                item.expenseId().equals(original.id())
                    && item.original().compareTo(new BigDecimal("12000.00")) == 0
                    && item.installmentNumber() == 2);
    assertThat(august.purchases()).anyMatch(item -> item.expenseId().equals(agreement.expenseId()));

    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + julyInvoiceId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.credits[0].creditDate").doesNotExist())
        .andExpect(jsonPath("$.credits[0].createdAt").exists())
        .andExpect(jsonPath("$.installmentAdjustments[0].adjustmentDate").doesNotExist())
        .andExpect(jsonPath("$.invoiceAdjustments[0].adjustmentDate").doesNotExist())
        .andExpect(jsonPath("$.installmentAdjustments[0].createdAt").exists())
        .andExpect(jsonPath("$.invoiceAdjustments[0].createdAt").exists());
  }

  @Test
  void shouldRejectUnknownAndInvalidCashFlowReportParams() throws Exception {
    Fixture fx = bootstrap("cf-invalid");
    String token = fx.token();
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("foo", "bar"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("dateType", "BOTH"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2026-08-31")
                .param("endDate", "2026-08-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("startDate", "2025-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("flowType", "EFFECTIVE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("sort", "description"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("flowType", "PROJECTED")
                .param("startDate", "2026-07-01")
                .param("endDate", "2026-07-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnEmptyHistoricalCashFlowForCurrentMonthWithoutFacts() throws Exception {
    Fixture fx = bootstrap("cf-empty", "5000.00", "1000.00");
    CashFlowResponse report =
        cashFlow(
            fx.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(report.flowType()).isEqualTo(CashFlowFlowType.HISTORICAL);
    assertThat(report.accountId()).isNull();
    assertThat(report.historical().items()).isEmpty();
    assertThat(report.historical().totalItems()).isZero();
    assertThat(report.historical().openingBalance()).isEqualByComparingTo("1000.00");
    assertThat(report.historical().closingBalance()).isEqualByComparingTo("1000.00");
    assertThat(report.historical().summary().totalIn()).isEqualByComparingTo("0.00");
    assertThat(report.historical().summary().totalOut()).isEqualByComparingTo("0.00");
    assertThat(report.historical().summary().net()).isEqualByComparingTo("0.00");
    assertThat(report.historical().openingBalance().add(report.historical().summary().net()))
        .isEqualByComparingTo(report.historical().closingBalance());
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("flowType", "HISTORICAL")
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projected").doesNotExist());
  }

  @Test
  void shouldPreserveCashFlowOpeningNetClosingIdentityAndOfficialDates() throws Exception {
    Fixture fx = bootstrap("cf-identity", "5000.00", "1000.00");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse prior = createIncome(fx.token(), salaryId, "Julho", "200.00", "2026-07-31");
    receive(fx.token(), prior.id(), fx.accountId(), "200.00", "2026-07-31");
    IncomeResponse salary = createIncome(fx.token(), salaryId, "Salário", "5400.00", "2026-08-05");
    receive(fx.token(), salary.id(), fx.accountId(), "5400.00", "2026-08-05");
    IncomeResponse todayIncome = createIncome(fx.token(), salaryId, "Hoje", "100.00", "2026-08-17");
    receive(fx.token(), todayIncome.id(), fx.accountId(), "100.00", "2026-08-17");
    createAccrual(fx.token(), salary.id(), "50.00", "2026-08-06");

    CashFlowResponse report =
        cashFlow(
            fx.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(report.historical().openingBalance()).isEqualByComparingTo("1200.00");
    assertThat(report.historical().items()).hasSize(2);
    assertThat(report.historical().items())
        .extracting(CashFlowItemResponse::date)
        .containsExactly(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 17));
    assertThat(report.historical().items())
        .allMatch(item -> item.type() == CashFlowType.INCOME_RECEIPT);
    assertThat(report.historical().summary().net()).isEqualByComparingTo("5500.00");
    assertThat(report.historical().closingBalance()).isEqualByComparingTo("6700.00");
    assertThat(report.historical().openingBalance().add(report.historical().summary().net()))
        .isEqualByComparingTo(report.historical().closingBalance());
    assertThat(accountBalance(fx.token(), fx.accountId()).totalBalance())
        .isEqualByComparingTo("6700.00");
  }

  @Test
  void shouldCloseHistoricalWindowOnMinEndDateAndToday() throws Exception {
    Fixture fx = bootstrap("cf-window", "5000.00", "1000.00");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse inWindow =
        createIncome(fx.token(), salaryId, "No recorte", "100.00", "2026-08-05");
    receive(fx.token(), inWindow.id(), fx.accountId(), "100.00", "2026-08-05");
    IncomeResponse afterEnd = createIncome(fx.token(), salaryId, "Após fim", "80.00", "2026-08-12");
    receive(fx.token(), afterEnd.id(), fx.accountId(), "80.00", "2026-08-12");

    CashFlowResponse untilTenth =
        cashFlow(
            fx.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-10");
    assertThat(untilTenth.historical().items()).hasSize(1);
    assertThat(untilTenth.historical().items().getFirst().amount()).isEqualByComparingTo("100.00");
    assertThat(untilTenth.historical().closingBalance()).isEqualByComparingTo("1100.00");
    assertThat(
            untilTenth.historical().openingBalance().add(untilTenth.historical().summary().net()))
        .isEqualByComparingTo(untilTenth.historical().closingBalance());
  }

  @Test
  void shouldReportExpenseInvoiceTransferRefundAndBalanceAdjustmentFacts() throws Exception {
    Fixture fx = bootstrap("cf-facts", "20000.00", "1000.00");
    AccountResponse destination = createAccount(fx.token(), "0.00", "Destino");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse salary = createIncome(fx.token(), salaryId, "Salário", "400.00", "2026-08-05");
    receive(fx.token(), salary.id(), fx.accountId(), "400.00", "2026-08-05");
    ExpenseResponse rent =
        createExpense(
            fx,
            "Aluguel",
            "150.00",
            "2026-08-01",
            "2026-08-10",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);
    pay(fx.token(), rent.id(), fx.accountId(), "150.00", "2026-08-10");
    ExpenseResponse cardPurchase = createCardExpense(fx, "80.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);
    payInvoice(fx, invoice.id(), "50.00", "2026-08-12");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cardPurchase.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"settlement":"ACCOUNT","accountId":"%s"}
                    """
                        .formatted(fx.accountId())))
        .andExpect(status().isOk());
    createTransfer(fx.token(), fx.accountId(), destination.id(), "100.00", "2026-08-13", "Repasse");
    createBalanceAdjustment(fx.token(), fx.accountId(), "1110.00", "2026-08-14");

    CashFlowResponse report =
        cashFlow(
            fx.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "size",
            "50");
    assertThat(report.historical().items())
        .extracting(CashFlowItemResponse::type)
        .containsExactlyInAnyOrder(
            CashFlowType.INCOME_RECEIPT,
            CashFlowType.EXPENSE_PAYMENT,
            CashFlowType.INVOICE_PAYMENT,
            CashFlowType.CARD_PURCHASE_REFUND,
            CashFlowType.TRANSFER_OUT,
            CashFlowType.TRANSFER_IN,
            CashFlowType.BALANCE_ADJUSTMENT);
    assertThat(report.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.EXPENSE_PAYMENT)
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.amount()).isEqualByComparingTo("-150.00");
              assertThat(item.description()).isEqualTo("Aluguel");
              assertThat(item.date()).isEqualTo(LocalDate.of(2026, 8, 10));
            });
    assertThat(report.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.INVOICE_PAYMENT)
        .singleElement()
        .satisfies(item -> assertThat(item.amount()).isEqualByComparingTo("-50.00"));
    assertThat(report.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.CARD_PURCHASE_REFUND)
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.amount()).isEqualByComparingTo("50.00");
              assertThat(item.date()).isEqualTo(TODAY);
            });
    List<CashFlowItemResponse> transfers =
        report.historical().items().stream()
            .filter(
                item ->
                    item.type() == CashFlowType.TRANSFER_IN
                        || item.type() == CashFlowType.TRANSFER_OUT)
            .toList();
    assertThat(transfers).hasSize(2);
    assertThat(
            transfers.stream()
                .map(CashFlowItemResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("0.00");
    assertThat(report.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.BALANCE_ADJUSTMENT)
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.date()).isEqualTo(LocalDate.of(2026, 8, 14));
              assertThat(item.amount()).isEqualByComparingTo("10.00");
            });
    assertThat(report.historical().openingBalance().add(report.historical().summary().net()))
        .isEqualByComparingTo(report.historical().closingBalance());
    CashFlowResponse sourceOnly =
        cashFlow(
            fx.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "accountId",
            fx.accountId().toString(),
            "size",
            "50");
    assertThat(sourceOnly.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.TRANSFER_IN)
        .isEmpty();
    assertThat(sourceOnly.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.TRANSFER_OUT)
        .hasSize(1);
  }

  @Test
  void shouldUseBalanceAdjustmentDateNotCreatedAtAndKeepAgreementWithoutDoubleCash()
      throws Exception {
    Fixture fx = bootstrap("cf-adj-agr", "20000.00", "1000.00");
    createBalanceAdjustment(fx.token(), fx.accountId(), "1010.00", "2026-07-15");
    ExpenseResponse original = createCardExpense(fx, "12000.00", "2026-07-05", 12);
    UUID julyInvoiceId = invoiceByClosing(fx, LocalDate.of(2026, 7, 10)).id();
    closeUntilStatus(fx, julyInvoiceId, CreditCardInvoiceStatus.CLOSED);
    payInvoice(fx, julyInvoiceId, "50.00", "2026-07-20");
    AgreementResponse agreement = createAgreement(fx, julyInvoiceId, "0.00", 10, "120.00");

    CashFlowResponse july =
        cashFlow(
            fx.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-07-01",
            "endDate",
            "2026-07-31");
    assertThat(july.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.BALANCE_ADJUSTMENT)
        .singleElement()
        .satisfies(item -> assertThat(item.date()).isEqualTo(LocalDate.of(2026, 7, 15)));
    assertThat(july.historical().items())
        .filteredOn(item -> item.type() == CashFlowType.INVOICE_PAYMENT)
        .hasSize(1);
    assertThat(july.historical().items())
        .noneMatch(item -> item.type() == CashFlowType.EXPENSE_PAYMENT);
    assertThat(july.historical().items())
        .extracting(CashFlowItemResponse::id)
        .doesNotContain(agreement.expenseId(), original.id());

    CashFlowResponse august =
        cashFlow(
            fx.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(august.historical().items())
        .noneMatch(item -> item.type() == CashFlowType.BALANCE_ADJUSTMENT);
    assertThat(august.historical().items())
        .noneMatch(item -> item.type() == CashFlowType.EXPENSE_PAYMENT);
    assertThat(august.historical().openingBalance()).isEqualByComparingTo("960.00");
  }

  @Test
  void shouldReturnProjectedFromOfficialServiceAndEmptyProjectedWhenBothIsEntirelyPast()
      throws Exception {
    Fixture fx = bootstrap("cf-proj", "5000.00", "1000.00");
    ExpenseResponse open =
        createExpense(
            fx,
            "Futura",
            "80.00",
            "2026-08-01",
            "2026-08-25",
            "ACCOUNT",
            fx.accountId(),
            "MINE",
            null,
            1);

    CashFlowResponse projected =
        cashFlow(
            fx.token(),
            "flowType",
            "PROJECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(projected.historical()).isNull();
    assertThat(projected.projected().empty()).isNull();
    assertThat(projected.projected().summary()).isNotNull();
    assertThat(projected.projected().months()).isNotEmpty();
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("flowType", "PROJECTED")
                .param("startDate", "2026-08-01")
                .param("endDate", "2026-08-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historical").doesNotExist())
        .andExpect(jsonPath("$.projected.empty").doesNotExist())
        .andExpect(jsonPath("$.projected.events").doesNotExist())
        .andExpect(jsonPath("$.projected.undatedEvents").doesNotExist());

    MvcResult projectionsResult =
        mockMvc
            .perform(
                get("/api/v1/projections")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .param("startDate", "2026-08-01")
                    .param("endDate", "2026-08-31"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(projected.projected().summary().projectedExpense())
        .isEqualByComparingTo(
            jsonMapper
                .readTree(projectionsResult.getResponse().getContentAsString())
                .get("summary")
                .get("projectedExpense")
                .decimalValue());
    assertThat(open.id()).isNotNull();

    CashFlowResponse bothPast =
        cashFlow(
            fx.token(), "flowType", "BOTH", "startDate", "2026-07-01", "endDate", "2026-07-31");
    assertThat(bothPast.historical()).isNotNull();
    assertThat(bothPast.projected().empty()).isTrue();
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("flowType", "BOTH")
                .param("startDate", "2026-07-01")
                .param("endDate", "2026-07-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projected.empty").value(true))
        .andExpect(jsonPath("$.projected.summary").doesNotExist())
        .andExpect(jsonPath("$.projected.months").doesNotExist())
        .andExpect(jsonPath("$.projected.quarters").doesNotExist());
  }

  @Test
  void shouldHandleCurrentFutureIsolationAndUnknownAccountOnCashFlow() throws Exception {
    Fixture owner = bootstrap("cf-owner", "5000.00", "1000.00");
    Fixture other = bootstrap("cf-other", "5000.00", "2000.00");
    UUID salaryId = createIncomeCategory(owner.token(), "Salário").id();
    IncomeResponse salary =
        createIncome(owner.token(), salaryId, "Salário", "400.00", "2026-08-05");
    receive(owner.token(), salary.id(), owner.accountId(), "400.00", "2026-08-05");

    CashFlowResponse current = cashFlow(owner.token(), "flowType", "BOTH");
    assertThat(current.period().startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(current.period().endDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(current.historical().openingBalance()).isEqualByComparingTo("1000.00");
    assertThat(current.historical().closingBalance()).isEqualByComparingTo("1400.00");
    assertThat(current.projected().empty()).isNull();
    assertThat(current.projected().summary()).isNotNull();

    CashFlowResponse future =
        cashFlow(
            owner.token(), "flowType", "BOTH", "startDate", "2026-09-01", "endDate", "2026-09-30");
    assertThat(future.historical().items()).isEmpty();
    assertThat(future.historical().totalItems()).isZero();
    mockMvc
        .perform(
            get("/api/v1/reports/cash-flow")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                .param("flowType", "BOTH")
                .param("startDate", "2026-09-01")
                .param("endDate", "2026-09-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historical.openingBalance").doesNotExist())
        .andExpect(jsonPath("$.historical.closingBalance").doesNotExist())
        .andExpect(jsonPath("$.projected.empty").doesNotExist())
        .andExpect(jsonPath("$.projected.summary").exists());

    CashFlowResponse isolated =
        cashFlow(
            other.token(),
            "flowType",
            "HISTORICAL",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(isolated.historical().items()).isEmpty();
    assertThat(isolated.historical().openingBalance()).isEqualByComparingTo("2000.00");

    CashFlowResponse unknown =
        cashFlow(
            owner.token(),
            "flowType",
            "BOTH",
            "accountId",
            UUID.randomUUID().toString(),
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(unknown.historical().items()).isEmpty();
    assertThat(unknown.historical().openingBalance()).isNull();
    assertThat(unknown.projected().empty()).isTrue();
  }

  @Test
  void shouldExportReportPdfsWithContractHeadersAndIgnoreInvalidPagination() throws Exception {
    Fixture fx = bootstrap("pdf-headers", "5000.00", "1000.00");
    UUID salaryId = createIncomeCategory(fx.token(), "Salário").id();
    IncomeResponse salary = createIncome(fx.token(), salaryId, "Salário", "400.00", "2026-08-05");
    receive(fx.token(), salary.id(), fx.accountId(), "400.00", "2026-08-05");
    createExpense(
        fx,
        "Aluguel",
        "150.00",
        "2026-08-01",
        "2026-08-10",
        "ACCOUNT",
        fx.accountId(),
        "MINE",
        null,
        1);

    byte[] expenses =
        pdf(
            fx.token(),
            "/api/v1/reports/expenses/pdf",
            "attachment; filename=\"relatorio-despesas-2026-08-01_2026-08-31.pdf\"",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31",
            "page",
            "-1",
            "size",
            "999");
    assertThat(pdfText(expenses)).contains("Financial Control", "Aluguel", "Relatório de despesas");

    byte[] incomes =
        pdf(
            fx.token(),
            "/api/v1/reports/incomes/pdf",
            "attachment; filename=\"relatorio-receitas-2026-08-01_2026-08-31.pdf\"",
            "dateType",
            "RECEIVED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(pdfText(incomes)).contains("Salário");

    byte[] categories =
        pdf(
            fx.token(),
            "/api/v1/reports/categories/pdf",
            "attachment; filename=\"relatorio-categorias-2026-08-01_2026-08-31.pdf\"",
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(pdfText(categories)).contains("Moradia");

    byte[] responsibles =
        pdf(
            fx.token(),
            "/api/v1/reports/responsibles/pdf",
            "attachment; filename=\"relatorio-responsaveis-2026-08-01_2026-08-31.pdf\"",
            "nature",
            "EXPENSE",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(pdfText(responsibles)).contains("MINE");

    byte[] empty =
        pdf(
            fx.token(),
            "/api/v1/reports/expenses/pdf",
            "attachment; filename=\"relatorio-despesas-2026-07-01_2026-07-31.pdf\"",
            "startDate",
            "2026-07-01",
            "endDate",
            "2026-07-31");
    assertThat(pdfText(empty)).contains("Sem itens.");
  }

  @Test
  void shouldRejectUnknownPdfParamsAndMissingInvoice() throws Exception {
    Fixture fx = bootstrap("pdf-invalid");
    mockMvc
        .perform(
            get("/api/v1/reports/expenses/pdf")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("userId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + UUID.randomUUID() + "/pdf")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldExportCardInvoiceAndCashFlowPdfsWithoutDuplicatingOriginal() throws Exception {
    Fixture fx = bootstrap("pdf-card-cf", "20000.00", "5000.00");
    createCardExpense(fx, "12000.00", "2026-08-02", 12);
    UUID invoiceId = invoiceByClosing(fx, LocalDate.of(2026, 8, 10)).id();

    byte[] cards =
        pdf(
            fx.token(),
            "/api/v1/reports/cards/pdf",
            "attachment; filename=\"relatorio-cartoes-2026-08-01_2026-08-31.pdf\"",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    String cardsText = pdfText(cards);
    assertThat(cardsText).contains("Compra", "Original", "Parcela");
    assertThat(count(cardsText, "R$ 12.000,00")).isBetween(1, 3);
    assertThat(cardsText).doesNotContain("creditLimit");

    InvoiceReportResponse json = invoiceReport(fx.token(), invoiceId);
    byte[] invoicePdf =
        pdf(
            fx.token(),
            "/api/v1/reports/invoices/" + invoiceId + "/pdf",
            "attachment; filename=\"relatorio-fatura-"
                + json.invoice().referenceYear()
                + "-"
                + json.invoice().referenceMonth()
                + ".pdf\"",
            "page",
            "0",
            "size",
            "1");
    String invoiceText = pdfText(invoicePdf);
    assertThat(invoiceText)
        .contains(
            "Total: " + ReportsPdfRenderer.money(json.invoice().totalAmount()),
            "Pago: " + ReportsPdfRenderer.money(json.invoice().paidAmount()),
            "Restante: " + ReportsPdfRenderer.money(json.invoice().remainingAmount()));
    assertThat(invoiceText).doesNotContain(invoiceId.toString());
    assertThat(invoiceText).doesNotContain("creditLimit");

    Fixture other = bootstrap("pdf-other");
    mockMvc
        .perform(
            get("/api/v1/reports/invoices/" + invoiceId + "/pdf")
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
        .andExpect(status().isNotFound());

    byte[] bothPast =
        pdf(
            fx.token(),
            "/api/v1/reports/cash-flow/pdf",
            "attachment; filename=\"relatorio-fluxo-caixa-2026-07-01_2026-07-31.pdf\"",
            "flowType",
            "BOTH",
            "startDate",
            "2026-07-01",
            "endDate",
            "2026-07-31");
    assertThat(pdfText(bothPast)).contains("empty: true", "Histórico");

    byte[] current =
        pdf(
            fx.token(),
            "/api/v1/reports/cash-flow/pdf",
            "attachment; filename=\"relatorio-fluxo-caixa-2026-08-01_2026-08-31.pdf\"",
            "flowType",
            "BOTH",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    String currentText = pdfText(current);
    assertThat(currentText).contains("Histórico", "Projeção", "Saldo inicial", "Saldo final");
    assertThat(currentText).doesNotContain("empty: true");
  }

  private CashFlowResponse cashFlow(String token, String... params) throws Exception {
    var request = get("/api/v1/reports/cash-flow").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, CashFlowResponse.class);
  }

  private byte[] pdf(String token, String path, String contentDisposition, String... params)
      throws Exception {
    var request = get(path).header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result =
        mockMvc
            .perform(request)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, contentDisposition))
            .andReturn();
    byte[] body = result.getResponse().getContentAsByteArray();
    assertThat(body.length).isGreaterThan(4);
    assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    return body;
  }

  private static String pdfText(byte[] pdf) {
    return new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
  }

  private static int count(String text, String token) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(token, index)) >= 0) {
      count++;
      index += token.length();
    }
    return count;
  }

  private AccountBalanceResponse accountBalance(String token, UUID accountId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + accountId + "/balance")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, AccountBalanceResponse.class);
  }

  private CardReportResponse cards(String token, String... params) throws Exception {
    var request = get("/api/v1/reports/cards").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, CardReportResponse.class);
  }

  private InvoiceReportResponse invoiceReport(String token, UUID invoiceId, String... params)
      throws Exception {
    var request =
        get("/api/v1/reports/invoices/" + invoiceId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, InvoiceReportResponse.class);
  }

  private ResponsibleReportResponse responsibles(String token, String... params) throws Exception {
    var request =
        get("/api/v1/reports/responsibles").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, ResponsibleReportResponse.class);
  }

  private static ResponsibleReportItemResponse itemByKey(
      ResponsibleReportResponse report, String key) {
    return report.items().stream().filter(item -> key.equals(item.key())).findFirst().orElseThrow();
  }

  private CategoryReportResponse categories(String token, String... params) throws Exception {
    var request =
        get("/api/v1/reports/categories").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, CategoryReportResponse.class);
  }

  private static CategoryReportItemResponse itemByCategory(
      CategoryReportResponse report, UUID categoryId) {
    return report.items().stream()
        .filter(item -> categoryId.equals(item.categoryId()))
        .findFirst()
        .orElseThrow();
  }

  private ExpenseReportResponse expenses(String token, String... params) throws Exception {
    var request = get("/api/v1/reports/expenses").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, ExpenseReportResponse.class);
  }

  private IncomeReportResponse incomes(String token, String... params) throws Exception {
    var request = get("/api/v1/reports/incomes").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, IncomeReportResponse.class);
  }

  private Fixture bootstrap(String prefix) throws Exception {
    return bootstrap(prefix, "5000.00", "5000.00");
  }

  private Fixture bootstrap(String prefix, String cardLimit, String accountBalance)
      throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail(prefix), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Moradia");
    AccountResponse account = createAccount(token, accountBalance);
    CreditCardResponse card = createCard(token, cardLimit);
    return new Fixture(token, category.id(), account.id(), card.id());
  }

  private ExpenseResponse createExpense(
      Fixture fx,
      String description,
      String amount,
      String expenseDate,
      String dueDate,
      String paymentMethod,
      UUID accountId,
      String responsibleType,
      String responsibleName,
      int installmentCount)
      throws Exception {
    return createExpense(
        fx,
        fx.categoryId(),
        description,
        amount,
        expenseDate,
        dueDate,
        paymentMethod,
        accountId,
        responsibleType,
        responsibleName,
        installmentCount);
  }

  private ExpenseResponse createExpense(
      Fixture fx,
      UUID categoryId,
      String description,
      String amount,
      String expenseDate,
      String dueDate,
      String paymentMethod,
      UUID accountId,
      String responsibleType,
      String responsibleName,
      int installmentCount)
      throws Exception {
    String accountField = accountId == null ? "" : ",\"accountId\":\"%s\"".formatted(accountId);
    String nameField =
        responsibleName == null ? "" : ",\"responsibleName\":\"%s\"".formatted(responsibleName);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","totalAmount":%s,"expenseDate":"%s","dueDate":"%s","paymentMethod":"%s","responsibleType":"%s","installmentCount":%s%s%s}
                        """
                            .formatted(
                                categoryId,
                                description,
                                amount,
                                expenseDate,
                                dueDate,
                                paymentMethod,
                                responsibleType,
                                installmentCount,
                                accountField,
                                nameField)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private ExpenseResponse createCardExpense(
      Fixture fx, String amount, String expenseDate, int installments) throws Exception {
    return createCardExpense(fx, fx.cardId(), amount, expenseDate, installments);
  }

  private ExpenseResponse createCardExpense(
      Fixture fx, UUID cardId, String amount, String expenseDate, int installments)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"Compra","totalAmount":%s,"expenseDate":"%s","dueDate":"2099-01-01","paymentMethod":"CREDIT_CARD","creditCardId":"%s","responsibleType":"MINE","installmentCount":%s}
                        """
                            .formatted(fx.categoryId(), amount, expenseDate, cardId, installments)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private ExpenseResponse createCardExpense(
      Fixture fx,
      UUID cardId,
      UUID categoryId,
      String description,
      String amount,
      String expenseDate,
      int installments,
      String responsibleType,
      String responsibleName)
      throws Exception {
    String nameField =
        responsibleName == null ? "" : ",\"responsibleName\":\"%s\"".formatted(responsibleName);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","totalAmount":%s,"expenseDate":"%s","dueDate":"2099-01-01","paymentMethod":"CREDIT_CARD","creditCardId":"%s","responsibleType":"%s","installmentCount":%s%s}
                        """
                            .formatted(
                                categoryId,
                                description,
                                amount,
                                expenseDate,
                                cardId,
                                responsibleType,
                                installments,
                                nameField)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private void pay(String token, UUID expenseId, UUID accountId, String amount) throws Exception {
    pay(token, expenseId, accountId, amount, "2026-08-12");
  }

  private void pay(String token, UUID expenseId, UUID accountId, String amount, String paymentDate)
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
                        .formatted(accountId, amount, paymentDate)))
        .andExpect(status().isOk());
  }

  private void payInstallment(
      String token, UUID expenseId, UUID installmentId, UUID accountId, String amount)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseId + "/installments/" + installmentId + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"paymentDate":"2026-08-12"}
                    """
                        .formatted(accountId, amount)))
        .andExpect(status().isOk());
  }

  private ExpenseInstallmentResponse[] listExpenseInstallments(Fixture fx, UUID expenseId)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/expenses/" + expenseId + "/installments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return jsonMapper.readValue(
        result.getResponse().getContentAsString(), ExpenseInstallmentResponse[].class);
  }

  private AgreementResponse createAgreement(
      Fixture fx,
      UUID invoiceId,
      String entryAmount,
      int installmentCount,
      String installmentAmount)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/invoices/" + invoiceId + "/agreements")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"entryAmount":%s,"accountId":"%s","entryPaymentDate":"2026-08-15","installmentCount":%s,"installmentAmount":%s}
                        """
                            .formatted(
                                entryAmount, fx.accountId(), installmentCount, installmentAmount)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AgreementResponse.class);
  }

  private CreditCardInvoiceResponse invoiceByClosing(Fixture fx, LocalDate closingDate)
      throws Exception {
    return invoiceByClosing(fx, fx.cardId(), closingDate);
  }

  private CreditCardInvoiceResponse invoiceByClosing(Fixture fx, UUID cardId, LocalDate closingDate)
      throws Exception {
    return listInvoices(fx, cardId).stream()
        .filter(invoice -> closingDate.equals(invoice.closingDate()))
        .findFirst()
        .orElseThrow();
  }

  private List<CreditCardInvoiceResponse> listInvoices(Fixture fx) throws Exception {
    return listInvoices(fx, fx.cardId());
  }

  private List<CreditCardInvoiceResponse> listInvoices(Fixture fx, UUID cardId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + cardId + "/invoices")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return List.of(
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), CreditCardInvoiceResponse[].class));
  }

  private CreditCardInvoiceResponse currentInvoice(Fixture fx) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + fx.cardId() + "/invoices/current")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, CreditCardInvoiceResponse.class);
  }

  private void createTransfer(
      String token,
      UUID sourceId,
      UUID destinationId,
      String amount,
      String transferDate,
      String description)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceAccountId":"%s","destinationAccountId":"%s","amount":%s,"transferDate":"%s","description":"%s"}
                    """
                        .formatted(sourceId, destinationId, amount, transferDate, description)))
        .andExpect(status().isCreated());
  }

  private void createBalanceAdjustment(
      String token, UUID accountId, String reportedBalance, String adjustmentDate)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/accounts/" + accountId + "/balance-adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"reportedBalance":%s,"adjustmentDate":"%s"}
                    """
                        .formatted(reportedBalance, adjustmentDate)))
        .andExpect(status().isCreated());
  }

  private void closeUntilStatus(Fixture fx, UUID invoiceId, CreditCardInvoiceStatus expected)
      throws Exception {
    for (int i = 0; i < 8; i++) {
      if (getInvoice(fx.token(), invoiceId).status() == expected) {
        return;
      }
      invoiceService.closeDueInvoices();
    }
    if (expected == CreditCardInvoiceStatus.CLOSED) {
      forceInvoiceClosedForTest(fx.cardId(), invoiceId);
    }
    assertThat(getInvoice(fx.token(), invoiceId).status()).isEqualTo(expected);
  }

  private void forceInvoiceClosedForTest(UUID cardId, UUID invoiceId) {
    CreditCardInvoice target = invoiceRepository.findById(invoiceId).orElseThrow();
    UUID userId = target.getUserId();
    for (CreditCardInvoice invoice :
        invoiceRepository.findAllByCreditCard_IdAndUserIdOrderByClosingDateAscIdAsc(
            cardId, userId)) {
      if (invoice.getStatus() == CreditCardInvoiceStatus.OPEN
          && !invoice.getId().equals(invoiceId)) {
        if (TODAY.isBefore(invoice.getClosingDate())) {
          invoice.setStatus(CreditCardInvoiceStatus.SCHEDULED);
        } else {
          invoice.setStatus(CreditCardInvoiceStatus.CLOSED);
        }
        invoiceRepository.save(invoice);
      }
    }
    target = invoiceRepository.findById(invoiceId).orElseThrow();
    if (target.getStatus() == CreditCardInvoiceStatus.SCHEDULED
        || target.getStatus() == CreditCardInvoiceStatus.OPEN) {
      target.setStatus(CreditCardInvoiceStatus.CLOSED);
      invoiceRepository.save(target);
    }
  }

  private CreditCardInvoiceResponse getInvoice(String token, UUID invoiceId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/invoices/" + invoiceId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, CreditCardInvoiceResponse.class);
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

  private IncomeResponse createIncome(
      String token, UUID categoryId, String description, String amount, String expectedDate)
      throws Exception {
    return createIncome(token, categoryId, description, amount, expectedDate, null, null);
  }

  private IncomeResponse createIncome(
      String token,
      UUID categoryId,
      String description,
      String amount,
      String expectedDate,
      String responsibleType,
      String responsibleName)
      throws Exception {
    String extra = "";
    if (responsibleType != null) {
      extra += ",\"responsibleType\":\"%s\"".formatted(responsibleType);
    }
    if (responsibleName != null) {
      extra += ",\"responsibleName\":\"%s\"".formatted(responsibleName);
    }
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","amount":%s,"expectedDate":"%s"%s}
                        """
                            .formatted(categoryId, description, amount, expectedDate, extra)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private IncomeMovementResponse receive(
      String token, UUID incomeId, UUID accountId, String amount, String date) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes/" + incomeId + "/receipts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"accountId":"%s","amount":%s,"date":"%s"}
                        """
                            .formatted(accountId, amount, date)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeMovementResponse.class);
  }

  private void createAccrual(String token, UUID incomeId, String amount, String date)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/accruals")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":%s,"date":"%s"}
                    """
                        .formatted(amount, date)))
        .andExpect(status().isCreated());
  }

  private void reverseMovement(String token, UUID incomeId, UUID movementId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/movements/" + movementId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
  }

  private void cancelIncome(String token, UUID incomeId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
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

  private CreditCardResponse createCard(String token, String limit) throws Exception {
    return createCard(token, "Nubank", "Ederson", "9999", limit, 10, 20);
  }

  private CreditCardResponse createCard(
      String token,
      String name,
      String holderName,
      String lastFourDigits,
      String limit,
      int closingDay,
      int dueDay)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/credit-cards")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","holderName":"%s","lastFourDigits":"%s","creditLimit":%s,"closingDay":%s,"dueDay":%s}
                        """
                            .formatted(
                                name, holderName, lastFourDigits, limit, closingDay, dueDay)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, CreditCardResponse.class);
  }

  private void payInvoice(Fixture fx, UUID invoiceId, String amount, String paymentDate)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/invoices/" + invoiceId + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"paymentDate":"%s"}
                    """
                        .formatted(fx.accountId(), amount, paymentDate)))
        .andExpect(status().isOk());
  }

  private void createManualCredit(Fixture fx, String amount) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/credit-cards/" + fx.cardId() + "/credits")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":%s,"reason":"crédito de teste"}
                    """
                        .formatted(amount)))
        .andExpect(status().isCreated());
  }

  private void createInstallmentAdjustment(
      Fixture fx, UUID expenseId, UUID installmentId, String amount) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses/"
                    + expenseId
                    + "/installments/"
                    + installmentId
                    + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"DISCOUNT","amount":%s,"reason":"ajuste de parcela"}
                    """
                        .formatted(amount)))
        .andExpect(status().isCreated());
  }

  private void createInvoiceAdjustment(Fixture fx, UUID invoiceId, String amount) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/invoices/" + invoiceId + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"DISCOUNT","amount":%s,"reason":"ajuste de fatura"}
                    """
                        .formatted(amount)))
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

  private <T> T read(MvcResult result, Class<T> type) throws Exception {
    return jsonMapper.readValue(result.getResponse().getContentAsString(), type);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private record Fixture(String token, UUID categoryId, UUID accountId, UUID cardId) {}
}
