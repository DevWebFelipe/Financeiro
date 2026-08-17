package br.com.financialcontrol.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.payables.dto.PayableItemResponse;
import br.com.financialcontrol.payables.dto.PayablePageResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
class PayablesApiTest {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private CreditCardInvoiceService invoiceService;
  @Autowired private CreditCardInvoiceRepository invoiceRepository;

  @Test
  void shouldListNoneAndAccountOpenAndExcludePaidCancelledRefunded() throws Exception {
    String token = registerAndLogin(uniqueEmail("eligibility"));
    UUID categoryId = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "2000.00").id();

    ExpenseResponse noneOpen =
        createExpense(token, categoryId, null, "NONE", "Luz", "80.00", "2026-09-10");
    ExpenseResponse accountOpen =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Aluguel", "150.00", "2026-09-12");
    ExpenseResponse paid =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Pago", "40.00", "2026-09-15");
    pay(token, paid.id(), accountId, "40.00");
    ExpenseResponse cancelled =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Cancelado", "25.00", "2026-09-16");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + cancelled.id() + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
    ExpenseResponse refunded =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Estornado", "30.00", "2026-09-17");
    pay(token, refunded.id(), accountId, "30.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + refunded.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    PayablePageResponse page = listPayables(token);
    assertThat(ids(page)).contains(noneOpen.installmentId(), accountOpen.installmentId());
    assertThat(ids(page))
        .doesNotContain(paid.installmentId(), cancelled.installmentId(), refunded.installmentId());
    assertThat(item(page, noneOpen.installmentId()).type()).isEqualTo(PayableItemType.INSTALLMENT);
    assertThat(item(page, noneOpen.installmentId()).paymentMethod().name()).isEqualTo("NONE");
    assertThat(item(page, accountOpen.installmentId()).remainingAmount())
        .isEqualByComparingTo("150.00");
  }

  @Test
  void shouldReturnInstallmentRemainingNotExpenseTotalForPartialAndNGreaterThanOne()
      throws Exception {
    String token = registerAndLogin(uniqueEmail("nplus"));
    UUID categoryId = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "5000.00").id();

    ExpenseResponse partial =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Parcial", "100.00", "2026-09-20");
    pay(token, partial.id(), accountId, "40.00");

    ExpenseResponse split =
        createExpense(
            token, categoryId, accountId, "ACCOUNT", "Parcelado", "300.00", "2026-09-10", 3);

    PayableItemResponse partialLine = item(listPayables(token), partial.installmentId());
    assertThat(partialLine.originalAmount()).isEqualByComparingTo("100.00");
    assertThat(partialLine.paidAmount()).isEqualByComparingTo("40.00");
    assertThat(partialLine.remainingAmount()).isEqualByComparingTo("60.00");
    assertThat(partialLine.status()).isEqualTo("PARTIALLY_PAID");

    PayablePageResponse october = listPayables(token, "year", "2026", "month", "10");
    assertThat(october.items()).hasSize(1);
    assertThat(october.items().getFirst().name()).isEqualTo("Parcelado");
    assertThat(october.items().getFirst().remainingAmount()).isEqualByComparingTo("100.00");
    assertThat(october.totalRemaining()).isEqualByComparingTo("100.00");
    assertThat(october.totalOriginal()).isEqualByComparingTo("100.00");

    List<ExpenseInstallmentResponse> installments = listInstallments(token, split.id());
    assertThat(installments).hasSize(3);
    PayablePageResponse september = listPayables(token, "year", "2026", "month", "9");
    assertThat(ids(september)).contains(installments.getFirst().id());
    assertThat(ids(september)).doesNotContain(installments.get(1).id());
  }

  @Test
  void shouldRepresentCardAsInvoiceWithoutInstallmentOrExpenseLine() throws Exception {
    CardFx fx = bootstrapCard(uniqueEmail("card-line"), "5000.00", "2000.00");
    ExpenseResponse purchase = createCardExpense(fx, "180.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);

    PayablePageResponse page = listPayables(fx.token());
    assertThat(page.items()).hasSize(1);
    PayableItemResponse line = page.items().getFirst();
    assertThat(line.type()).isEqualTo(PayableItemType.INVOICE);
    assertThat(line.id()).isEqualTo(invoice.id());
    assertThat(line.creditCardId()).isEqualTo(fx.cardId());
    assertThat(line.remainingAmount()).isEqualByComparingTo("180.00");
    assertThat(ids(page)).doesNotContain(purchase.id(), purchase.installmentId());
  }

  @Test
  void shouldIncludeScheduledInvoiceAndNeverMarkItOverdue() throws Exception {
    CardFx fx = bootstrapCard(uniqueEmail("scheduled"), "8000.00", "3000.00");
    createCardExpense(fx, "300.00", "2026-08-11", 3);
    List<CreditCardInvoiceResponse> invoices = listInvoices(fx);
    List<CreditCardInvoiceResponse> scheduled =
        invoices.stream()
            .filter(invoice -> invoice.status() == CreditCardInvoiceStatus.SCHEDULED)
            .toList();
    assertThat(scheduled).isNotEmpty();

    PayablePageResponse page = listPayables(fx.token());
    for (CreditCardInvoiceResponse invoice : scheduled) {
      if (invoice.remainingAmount().compareTo(BigDecimal.ZERO) > 0) {
        PayableItemResponse line = item(page, invoice.id());
        assertThat(line.type()).isEqualTo(PayableItemType.INVOICE);
        assertThat(line.status()).isEqualTo("SCHEDULED");
        assertThat(line.overdue()).isFalse();
      }
    }
  }

  @Test
  void shouldExcludeInvoiceWithRemainingZero() throws Exception {
    CardFx fx = bootstrapCard(uniqueEmail("zero-inv"), "5000.00", "2000.00");
    createCardExpense(fx, "50.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);
    payInvoice(fx, invoice.id(), "50.00");
    CreditCardInvoiceResponse after = getInvoice(fx.token(), invoice.id());
    assertThat(after.remainingAmount()).isEqualByComparingTo("0.00");

    PayablePageResponse page = listPayables(fx.token());
    assertThat(ids(page)).doesNotContain(invoice.id());
    assertThat(page.totalRemaining()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldReflectAgreementSettlementWithoutDuplicatingOriginalInvoice() throws Exception {
    CardFx fx = bootstrapCard(uniqueEmail("agreement"), "5000.00", "3000.00");
    createCardExpense(fx, "1000.00", "2026-07-05", 1);
    CreditCardInvoiceResponse source = currentInvoice(fx);
    closeUntilStatus(fx.token(), source.id(), CreditCardInvoiceStatus.CLOSED);
    source = getInvoice(fx.token(), source.id());

    mockMvc
        .perform(
            post("/api/v1/invoices/" + source.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"entryAmount":100.00,"accountId":"%s","entryPaymentDate":"2026-08-15","installmentCount":10,"installmentAmount":100.00}
                    """
                        .formatted(fx.accountId())))
        .andExpect(status().isCreated());

    CreditCardInvoiceResponse settled = getInvoice(fx.token(), source.id());
    assertThat(settled.status()).isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
    assertThat(settled.remainingAmount()).isEqualByComparingTo("0.00");

    PayablePageResponse page = listPayables(fx.token(), "size", "100");
    assertThat(ids(page)).doesNotContain(source.id());
    assertThat(page.items())
        .allMatch(item -> item.type() == PayableItemType.INVOICE)
        .allMatch(item -> item.remainingAmount().compareTo(BigDecimal.ZERO) > 0);
  }

  @Test
  void shouldComputeOverdueFromLineDueDate() throws Exception {
    String token = registerAndLogin(uniqueEmail("overdue"));
    UUID categoryId = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "500.00").id();
    ExpenseResponse overdueExpense =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Vencida", "20.00", "2026-08-01");
    ExpenseResponse future =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Futura", "15.00", "2026-12-01");

    PayablePageResponse page = listPayables(token);
    assertThat(item(page, overdueExpense.installmentId()).overdue()).isTrue();
    assertThat(item(page, future.installmentId()).overdue()).isFalse();

    PayablePageResponse onlyOverdue = listPayables(token, "overdue", "true");
    assertThat(ids(onlyOverdue)).containsExactly(overdueExpense.installmentId());

    CardFx fx = bootstrapCard(uniqueEmail("invoice-overdue"), "3000.00", "500.00");
    createCardExpense(fx, "20.00", "2026-07-05", 1);
    CreditCardInvoiceResponse july = currentInvoice(fx);
    closeUntilStatus(fx.token(), july.id(), CreditCardInvoiceStatus.CLOSED);
    july = getInvoice(fx.token(), july.id());
    assertThat(july.status()).isEqualTo(CreditCardInvoiceStatus.CLOSED);
    PayableItemResponse invoiceLine = item(listPayables(fx.token()), july.id());
    assertThat(invoiceLine.type()).isEqualTo(PayableItemType.INVOICE);
    assertThat(invoiceLine.overdue()).isTrue();
    assertThat(invoiceLine.dueDate()).isEqualTo(july.dueDate());
  }

  @Test
  void shouldFilterSelectedMonthIndependentOfClockMonth() throws Exception {
    String token = registerAndLogin(uniqueEmail("month"));
    UUID categoryId = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "500.00").id();
    createExpense(token, categoryId, accountId, "ACCOUNT", "Agosto", "10.00", "2026-08-20");
    ExpenseResponse october =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Outubro", "22.00", "2026-10-15");

    assertThat(LocalDate.now(FINANCIAL_ZONE).getMonthValue()).isNotEqualTo(10);
    PayablePageResponse page = listPayables(token, "year", "2026", "month", "10");
    assertThat(ids(page)).containsExactly(october.installmentId());
    assertThat(page.totalRemaining()).isEqualByComparingTo("22.00");

    PayablePageResponse range =
        listPayables(token, "startDate", "2026-10-01", "endDate", "2026-10-31");
    assertThat(ids(range)).containsExactly(october.installmentId());
  }

  @Test
  void shouldApplyCombinedFiltersIncludingCategoryResponsibleCardAndSearch() throws Exception {
    String token = registerAndLogin(uniqueEmail("filters"));
    UUID housing = createExpenseCategory(token, "Moradia").id();
    UUID food = createExpenseCategory(token, "Alimentacao").id();
    UUID accountId = createAccount(token, "5000.00").id();
    CardFx card = bootstrapCard(token, uniqueEmail("filters-card"), "3000.00", "2000.00");
    createCardExpense(card, "90.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(card);

    ExpenseResponse mineHousing =
        createNamedExpense(
            token,
            housing,
            accountId,
            "ACCOUNT",
            "Aluguel boleto",
            "70.00",
            "2026-09-10",
            "MINE",
            "23791");
    createNamedExpense(
        token, food, accountId, "ACCOUNT", "Mercado", "40.00", "2026-09-11", "GIULIA", null);

    PayablePageResponse byCategory = listPayables(token, "categoryId", housing.toString());
    assertThat(ids(byCategory)).contains(mineHousing.installmentId(), invoice.id());
    assertThat(byCategory.items())
        .filteredOn(item -> item.type() == PayableItemType.INSTALLMENT)
        .extracting(PayableItemResponse::categoryId)
        .containsOnly(housing);

    PayablePageResponse byResponsible = listPayables(token, "responsibleType", "MINE");
    assertThat(ids(byResponsible)).contains(mineHousing.installmentId(), invoice.id());

    PayablePageResponse byCard = listPayables(token, "creditCardId", card.cardId().toString());
    assertThat(ids(byCard)).containsExactly(invoice.id());

    PayablePageResponse withoutCard = listPayables(token, "withoutCreditCard", "true");
    assertThat(withoutCard.items()).allMatch(item -> item.type() == PayableItemType.INSTALLMENT);
    assertThat(ids(withoutCard)).doesNotContain(invoice.id());

    PayablePageResponse search = listPayables(token, "search", "boleto");
    assertThat(ids(search)).contains(mineHousing.installmentId());
    PayablePageResponse barcodeSearch = listPayables(token, "search", "23791");
    assertThat(ids(barcodeSearch)).contains(mineHousing.installmentId());
    PayablePageResponse cardSearch = listPayables(token, "search", "nubank");
    assertThat(ids(cardSearch)).contains(invoice.id());

    PayablePageResponse statusFilter = listPayables(token, "status", "OPEN,PARTIALLY_PAID");
    assertThat(statusFilter.items()).extracting(PayableItemResponse::status).contains("OPEN");
  }

  @Test
  void shouldSortWithIdTieBreakAndPaginateTotalsFromFullUniverse() throws Exception {
    String token = registerAndLogin(uniqueEmail("page"));
    UUID categoryId = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "500.00").id();
    ExpenseResponse first =
        createExpense(token, categoryId, accountId, "ACCOUNT", "A-primeiro", "10.00", "2026-11-01");
    ExpenseResponse second =
        createExpense(token, categoryId, accountId, "ACCOUNT", "B-segundo", "30.00", "2026-11-01");

    PayablePageResponse byName = listPayables(token, "sort", "name", "direction", "asc");
    assertThat(byName.items())
        .extracting(PayableItemResponse::name)
        .containsExactly("A-primeiro", "B-segundo");

    PayablePageResponse defaults = listPayables(token);
    assertThat(defaults.page()).isZero();
    assertThat(defaults.size()).isEqualTo(20);

    PayablePageResponse byDue = listPayables(token, "sort", "dueDate", "direction", "asc");
    assertThat(byDue.items().getFirst().id().compareTo(byDue.items().get(1).id())).isNegative();

    PayablePageResponse page0 = listPayables(token, "page", "0", "size", "1", "sort", "name");
    assertThat(page0.items()).hasSize(1);
    assertThat(page0.totalItems()).isEqualTo(2);
    assertThat(page0.totalPages()).isEqualTo(2);
    assertThat(page0.totalRemaining()).isEqualByComparingTo("40.00");
    assertThat(page0.totalOriginal()).isEqualByComparingTo("40.00");
    assertThat(page0.totalPaid()).isEqualByComparingTo("0.00");
    assertThat(page0.items().getFirst().id()).isEqualTo(first.installmentId());

    PayablePageResponse page1 = listPayables(token, "page", "1", "size", "1", "sort", "name");
    assertThat(page1.items().getFirst().id()).isEqualTo(second.installmentId());
    assertThat(page1.totalRemaining()).isEqualByComparingTo("40.00");
  }

  @Test
  void shouldIsolateUsersAndRejectUnauthenticated() throws Exception {
    String tokenA = registerAndLogin(uniqueEmail("iso-a"));
    String tokenB = registerAndLogin(uniqueEmail("iso-b"));
    UUID categoryA = createExpenseCategory(tokenA, "Moradia").id();
    UUID accountA = createAccount(tokenA, "100.00").id();
    ExpenseResponse expenseA =
        createExpense(tokenA, categoryA, accountA, "ACCOUNT", "SoA", "12.00", "2026-09-01");

    PayablePageResponse pageB = listPayables(tokenB);
    assertThat(ids(pageB)).doesNotContain(expenseA.installmentId());

    mockMvc.perform(get("/api/v1/payables")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldExcludeTransfersBalanceAdjustmentsInitialBalanceAndGoals() throws Exception {
    String token = registerAndLogin(uniqueEmail("outside"));
    UUID source = createAccount(token, "Conta A", "BANK_ACCOUNT", "500.00").id();
    UUID destination = createAccount(token, "Conta B", "BANK_ACCOUNT", "100.00").id();
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceAccountId":"%s","destinationAccountId":"%s","amount":20.00,"transferDate":"2026-08-10","description":"Pix"}
                    """
                        .formatted(source, destination)))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/accounts/" + source + "/balance-adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedBalance\":470.00}"))
        .andExpect(status().isCreated());
    MvcResult goalResult =
        mockMvc
            .perform(
                post("/api/v1/financial-goals")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"accountId":"%s","name":"Viagem","targetAmount":200.00}
                        """
                            .formatted(destination)))
            .andExpect(status().isCreated())
            .andReturn();
    UUID goalId =
        UUID.fromString(JsonPath.read(goalResult.getResponse().getContentAsString(), "$.id"));
    mockMvc
        .perform(
            post("/api/v1/financial-goals/" + goalId + "/contributions")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":50.00,"contributionDate":"2026-08-17","notes":"Aporte"}
                    """))
        .andExpect(status().isCreated());

    PayablePageResponse page = listPayables(token);
    assertThat(page.totalItems()).isZero();
    assertThat(page.items()).isEmpty();
  }

  @Test
  void shouldKeepIndependentOriginalPaidRemainingAfterDiscountSurchargeAndCredit()
      throws Exception {
    String token = registerAndLogin(uniqueEmail("adj"));
    UUID categoryId = createExpenseCategory(token, "Moradia").id();
    UUID accountId = createAccount(token, "500.00").id();
    ExpenseResponse expense =
        createExpense(token, categoryId, accountId, "ACCOUNT", "Ajuste", "100.00", "2026-09-05");
    mockMvc
        .perform(
            post("/api/v1/expenses/"
                    + expense.id()
                    + "/installments/"
                    + expense.installmentId()
                    + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"DISCOUNT","amount":10.00,"reason":"Desconto"}
                    """))
        .andExpect(status().isCreated());

    PayableItemResponse discounted = item(listPayables(token), expense.installmentId());
    assertThat(discounted.originalAmount()).isEqualByComparingTo("100.00");
    assertThat(discounted.paidAmount()).isEqualByComparingTo("0.00");
    assertThat(discounted.remainingAmount()).isEqualByComparingTo("90.00");
    assertThat(discounted.originalAmount())
        .isNotEqualByComparingTo(discounted.paidAmount().add(discounted.remainingAmount()));

    mockMvc
        .perform(
            post("/api/v1/expenses/"
                    + expense.id()
                    + "/installments/"
                    + expense.installmentId()
                    + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"SURCHARGE","amount":5.00,"reason":"Juros"}
                    """))
        .andExpect(status().isCreated());
    PayableItemResponse surcharged = item(listPayables(token), expense.installmentId());
    assertThat(surcharged.originalAmount()).isEqualByComparingTo("100.00");
    assertThat(surcharged.remainingAmount()).isEqualByComparingTo("95.00");

    CardFx fx = bootstrapCard(uniqueEmail("credit-pay"), "3000.00", "2000.00");
    createCardExpense(fx, "80.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);
    mockMvc
        .perform(
            post("/api/v1/credit-cards/" + fx.cardId() + "/credits")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":30.00,"reason":"Credito"}
                    """))
        .andExpect(status().isCreated());
    CreditCardInvoiceResponse afterCredit = getInvoice(fx.token(), invoice.id());
    PayableItemResponse creditLine = item(listPayables(fx.token()), invoice.id());
    assertThat(creditLine.remainingAmount()).isEqualByComparingTo(afterCredit.remainingAmount());
    assertThat(creditLine.originalAmount()).isEqualByComparingTo(afterCredit.totalAmount());
    assertThat(creditLine.paidAmount()).isEqualByComparingTo(afterCredit.paidAmount());
  }

  @Test
  void shouldRejectInvalidQueryAndPageSize() throws Exception {
    String token = registerAndLogin(uniqueEmail("invalid"));
    mockMvc
        .perform(
            get("/api/v1/payables")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("foo", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/payables")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("year", "2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mockMvc
        .perform(
            get("/api/v1/payables")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            get("/api/v1/payables")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    mockMvc
        .perform(
            get("/api/v1/payables")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  private PayablePageResponse listPayables(String token, String... params) throws Exception {
    var request = get("/api/v1/payables").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, PayablePageResponse.class);
  }

  private List<UUID> ids(PayablePageResponse page) {
    return page.items().stream().map(PayableItemResponse::id).toList();
  }

  private PayableItemResponse item(PayablePageResponse page, UUID id) {
    return page.items().stream().filter(line -> line.id().equals(id)).findFirst().orElseThrow();
  }

  private ExpenseResponse createExpense(
      String token,
      UUID categoryId,
      UUID accountId,
      String paymentMethod,
      String description,
      String amount,
      String dueDate)
      throws Exception {
    return createExpense(
        token, categoryId, accountId, paymentMethod, description, amount, dueDate, 1);
  }

  private ExpenseResponse createExpense(
      String token,
      UUID categoryId,
      UUID accountId,
      String paymentMethod,
      String description,
      String amount,
      String dueDate,
      int installmentCount)
      throws Exception {
    return createNamedExpense(
        token,
        categoryId,
        accountId,
        paymentMethod,
        description,
        amount,
        dueDate,
        "MINE",
        null,
        installmentCount);
  }

  private ExpenseResponse createNamedExpense(
      String token,
      UUID categoryId,
      UUID accountId,
      String paymentMethod,
      String description,
      String amount,
      String dueDate,
      String responsibleType,
      String barcode)
      throws Exception {
    return createNamedExpense(
        token,
        categoryId,
        accountId,
        paymentMethod,
        description,
        amount,
        dueDate,
        responsibleType,
        barcode,
        1);
  }

  private ExpenseResponse createNamedExpense(
      String token,
      UUID categoryId,
      UUID accountId,
      String paymentMethod,
      String description,
      String amount,
      String dueDate,
      String responsibleType,
      String barcode,
      int installmentCount)
      throws Exception {
    String accountField = accountId == null ? "" : ",\"accountId\":\"%s\"".formatted(accountId);
    String barcodeField = barcode == null ? "" : ",\"barcode\":\"%s\"".formatted(barcode);
    String countField =
        installmentCount == 1 ? "" : ",\"installmentCount\":%s".formatted(installmentCount);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoryId":"%s","description":"%s","totalAmount":%s,"expenseDate":"2026-08-01","dueDate":"%s","paymentMethod":"%s","responsibleType":"%s"%s%s%s}
                        """
                            .formatted(
                                categoryId,
                                description,
                                amount,
                                dueDate,
                                paymentMethod,
                                responsibleType,
                                accountField,
                                barcodeField,
                                countField)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
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

  private CardFx bootstrapCard(String email, String limit, String initial) throws Exception {
    String token = registerAndLogin(email);
    return bootstrapCard(token, email, limit, initial);
  }

  private CardFx bootstrapCard(String token, String unused, String limit, String initial)
      throws Exception {
    UUID categoryId = createExpenseCategory(token, "Cartao-" + UUID.randomUUID()).id();
    UUID accountId =
        createAccount(token, "Cartao-" + UUID.randomUUID(), "BANK_ACCOUNT", initial).id();
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
                    {"accountId":"%s","amount":%s,"paymentDate":"2026-08-20"}
                    """
                        .formatted(fx.accountId(), amount)))
        .andExpect(status().isOk());
  }

  private void closeUntilStatus(String token, UUID invoiceId, CreditCardInvoiceStatus expected)
      throws Exception {
    for (int i = 0; i < 8; i++) {
      if (getInvoice(token, invoiceId).status() == expected) {
        return;
      }
      invoiceService.closeDueInvoices();
    }
    if (expected == CreditCardInvoiceStatus.CLOSED) {
      CreditCardInvoice target = invoiceRepository.findById(invoiceId).orElseThrow();
      UUID cardId = target.getCreditCard().getId();
      UUID userId = target.getUserId();
      LocalDate today = LocalDate.now(FINANCIAL_ZONE);
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
    }
    assertThat(getInvoice(token, invoiceId).status()).isEqualTo(expected);
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

  private AccountResponse createAccount(String token, String initial) throws Exception {
    return createAccount(token, "Nubank", "BANK_ACCOUNT", initial);
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

  private record CardFx(String token, UUID categoryId, UUID accountId, UUID cardId) {}
}
