package br.com.financialcontrol.credit_cards;

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
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentAllocation;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoiceAdjustmentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoicePaymentResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardCreditResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardLimitResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.expenses.AdjustmentStatus;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
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
class CreditCardPhase9ApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private CreditCardInvoiceService invoiceService;
  @Autowired private CreditCardRepository creditCardRepository;
  @Autowired private CreditCardCreditRepository creditRepository;
  @Autowired private CreditCardCreditApplicationRepository creditApplicationRepository;
  @Autowired private CreditCardInvoicePaymentAllocationRepository paymentAllocationRepository;

  @Test
  void shouldCreateCardWithOptionalLastFourDigitsAndFilterByHolderName() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("card-crud"), "senha-segura");

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/credit-cards")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","holderName":"Ederson","creditLimit":5000.00,"closingDay":10,"dueDay":20}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();
    CreditCardResponse card = read(created, CreditCardResponse.class);
    assertThat(card.lastFourDigits()).isNull();

    mockMvc
        .perform(
            get("/api/v1/credit-cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("holderName", "ederson"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mockMvc
        .perform(
            get("/api/v1/credit-cards")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("holderName", "Giulia"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(
            put("/api/v1/credit-cards/" + card.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Nubank Gold","holderName":"Ederson","lastFourDigits":"1234","creditLimit":8000.00,"closingDay":10,"dueDay":20}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Nubank Gold"))
        .andExpect(jsonPath("$.lastFourDigits").value("1234"));
  }

  @Test
  void shouldAllowPurchaseAboveLimitAndKeepDerivedAvailableNegative() throws Exception {
    Fixture fx = bootstrap("over-limit");
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cardExpenseJson(fx.categoryId(), fx.cardId(), "500.00", "2026-08-11", 1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
        .andExpect(jsonPath("$.creditCardId").value(fx.cardId().toString()));

    MvcResult limitResult =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + fx.cardId() + "/limit")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    CreditCardLimitResponse limit = read(limitResult, CreditCardLimitResponse.class);
    assertThat(limit.creditLimit()).isEqualByComparingTo("100.00");
    assertThat(limit.usedLimit()).isEqualByComparingTo("500.00");
    assertThat(limit.availableLimit()).isEqualByComparingTo("-400.00");
  }

  @Test
  void shouldAssignInvoiceDueDateIgnoringRequestDueDateAndKeepClosingDayOnNextCycle()
      throws Exception {
    Fixture fx = bootstrap("cycle");
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        cardExpenseJson(fx.categoryId(), fx.cardId(), "90.00", "2026-08-10", 3)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dueDate").value("2026-09-20"))
            .andReturn();
    ExpenseResponse expense = read(result, ExpenseResponse.class);
    MvcResult installmentsResult =
        mockMvc
            .perform(
                get("/api/v1/expenses/" + expense.id() + "/installments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    ExpenseInstallmentResponse[] installments =
        jsonMapper.readValue(
            installmentsResult.getResponse().getContentAsString(),
            ExpenseInstallmentResponse[].class);
    assertThat(installments).hasSize(3);
    assertThat(installments[0].dueDate()).hasToString("2026-09-20");
    assertThat(installments[1].dueDate()).hasToString("2026-10-20");
    assertThat(installments[2].dueDate()).hasToString("2026-11-20");
    List<CreditCardInvoiceResponse> invoices = listInvoices(fx);
    assertThat(invoices.getFirst().status()).isEqualTo(CreditCardInvoiceStatus.OPEN);
    assertThat(invoices.getFirst().closingDate()).hasToString("2026-09-10");
    assertThat(invoices.get(1).status()).isEqualTo(CreditCardInvoiceStatus.SCHEDULED);
  }

  @Test
  void shouldPayInvoiceWithOddCentsWithoutExceedingRemainingOrChangingAccountBeyondPayment()
      throws Exception {
    Fixture fx = bootstrap("rateio");
    ExpenseResponse first = createCardExpense(fx, "10.00", "2026-08-11", 1);
    ExpenseResponse second = createCardExpense(fx, "10.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);
    assertThat(invoice.remainingAmount()).isEqualByComparingTo("20.00");

    mockMvc
        .perform(
            post("/api/v1/invoices/" + invoice.id() + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payInvoiceJson(fx.accountId(), "10.01")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.amount").value(10.01));

    CreditCardInvoiceResponse after = currentInvoice(fx);
    assertThat(after.remainingAmount()).isEqualByComparingTo("9.99");
    assertThat(after.paidAmount()).isEqualByComparingTo("10.01");
    assertThat(after.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);
    assertThat(balance(fx.token(), fx.accountId())).isEqualByComparingTo("989.99");
    MvcResult items =
        mockMvc
            .perform(
                get("/api/v1/invoices/" + invoice.id() + "/items")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    ExpenseInstallmentResponse[] itemBody =
        jsonMapper.readValue(
            items.getResponse().getContentAsString(), ExpenseInstallmentResponse[].class);
    BigDecimal remainingSum =
        java.util.Arrays.stream(itemBody)
            .map(ExpenseInstallmentResponse::remainingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(remainingSum).isEqualByComparingTo("9.99");
    assertThat(second.id()).isNotEqualTo(first.id());
  }

  @Test
  void shouldApplyCreditsFifoAcrossClosedThenOpenInvoices() throws Exception {
    Fixture fx = bootstrap("credits-fifo");
    createCardExpense(fx, "30.00", "2026-07-05", 1);
    invoiceService.closeDueInvoices();
    createCardExpense(fx, "40.00", "2026-08-11", 1);

    mockMvc
        .perform(
            post("/api/v1/credit-cards/" + fx.cardId() + "/credits")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":50.00,"reason":"crédito manual"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.remainingAmount").value(0.00));

    List<CreditCardInvoiceResponse> invoices = listInvoices(fx);
    CreditCardInvoiceResponse july =
        invoices.stream().filter(item -> item.referenceMonth() == 7).findFirst().orElseThrow();
    CreditCardInvoiceResponse september =
        invoices.stream().filter(item -> item.referenceMonth() == 9).findFirst().orElseThrow();
    assertThat(july.status()).isEqualTo(CreditCardInvoiceStatus.PAID);
    assertThat(july.remainingAmount()).isEqualByComparingTo("0.00");
    assertThat(september.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);
    assertThat(september.remainingAmount()).isEqualByComparingTo("20.00");
    assertThat(balance(fx.token(), fx.accountId())).isEqualByComparingTo("1000.00");
  }

  @Test
  void shouldApplyIdleCreditWhenSchedulerOpensNextInvoice() throws Exception {
    Fixture fx = bootstrap("rn246-sched");
    createCardExpense(fx, "30.00", "2026-07-05", 1);
    CreditCardInvoiceResponse july = currentInvoice(fx);
    payInvoice(fx, july.id(), "30.00");
    assertThat(getInvoice(fx.token(), july.id()).remainingAmount()).isEqualByComparingTo("0.00");

    CreditCardCreditResponse credit = createManualCredit(fx, "40.00", "crédito ocioso scheduler");
    assertThat(credit.remainingAmount()).isEqualByComparingTo("40.00");
    assertThat(applicationsOf(credit.id())).isEmpty();

    createCardExpense(fx, "40.00", "2026-08-11", 1);
    CreditCardInvoiceResponse septemberBefore =
        listInvoices(fx).stream()
            .filter(item -> item.referenceMonth() == 9)
            .findFirst()
            .orElseThrow();
    assertThat(septemberBefore.status()).isEqualTo(CreditCardInvoiceStatus.SCHEDULED);
    assertThat(septemberBefore.remainingAmount()).isEqualByComparingTo("40.00");
    assertThat(listCredits(fx).getFirst().remainingAmount()).isEqualByComparingTo("40.00");

    invoiceService.closeDueInvoices();

    CreditCardInvoiceResponse julyAfter = getInvoice(fx.token(), july.id());
    assertThat(julyAfter.status()).isEqualTo(CreditCardInvoiceStatus.PAID);

    CreditCardInvoiceResponse septemberAfter = getInvoice(fx.token(), septemberBefore.id());
    assertThat(septemberAfter.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);
    assertThat(septemberAfter.remainingAmount()).isEqualByComparingTo("0.00");

    List<CreditCardCreditApplication> applications = applicationsOf(credit.id());
    assertThat(applications).hasSize(1);
    assertThat(applications.getFirst().getAmount()).isEqualByComparingTo("40.00");
    assertThat(applications.getFirst().getInvoice().getId()).isEqualTo(septemberBefore.id());
    assertThat(listCredits(fx).getFirst().remainingAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldReapplyIdleCreditAfterInvoicePaymentReverse() throws Exception {
    Fixture fx = bootstrap("rn246-rev-pay");
    createCardExpense(fx, "100.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);
    InvoicePaymentResponse payment = payInvoice(fx, invoice.id(), "100.00");
    assertThat(getInvoice(fx.token(), invoice.id()).remainingAmount()).isEqualByComparingTo("0.00");

    List<CreditCardInvoicePaymentAllocation> originalAllocations =
        paymentAllocationsOf(fx, payment.id());
    assertThat(originalAllocations).hasSize(1);
    assertThat(originalAllocations.getFirst().getAmount()).isEqualByComparingTo("100.00");

    CreditCardCreditResponse credit = createManualCredit(fx, "40.00", "crédito ocioso reverse pay");
    assertThat(credit.remainingAmount()).isEqualByComparingTo("40.00");
    assertThat(applicationsOf(credit.id())).isEmpty();

    mockMvc
        .perform(
            post("/api/v1/invoices/" + invoice.id() + "/payments/" + payment.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVERSED"));

    assertThat(paymentAllocationsOf(fx, payment.id())).hasSize(1);
    assertThat(paymentAllocationsOf(fx, payment.id()).getFirst().getAmount())
        .isEqualByComparingTo("100.00");

    CreditCardInvoiceResponse after = getInvoice(fx.token(), invoice.id());
    assertThat(after.remainingAmount()).isEqualByComparingTo("60.00");
    assertThat(after.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);

    List<CreditCardCreditApplication> applications = applicationsOf(credit.id());
    assertThat(applications).hasSize(1);
    assertThat(applications.getFirst().getAmount()).isEqualByComparingTo("40.00");
    assertThat(listCredits(fx).getFirst().remainingAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldReapplyIdleCreditAfterInvoiceAdjustmentReverse() throws Exception {
    Fixture fx = bootstrap("rn246-rev-adj");
    createCardExpense(fx, "100.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);

    MvcResult adjustmentResult =
        mockMvc
            .perform(
                post("/api/v1/invoices/" + invoice.id() + "/adjustments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"type":"DISCOUNT","amount":100.00,"reason":"desconto total"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    InvoiceAdjustmentResponse adjustment = read(adjustmentResult, InvoiceAdjustmentResponse.class);
    assertThat(getInvoice(fx.token(), invoice.id()).remainingAmount()).isEqualByComparingTo("0.00");

    CreditCardCreditResponse credit = createManualCredit(fx, "40.00", "crédito ocioso reverse adj");
    assertThat(credit.remainingAmount()).isEqualByComparingTo("40.00");
    assertThat(applicationsOf(credit.id())).isEmpty();

    mockMvc
        .perform(
            post("/api/v1/invoices/"
                    + invoice.id()
                    + "/adjustments/"
                    + adjustment.id()
                    + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVERSED"));

    MvcResult adjustments =
        mockMvc
            .perform(
                get("/api/v1/invoices/" + invoice.id() + "/adjustments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    InvoiceAdjustmentResponse[] history =
        jsonMapper.readValue(
            adjustments.getResponse().getContentAsString(), InvoiceAdjustmentResponse[].class);
    assertThat(history).hasSize(1);
    assertThat(history[0].status()).isEqualTo(AdjustmentStatus.REVERSED);
    assertThat(history[0].amount()).isEqualByComparingTo("100.00");
    assertThat(history[0].reason()).isEqualTo("desconto total");

    CreditCardInvoiceResponse after = getInvoice(fx.token(), invoice.id());
    assertThat(after.remainingAmount()).isEqualByComparingTo("60.00");
    assertThat(after.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);

    List<CreditCardCreditApplication> applications = applicationsOf(credit.id());
    assertThat(applications).hasSize(1);
    assertThat(applications.getFirst().getAmount()).isEqualByComparingTo("40.00");
    assertThat(listCredits(fx).getFirst().remainingAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldRefundCardPurchaseToCardCreditAndToAccount() throws Exception {
    Fixture creditFx = bootstrap("rn117-credit");
    ExpenseResponse creditPurchase = createCardExpense(creditFx, "80.00", "2026-08-11", 1);
    CreditCardInvoiceResponse creditInvoice = currentInvoice(creditFx);
    payInvoice(creditFx, creditInvoice.id(), "50.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + creditPurchase.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(creditFx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"settlement":"CARD_CREDIT"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REFUNDED"));
    mockMvc
        .perform(
            get("/api/v1/credit-cards/" + creditFx.cardId() + "/credits")
                .header(HttpHeaders.AUTHORIZATION, bearer(creditFx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].amount").value(50.00))
        .andExpect(jsonPath("$[0].origin").value("CARD_PURCHASE_REFUND"));

    Fixture accountFx = bootstrap("rn117-account");
    ExpenseResponse accountPurchase = createCardExpense(accountFx, "80.00", "2026-08-11", 1);
    CreditCardInvoiceResponse accountInvoice = currentInvoice(accountFx);
    payInvoice(accountFx, accountInvoice.id(), "50.00");
    assertThat(balance(accountFx.token(), accountFx.accountId())).isEqualByComparingTo("950.00");
    mockMvc
        .perform(
            post("/api/v1/expenses/" + accountPurchase.id() + "/refund")
                .header(HttpHeaders.AUTHORIZATION, bearer(accountFx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"settlement":"ACCOUNT","accountId":"%s"}
                    """
                        .formatted(accountFx.accountId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REFUNDED"));
    assertThat(balance(accountFx.token(), accountFx.accountId())).isEqualByComparingTo("1000.00");
  }

  @Test
  void shouldCloseOpenInvoiceIdempotentlyAndRejectExpensePayForCardPurchase() throws Exception {
    Fixture fx = bootstrap("scheduler");
    ExpenseResponse expense = createCardExpense(fx, "25.00", "2026-07-05", 1);
    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payInvoiceJson(fx.accountId(), "25.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

    invoiceService.closeDueInvoices();
    CreditCardInvoiceResponse closed =
        listInvoices(fx).stream()
            .filter(item -> item.referenceMonth() == 7)
            .findFirst()
            .orElseThrow();
    assertThat(closed.status()).isEqualTo(CreditCardInvoiceStatus.CLOSED);
    invoiceService.closeDueInvoices();
    CreditCardInvoiceResponse stillClosed = getInvoice(fx.token(), closed.id());
    assertThat(stillClosed.status()).isEqualTo(CreditCardInvoiceStatus.CLOSED);
    assertThat(stillClosed.remainingAmount()).isEqualByComparingTo("25.00");
  }

  @Test
  void shouldIsolateCardsAndRejectCadastralEditOfInvoicedInstallment() throws Exception {
    Fixture owner = bootstrap("own-a");
    String otherToken = registerAndLogin("Bob", uniqueEmail("own-b"), "senha-segura");
    mockMvc
        .perform(
            get("/api/v1/credit-cards/" + owner.cardId())
                .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
        .andExpect(status().isNotFound());

    ExpenseResponse expense = createCardExpense(owner, "40.00", "2026-08-11", 1);
    MvcResult installmentsResult =
        mockMvc
            .perform(
                get("/api/v1/expenses/" + expense.id() + "/installments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
            .andExpect(status().isOk())
            .andReturn();
    ExpenseInstallmentResponse installment =
        jsonMapper
            .readValue(
                installmentsResult.getResponse().getContentAsString(),
                ExpenseInstallmentResponse[].class)[0];
    mockMvc
        .perform(
            put("/api/v1/expenses/" + expense.id() + "/installments/" + installment.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":40.00,"dueDate":"2026-09-21"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldCreateInvoiceAdjustmentWithReasonAndListItems() throws Exception {
    Fixture fx = bootstrap("invoice-adj");
    createCardExpense(fx, "100.00", "2026-08-11", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);
    mockMvc
        .perform(
            post("/api/v1/invoices/" + invoice.id() + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"DISCOUNT","amount":10.00,"reason":"desconto da fatura"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reason").value("desconto da fatura"));
    MvcResult items =
        mockMvc
            .perform(
                get("/api/v1/invoices/" + invoice.id() + "/items")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    ExpenseInstallmentResponse[] body =
        jsonMapper.readValue(
            items.getResponse().getContentAsString(), ExpenseInstallmentResponse[].class);
    assertThat(body).hasSize(1);
    assertThat(body[0].remainingAmount()).isEqualByComparingTo("90.00");
  }

  @Test
  void shouldEnforceSurchargeRequiresPositiveInvoiceRemaining() throws Exception {
    Fixture allowed = bootstrap("surcharge-ok");
    createCardExpense(allowed, "100.00", "2026-08-11", 1);
    CreditCardInvoiceResponse openInvoice = currentInvoice(allowed);
    mockMvc
        .perform(
            post("/api/v1/invoices/" + openInvoice.id() + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(allowed.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"SURCHARGE","amount":5.00,"reason":"juros"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("SURCHARGE"))
        .andExpect(jsonPath("$.amount").value(5.00));
    assertThat(getInvoice(allowed.token(), openInvoice.id()).remainingAmount())
        .isEqualByComparingTo("105.00");
    assertThat(listAdjustments(allowed, openInvoice.id())).hasSize(1);

    Fixture zeroRemaining = bootstrap("surcharge-zero");
    createCardExpense(zeroRemaining, "50.00", "2026-08-11", 1);
    CreditCardInvoiceResponse paidDown = currentInvoice(zeroRemaining);
    payInvoice(zeroRemaining, paidDown.id(), "50.00");
    assertThat(getInvoice(zeroRemaining.token(), paidDown.id()).remainingAmount())
        .isEqualByComparingTo("0.00");
    assertThat(getInvoice(zeroRemaining.token(), paidDown.id()).status())
        .isEqualTo(CreditCardInvoiceStatus.OPEN);

    mockMvc
        .perform(
            post("/api/v1/invoices/" + paidDown.id() + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(zeroRemaining.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"SURCHARGE","amount":10.00,"reason":"juros sem remaining"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value("O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto."));

    assertThat(getInvoice(zeroRemaining.token(), paidDown.id()).remainingAmount())
        .isEqualByComparingTo("0.00");
    assertThat(listAdjustments(zeroRemaining, paidDown.id())).isEmpty();

    mockMvc
        .perform(
            post("/api/v1/invoices/" + paidDown.id() + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(zeroRemaining.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"DISCOUNT","amount":1.00,"reason":"desconto sem remaining"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message").value("O desconto não pode ultrapassar o saldo da fatura."));
    assertThat(listAdjustments(zeroRemaining, paidDown.id())).isEmpty();

    Fixture paidInvoiceFx = bootstrap("surcharge-paid");
    createCardExpense(paidInvoiceFx, "30.00", "2026-07-05", 1);
    CreditCardInvoiceResponse july = currentInvoice(paidInvoiceFx);
    payInvoice(paidInvoiceFx, july.id(), "30.00");
    invoiceService.closeDueInvoices();
    CreditCardInvoiceResponse paid = getInvoice(paidInvoiceFx.token(), july.id());
    assertThat(paid.status()).isEqualTo(CreditCardInvoiceStatus.PAID);

    mockMvc
        .perform(
            post("/api/v1/invoices/" + paid.id() + "/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(paidInvoiceFx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"SURCHARGE","amount":10.00,"reason":"juros em paid"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value("Fatura paga não pode ser alterada."));
    assertThat(listAdjustments(paidInvoiceFx, paid.id())).isEmpty();
  }

  private Fixture bootstrap(String prefix) throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail(prefix), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Cartão");
    AccountResponse account = createAccount(token);
    CreditCardResponse card = createCard(token, prefix.startsWith("over") ? "100.00" : "5000.00");
    return new Fixture(token, category.id(), account.id(), card.id());
  }

  private CreditCardResponse createCard(String token, String limit) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/credit-cards")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","holderName":"Ederson","lastFourDigits":"9999","creditLimit":%s,"closingDay":10,"dueDay":20}
                        """
                            .formatted(limit)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, CreditCardResponse.class);
  }

  private ExpenseResponse createCardExpense(
      Fixture fx, String amount, String expenseDate, int installments) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/expenses")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        cardExpenseJson(
                            fx.categoryId(), fx.cardId(), amount, expenseDate, installments)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, ExpenseResponse.class);
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

  private List<CreditCardInvoiceResponse> listInvoices(Fixture fx) throws Exception {
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

  private InvoicePaymentResponse payInvoice(Fixture fx, UUID invoiceId, String amount)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/invoices/" + invoiceId + "/payments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payInvoiceJson(fx.accountId(), amount)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, InvoicePaymentResponse.class);
  }

  private CreditCardCreditResponse createManualCredit(Fixture fx, String amount, String reason)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/credit-cards/" + fx.cardId() + "/credits")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"amount":%s,"reason":"%s"}
                        """
                            .formatted(amount, reason)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, CreditCardCreditResponse.class);
  }

  private List<CreditCardCreditResponse> listCredits(Fixture fx) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + fx.cardId() + "/credits")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return List.of(
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), CreditCardCreditResponse[].class));
  }

  private List<InvoiceAdjustmentResponse> listAdjustments(Fixture fx, UUID invoiceId)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/invoices/" + invoiceId + "/adjustments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return List.of(
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), InvoiceAdjustmentResponse[].class));
  }

  private List<CreditCardCreditApplication> applicationsOf(UUID creditId) {
    CreditCardCredit credit = creditRepository.findById(creditId).orElseThrow();
    return creditApplicationRepository.findAllByCredit_IdAndUserId(
        credit.getId(), credit.getUserId());
  }

  private List<CreditCardInvoicePaymentAllocation> paymentAllocationsOf(
      Fixture fx, UUID paymentId) {
    UUID userId = creditCardRepository.findById(fx.cardId()).orElseThrow().getUserId();
    return paymentAllocationRepository.findAllByInvoicePayment_IdAndUserId(paymentId, userId);
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

  private AccountResponse createAccount(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","type":"BANK_ACCOUNT","initialBalance":1000.00}
                        """))
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

  private static String cardExpenseJson(
      UUID categoryId, UUID cardId, String amount, String expenseDate, int installments) {
    return """
        {"categoryId":"%s","description":"Compra","totalAmount":%s,"expenseDate":"%s","dueDate":"2099-01-01","paymentMethod":"CREDIT_CARD","creditCardId":"%s","responsibleType":"MINE","installmentCount":%s}
        """
        .formatted(categoryId, amount, expenseDate, cardId, installments);
  }

  private static String payInvoiceJson(UUID accountId, String amount) {
    return """
        {"accountId":"%s","amount":%s,"paymentDate":"2026-08-20"}
        """
        .formatted(accountId, amount);
  }

  private record Fixture(String token, UUID categoryId, UUID accountId, UUID cardId) {}
}
