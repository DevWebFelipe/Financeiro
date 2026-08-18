package br.com.financialcontrol.projections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementResponse;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import br.com.financialcontrol.projections.dto.ProjectionEventResponse;
import br.com.financialcontrol.projections.dto.ProjectionMonthResponse;
import br.com.financialcontrol.projections.dto.ProjectionResponse;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class ProjectionApiTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);
  private static final String TODAY_TEXT = "2026-08-17";

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
    mockMvc.perform(get("/api/v1/projections")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldKeepProjectedBalanceConstantWhenThereAreNoFutureEvents() throws Exception {
    Fixture fx = bootstrap("constant", "10000.00");
    ProjectionResponse projection = projectAugust(fx.token());
    assertThat(projection.summary().currentBalance()).isEqualByComparingTo("10000.00");
    assertThat(projection.summary().projectedFinalBalance()).isEqualByComparingTo("10000.00");
    assertThat(projection.months()).hasSize(1);
    assertThat(projection.months().getFirst().closingBalance()).isEqualByComparingTo("10000.00");
    assertThat(projection.events().items()).isEmpty();
    assertThat(projection.undatedEvents()).isEmpty();
  }

  @Test
  void shouldProjectCurrentBalancePlusFutureIncome() throws Exception {
    Fixture fx = bootstrap("income", "1000.00");
    IncomeResponse income = createIncome(fx, "Salario", "400.00", "2026-08-25");
    ProjectionResponse projection = projectAugust(fx.token());
    assertThat(projection.summary().currentBalance()).isEqualByComparingTo("1000.00");
    assertThat(projection.summary().projectedIncome()).isEqualByComparingTo("400.00");
    assertThat(projection.summary().projectedFinalBalance()).isEqualByComparingTo("1400.00");
    assertThat(sourceIds(projection)).contains(income.id());
    ProjectionEventResponse event = event(projection, income.id());
    assertThat(event.type()).isEqualTo(ProjectionEventType.INCOME);
    assertThat(event.direction()).isEqualTo(ProjectionDirection.IN);
    assertThat(event.accountAssignment()).isEqualTo(ProjectionAccountAssignment.UNASSIGNED);
    assertThat(event.amount()).isEqualByComparingTo("400.00");
  }

  @Test
  void shouldProjectCurrentBalancePlusFutureExpense() throws Exception {
    Fixture fx = bootstrap("expense", "1000.00");
    ExpenseResponse expense = createExpense(fx, "NONE", null, "Internet", "250.00", "2026-08-28");
    ProjectionResponse projection = projectAugust(fx.token());
    assertThat(projection.summary().projectedExpense()).isEqualByComparingTo("250.00");
    assertThat(projection.summary().projectedFinalBalance()).isEqualByComparingTo("750.00");
    assertThat(sourceIds(projection)).contains(expense.installmentId());
    assertThat(event(projection, expense.installmentId()).type())
        .isEqualTo(ProjectionEventType.EXPENSE);
  }

  @Test
  void shouldProjectIncomeAndExpenseTogether() throws Exception {
    Fixture fx = bootstrap("both", "1000.00");
    createIncome(fx, "Salario", "1000.00", "2026-08-20");
    createExpense(fx, "ACCOUNT", fx.accountId(), "Aluguel", "5000.00", "2026-08-25");
    ProjectionResponse projection = projectAugust(fx.token());
    assertThat(projection.summary().projectedIncome()).isEqualByComparingTo("1000.00");
    assertThat(projection.summary().projectedExpense()).isEqualByComparingTo("5000.00");
    assertThat(projection.summary().projectedFinalBalance()).isEqualByComparingTo("-3000.00");
    assertThat(projection.summary().minimumProjectedBalance()).isEqualByComparingTo("-3000.00");
    assertThat(projection.months().getFirst().negative()).isTrue();
  }

  @Test
  void shouldProjectInstallmentsIndividuallyAndUseRemainingAfterPartialPayment() throws Exception {
    Fixture fx = bootstrap("installments", "5000.00");
    ExpenseResponse split =
        createExpense(fx, "ACCOUNT", fx.accountId(), "Parcelado", "300.00", "2026-08-20", 3);
    List<ExpenseInstallmentResponse> installments = listInstallments(fx.token(), split.id());
    payInstallment(fx, split.id(), installments.getFirst().id(), "40.00");

    ProjectionResponse august = projectAugust(fx.token());
    assertThat(sourceIds(august)).contains(installments.getFirst().id());
    assertThat(sourceIds(august)).doesNotContain(installments.get(1).id());
    assertThat(event(august, installments.getFirst().id()).amount()).isEqualByComparingTo("60.00");

    ProjectionResponse threeMonths =
        project(fx.token(), "year", "2026", "month", "8", "months", "3", "size", "100");
    assertThat(sourceIds(threeMonths))
        .contains(installments.getFirst().id(), installments.get(1).id(), installments.get(2).id());
    assertThat(threeMonths.months()).hasSize(3);
    assertThat(threeMonths.months().get(0).closingBalance())
        .isEqualByComparingTo(threeMonths.months().get(1).openingBalance());
  }

  @Test
  void shouldUseOfficialRemainingAfterDiscountSurchargeAndReverse() throws Exception {
    Fixture fx = bootstrap("adjust", "2000.00");
    ExpenseResponse discounted =
        createExpense(fx, "ACCOUNT", fx.accountId(), "Desconto", "100.00", "2026-08-22");
    createAdjustment(fx, discounted, "DISCOUNT", "10.00");
    assertThat(event(projectAugust(fx.token()), discounted.installmentId()).amount())
        .isEqualByComparingTo("90.00");

    ExpenseResponse surcharged =
        createExpense(fx, "ACCOUNT", fx.accountId(), "Acrescimo", "100.00", "2026-08-23");
    createAdjustment(fx, surcharged, "SURCHARGE", "15.00");
    assertThat(event(projectAugust(fx.token()), surcharged.installmentId()).amount())
        .isEqualByComparingTo("115.00");

    ExpenseResponse reversed =
        createExpense(fx, "ACCOUNT", fx.accountId(), "EstornoPgto", "80.00", "2026-08-24");
    pay(fx.token(), reversed.id(), fx.accountId(), "80.00");
    assertThat(sourceIds(projectAugust(fx.token()))).doesNotContain(reversed.installmentId());
    reverseFirstPayment(fx.token(), reversed.id());
    assertThat(event(projectAugust(fx.token()), reversed.installmentId()).amount())
        .isEqualByComparingTo("80.00");
  }

  @Test
  void shouldProjectInvoiceInsteadOfCardPurchaseAndRespectCreditsAndPartialPayment()
      throws Exception {
    CardFx card = bootstrapCard("card-dup", "5000.00", "2000.00");
    ExpenseResponse purchase = createCardExpense(card, "180.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(card);
    ProjectionResponse before = project(card.token(), "year", "2026", "month", "8", "months", "12");
    assertThat(sourceIds(before)).contains(invoice.id());
    assertThat(sourceIds(before)).doesNotContain(purchase.id(), purchase.installmentId());
    assertThat(event(before, invoice.id()).type())
        .isEqualTo(ProjectionEventType.CREDIT_CARD_INVOICE);
    assertThat(event(before, invoice.id()).amount()).isEqualByComparingTo("180.00");

    mockMvc
        .perform(
            post("/api/v1/credit-cards/" + card.cardId() + "/credits")
                .header(HttpHeaders.AUTHORIZATION, bearer(card.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":30.00,"reason":"Credito"}
                    """))
        .andExpect(status().isCreated());
    CreditCardInvoiceResponse afterCredit = getInvoice(card.token(), invoice.id());
    ProjectionResponse withCredit =
        project(card.token(), "year", "2026", "month", "8", "months", "12");
    assertThat(event(withCredit, invoice.id()).amount())
        .isEqualByComparingTo(afterCredit.remainingAmount());

    payInvoice(card, invoice.id(), "50.00");
    CreditCardInvoiceResponse afterPay = getInvoice(card.token(), invoice.id());
    ProjectionResponse partial =
        project(card.token(), "year", "2026", "month", "8", "months", "12");
    if (afterPay.remainingAmount().compareTo(BigDecimal.ZERO) > 0) {
      assertThat(event(partial, invoice.id()).amount())
          .isEqualByComparingTo(afterPay.remainingAmount());
    } else {
      assertThat(sourceIds(partial)).doesNotContain(invoice.id());
    }
  }

  @Test
  void shouldExcludeSettledAgreementInvoiceAndKeepFutureAgreementObligation() throws Exception {
    CardFx card = bootstrapCard("agreement", "5000.00", "2000.00");
    createCardExpense(card, "1000.00", "2026-07-05", 1);
    CreditCardInvoiceResponse source = currentInvoice(card);
    closeUntilStatus(card.token(), source.id(), CreditCardInvoiceStatus.CLOSED);
    AgreementResponse agreement = createAgreement(card, source.id(), "400.00", 10, "120.00");
    CreditCardInvoiceResponse settled = getInvoice(card.token(), source.id());
    assertThat(settled.status()).isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);

    ProjectionResponse projection =
        project(card.token(), "year", "2026", "month", "8", "months", "12", "size", "100");
    assertThat(sourceIds(projection)).doesNotContain(source.id());
    List<UUID> futureInvoiceIds =
        listInvoices(card).stream()
            .filter(item -> item.status() != CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT)
            .filter(item -> item.remainingAmount().compareTo(BigDecimal.ZERO) > 0)
            .map(CreditCardInvoiceResponse::id)
            .toList();
    assertThat(futureInvoiceIds).isNotEmpty();
    assertThat(sourceIds(projection)).containsAll(futureInvoiceIds);
    assertThat(agreement.installments()).isNotEmpty();
  }

  @Test
  void shouldIncludeOverdueIncomeAndExpenseInTheFirstPeriod() throws Exception {
    Fixture fx = bootstrap("overdue", "500.00");
    IncomeResponse overdueIncome = createIncome(fx, "Atraso", "80.00", "2026-08-10");
    ExpenseResponse overdueExpense =
        createExpense(fx, "NONE", null, "Boleto", "30.00", "2026-08-05");
    ProjectionResponse projection = projectAugust(fx.token());
    assertThat(event(projection, overdueIncome.id()).overdue()).isTrue();
    assertThat(event(projection, overdueExpense.installmentId()).overdue()).isTrue();
    assertThat(projection.months().getFirst().totalIncome()).isEqualByComparingTo("80.00");
    assertThat(projection.months().getFirst().totalExpense()).isEqualByComparingTo("30.00");
  }

  @Test
  void shouldExcludeCancelledAndRefundedExpensesAndReceivedIncomes() throws Exception {
    Fixture fx = bootstrap("exclude", "2000.00");
    IncomeResponse received = createIncome(fx, "Recebida", "100.00", "2026-08-10");
    receive(fx, received.id(), "100.00", TODAY_TEXT);
    IncomeResponse cancelledIncome = createIncome(fx, "Cancelada", "70.00", "2026-08-25");
    mockMvc
        .perform(
            post("/api/v1/incomes/" + cancelledIncome.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    ExpenseResponse cancelled =
        createExpense(fx, "ACCOUNT", fx.accountId(), "Cancelada", "40.00", "2026-08-26");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());
    ExpenseResponse refunded =
        createExpense(fx, "ACCOUNT", fx.accountId(), "Estornada", "50.00", "2026-08-27");
    pay(fx.token(), refunded.id(), fx.accountId(), "50.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refunded.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk());

    ProjectionResponse projection = projectAugust(fx.token());
    assertThat(sourceIds(projection))
        .doesNotContain(
            received.id(),
            cancelledIncome.id(),
            cancelled.installmentId(),
            refunded.installmentId());
    assertThat(projection.summary().currentBalance())
        .isEqualByComparingTo(balance(fx.token(), fx.accountId()).totalBalance());
  }

  @Test
  void shouldNotProjectHistoricalTransfersAndKeepConsolidatedNetZero() throws Exception {
    String token = registerAndLogin(uniqueEmail("transfer"));
    AccountResponse source = createAccount(token, "Origem", "BANK_ACCOUNT", "1000.00");
    AccountResponse destination = createAccount(token, "Destino", "BANK_ACCOUNT", "200.00");
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceAccountId":"%s","destinationAccountId":"%s","amount":300.00,"transferDate":"%s","description":"Entre contas"}
                    """
                        .formatted(source.id(), destination.id(), TODAY_TEXT)))
        .andExpect(status().isCreated());
    ProjectionResponse consolidated = projectAugust(token);
    assertThat(consolidated.summary().currentBalance()).isEqualByComparingTo("1200.00");
    assertThat(consolidated.summary().projectedFinalBalance()).isEqualByComparingTo("1200.00");
    assertThat(consolidated.events().items())
        .noneMatch(event -> event.type() == ProjectionEventType.TRANSFER);
    ProjectionResponse origin = projectAugust(token, source.id());
    assertThat(origin.summary().currentBalance()).isEqualByComparingTo("700.00");
    assertThat(origin.summary().projectedFinalBalance()).isEqualByComparingTo("700.00");
  }

  @Test
  void shouldConsolidateMultipleAccountsAndIgnoreUnassignedEventsOnAccountFilter()
      throws Exception {
    String token = registerAndLogin(uniqueEmail("multi"));
    AccountResponse first = createAccount(token, "A", "BANK_ACCOUNT", "1000.00");
    AccountResponse second = createAccount(token, "B", "BANK_ACCOUNT", "2000.00");
    UUID incomeCategory = createIncomeCategory(token, "Salario").id();
    UUID expenseCategory = createExpenseCategory(token, "Moradia").id();
    IncomeResponse income = createIncome(token, incomeCategory, "Bonus", "150.00", "2026-08-22");
    ExpenseResponse none =
        createExpense(token, expenseCategory, "NONE", null, "Avulsa", "40.00", "2026-08-23", 1);

    ProjectionResponse consolidated = projectAugust(token);
    assertThat(consolidated.summary().currentBalance()).isEqualByComparingTo("3000.00");
    assertThat(consolidated.summary().projectedFinalBalance()).isEqualByComparingTo("3110.00");
    assertThat(event(consolidated, income.id()).accountAssignment())
        .isEqualTo(ProjectionAccountAssignment.UNASSIGNED);
    assertThat(event(consolidated, none.installmentId()).accountAssignment())
        .isEqualTo(ProjectionAccountAssignment.UNASSIGNED);

    ProjectionResponse filtered = projectAugust(token, first.id());
    assertThat(filtered.summary().currentBalance()).isEqualByComparingTo("1000.00");
    assertThat(filtered.summary().projectedFinalBalance()).isEqualByComparingTo("1000.00");
    assertThat(sourceIds(filtered)).doesNotContain(income.id(), none.installmentId());
    assertThat(filtered.summary().currentBalance())
        .isNotEqualByComparingTo(consolidated.summary().projectedFinalBalance());
  }

  @Test
  void shouldExposeReservedAmountWithoutReducingClosingBalance() throws Exception {
    Fixture fx = bootstrap("goals", "1000.00");
    FinancialGoalResponse goal = createGoal(fx.token(), fx.accountId(), "Viagem", "500.00");
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goal.id() + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":200.00,"contributionDate":"%s","notes":"Aporte"}
                    """
                        .formatted(TODAY_TEXT)))
        .andExpect(status().isCreated());
    ProjectionResponse projection = projectAugust(fx.token());
    assertThat(projection.summary().currentBalance()).isEqualByComparingTo("1000.00");
    assertThat(projection.summary().projectedFinalBalance()).isEqualByComparingTo("1000.00");
    assertThat(projection.summary().reservedAmount()).isEqualByComparingTo("200.00");
    assertThat(projection.summary().availableProjectedBalance()).isEqualByComparingTo("800.00");
    assertThat(projection.months().getFirst().closingBalance()).isEqualByComparingTo("1000.00");
    assertThat(projection.months().getFirst().availableProjectedBalance())
        .isEqualByComparingTo("800.00");
  }

  @Test
  void shouldReturnTwelveMonthsByDefaultAndRejectHorizonAboveTwelve() throws Exception {
    Fixture fx = bootstrap("horizon", "10.00");
    ProjectionResponse projection = project(fx.token(), "size", "100");
    assertThat(projection.startDate()).isEqualTo(TODAY);
    assertThat(projection.endDate()).isEqualTo(LocalDate.of(2027, 7, 31));
    assertThat(projection.months()).hasSize(12);
    assertThat(projection.months().getFirst().period()).isEqualTo("2026-08");
    assertThat(projection.months().getLast().period()).isEqualTo("2027-07");
    assertThat(projection.quarters().stream().map(item -> item.period()).toList())
        .contains("2026-Q4", "2027-Q1", "2027-Q2");

    mockMvc
        .perform(
            get("/api/v1/projections")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("months", "13"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/projections")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-01-01")
                .param("endDate", "2027-01-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnCalendarQuarterWhenTheRangeContainsACompleteQuarter() throws Exception {
    Fixture fx = bootstrap("quarter", "0.00");
    createIncome(fx, "Outubro", "10.00", "2026-10-02");
    ProjectionResponse projection =
        project(fx.token(), "year", "2026", "month", "10", "months", "3", "size", "100");
    assertThat(projection.months())
        .extracting(ProjectionMonthResponse::period)
        .containsExactly("2026-10", "2026-11", "2026-12");
    assertThat(projection.quarters()).hasSize(1);
    assertThat(projection.quarters().getFirst().period()).isEqualTo("2026-Q4");
    assertThat(projection.quarters().getFirst().totalIncome()).isEqualByComparingTo("10.00");
  }

  @Test
  void shouldIsolateUsersAndHideForeignAccountId() throws Exception {
    Fixture owner = bootstrap("iso-a", "100.00");
    Fixture other = bootstrap("iso-b", "999.00");
    IncomeResponse ownerIncome = createIncome(owner, "Minha", "50.00", "2026-08-20");
    IncomeResponse otherIncome = createIncome(other, "Alheia", "80.00", "2026-08-20");
    ProjectionResponse mine = projectAugust(owner.token());
    assertThat(sourceIds(mine)).contains(ownerIncome.id());
    assertThat(sourceIds(mine)).doesNotContain(otherIncome.id());
    assertThat(mine.summary().currentBalance()).isEqualByComparingTo("100.00");

    ProjectionResponse hidden = projectAugust(owner.token(), other.accountId());
    assertThat(hidden.summary().currentBalance()).isEqualByComparingTo("0.00");
    assertThat(hidden.events().items()).isEmpty();
    assertThat(sourceIds(hidden)).doesNotContain(ownerIncome.id(), otherIncome.id());
  }

  @Test
  void shouldRejectEntirelyPastPeriodUnknownParamsAndConflictingFilters() throws Exception {
    Fixture fx = bootstrap("invalid", "10.00");
    mockMvc
        .perform(
            get("/api/v1/projections")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-06-01")
                .param("endDate", "2026-06-30"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/projections")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("includeEvents", "true"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/projections")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("startDate", "2026-08-17")
                .param("endDate", "2026-08-31")
                .param("year", "2026")
                .param("month", "8"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/projections")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("year", "2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/projections")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  private ProjectionResponse projectAugust(String token) throws Exception {
    return project(token, "year", "2026", "month", "8", "size", "100");
  }

  private ProjectionResponse projectAugust(String token, UUID accountId) throws Exception {
    return project(
        token, "year", "2026", "month", "8", "accountId", accountId.toString(), "size", "100");
  }

  private ProjectionResponse project(String token, String... params) throws Exception {
    MockHttpServletRequestBuilder request =
        get("/api/v1/projections").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, ProjectionResponse.class);
  }

  private Fixture bootstrap(String prefix, String initial) throws Exception {
    String token = registerAndLogin(uniqueEmail(prefix));
    UUID incomeCategory = createIncomeCategory(token, "Salario").id();
    UUID expenseCategory = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "Nubank", "BANK_ACCOUNT", initial).id();
    return new Fixture(token, incomeCategory, expenseCategory, accountId);
  }

  private CardFx bootstrapCard(String prefix, String limit, String initial) throws Exception {
    String token = registerAndLogin(uniqueEmail(prefix));
    UUID categoryId = createExpenseCategory(token, "Cartao").id();
    UUID accountId = createAccount(token, "Banco", "BANK_ACCOUNT", initial).id();
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/credit-cards")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","holderName":"Alice","creditLimit":%s,"closingDay":10,"dueDay":20}
                        """
                            .formatted(limit)))
            .andExpect(status().isCreated())
            .andReturn();
    CreditCardResponse card = read(created, CreditCardResponse.class);
    return new CardFx(token, categoryId, accountId, card.id());
  }

  private IncomeResponse createIncome(Fixture fx, String description, String amount, String date)
      throws Exception {
    return createIncome(fx.token(), fx.incomeCategoryId(), description, amount, date);
  }

  private IncomeResponse createIncome(
      String token, UUID categoryId, String description, String amount, String date)
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
                            .formatted(categoryId, description, amount, date)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private void receive(Fixture fx, UUID incomeId, String amount, String date) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/incomes/" + incomeId + "/receipts")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"date":"%s"}
                    """
                        .formatted(fx.accountId(), amount, date)))
        .andExpect(status().isCreated());
  }

  private ExpenseResponse createExpense(
      Fixture fx,
      String paymentMethod,
      UUID accountId,
      String description,
      String amount,
      String dueDate)
      throws Exception {
    return createExpense(
        fx.token(),
        fx.expenseCategoryId(),
        paymentMethod,
        accountId,
        description,
        amount,
        dueDate,
        1);
  }

  private ExpenseResponse createExpense(
      Fixture fx,
      String paymentMethod,
      UUID accountId,
      String description,
      String amount,
      String dueDate,
      int installments)
      throws Exception {
    return createExpense(
        fx.token(),
        fx.expenseCategoryId(),
        paymentMethod,
        accountId,
        description,
        amount,
        dueDate,
        installments);
  }

  private ExpenseResponse createExpense(
      String token,
      UUID categoryId,
      String paymentMethod,
      UUID accountId,
      String description,
      String amount,
      String dueDate,
      int installments)
      throws Exception {
    String accountField = accountId == null ? "" : ",\"accountId\":\"" + accountId + "\"";
    String countField = installments == 1 ? "" : ",\"installmentCount\":" + installments;
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","totalAmount":%s,"expenseDate":"2026-08-01","dueDate":"%s","paymentMethod":"%s","responsibleType":"MINE"%s%s}
                        """
                            .formatted(
                                categoryId,
                                description,
                                amount,
                                dueDate,
                                paymentMethod,
                                accountField,
                                countField)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
  }

  private ExpenseResponse createCardExpense(
      CardFx fx, String amount, String expenseDate, int installments) throws Exception {
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
                            .formatted(
                                fx.categoryId(), amount, expenseDate, fx.cardId(), installments)))
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
                .content(
                    """
                    {"accountId":"%s","amount":%s,"paymentDate":"2026-08-12"}
                    """
                        .formatted(accountId, amount)))
        .andExpect(status().isOk());
  }

  private void payInstallment(Fixture fx, UUID expenseId, UUID installmentId, String amount)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expenseId + "/installments/" + installmentId + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"paymentDate":"2026-08-12"}
                    """
                        .formatted(fx.accountId(), amount)))
        .andExpect(status().isOk());
  }

  private void createAdjustment(Fixture fx, ExpenseResponse expense, String type, String amount)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses/"
                    + expense.id()
                    + "/installments/"
                    + expense.installmentId()
                    + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"%s","amount":%s,"reason":"Ajuste"}
                    """
                        .formatted(type, amount)))
        .andExpect(status().isCreated());
  }

  private void reverseFirstPayment(String token, UUID expenseId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/expenses/" + expenseId + "/payments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    PaymentResponse[] payments =
        jsonMapper.readValue(result.getResponse().getContentAsString(), PaymentResponse[].class);
    mockMvc
        .perform(
            post("/api/v1/payments/" + payments[0].id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
  }

  private List<ExpenseInstallmentResponse> listInstallments(String token, UUID expenseId)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/expenses/" + expenseId + "/installments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return List.of(
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), ExpenseInstallmentResponse[].class));
  }

  private CreditCardInvoiceResponse currentInvoice(CardFx fx) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + fx.cardId() + "/invoices/current")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, CreditCardInvoiceResponse.class);
  }

  private List<CreditCardInvoiceResponse> listInvoices(CardFx fx) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + fx.cardId() + "/invoices")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return List.of(
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), CreditCardInvoiceResponse[].class));
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

  private void payInvoice(CardFx fx, UUID invoiceId, String amount) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/invoices/" + invoiceId + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId":"%s","amount":%s,"paymentDate":"%s"}
                    """
                        .formatted(fx.accountId(), amount, TODAY_TEXT)))
        .andExpect(status().isOk());
  }

  private AgreementResponse createAgreement(
      CardFx fx, UUID invoiceId, String entry, int count, String installmentAmount)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/invoices/" + invoiceId + "/agreements")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"entryAmount":%s,"accountId":"%s","entryPaymentDate":"%s","installmentCount":%s,"installmentAmount":%s}
                        """
                            .formatted(
                                entry, fx.accountId(), TODAY_TEXT, count, installmentAmount)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AgreementResponse.class);
  }

  private void closeUntilStatus(String token, UUID invoiceId, CreditCardInvoiceStatus expected)
      throws Exception {
    for (int i = 0; i < 8; i++) {
      if (getInvoice(token, invoiceId).status() == expected) {
        return;
      }
      invoiceService.closeDueInvoices();
    }
    CreditCardInvoice target = invoiceRepository.findById(invoiceId).orElseThrow();
    UUID cardId = target.getCreditCard().getId();
    UUID userId = target.getUserId();
    LocalDate today = TODAY;
    for (CreditCardInvoice invoice :
        invoiceRepository.findAllByCreditCard_IdAndUserIdOrderByClosingDateAscIdAsc(
            cardId, userId)) {
      if (invoice.getStatus() == CreditCardInvoiceStatus.OPEN
          && !invoice.getId().equals(invoiceId)) {
        invoice.setStatus(
            today.isBefore(invoice.getClosingDate())
                ? CreditCardInvoiceStatus.SCHEDULED
                : CreditCardInvoiceStatus.CLOSED);
        invoiceRepository.save(invoice);
      }
    }
    target = invoiceRepository.findById(invoiceId).orElseThrow();
    if (target.getStatus() == CreditCardInvoiceStatus.SCHEDULED
        || target.getStatus() == CreditCardInvoiceStatus.OPEN) {
      target.setStatus(CreditCardInvoiceStatus.CLOSED);
      invoiceRepository.save(target);
    }
    assertThat(getInvoice(token, invoiceId).status()).isEqualTo(expected);
  }

  private FinancialGoalResponse createGoal(String token, UUID accountId, String name, String target)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/financial-goals")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"accountId":"%s","name":"%s","description":"Obs","targetAmount":%s,"targetDate":null}
                        """
                            .formatted(accountId, name, target)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, FinancialGoalResponse.class);
  }

  private CategoryResponse createIncomeCategory(String token, String name) throws Exception {
    return createCategory(token, name, "INCOME");
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

  private AccountBalanceResponse balance(String token, UUID accountId) throws Exception {
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

  private List<UUID> sourceIds(ProjectionResponse projection) {
    return projection.events().items().stream().map(ProjectionEventResponse::sourceId).toList();
  }

  private ProjectionEventResponse event(ProjectionResponse projection, UUID sourceId) {
    return projection.events().items().stream()
        .filter(item -> item.sourceId().equals(sourceId))
        .findFirst()
        .orElseThrow();
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

  private record Fixture(
      String token, UUID incomeCategoryId, UUID expenseCategoryId, UUID accountId) {}

  private record CardFx(String token, UUID categoryId, UUID accountId, UUID cardId) {}
}
