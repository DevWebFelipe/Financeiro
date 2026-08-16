package br.com.financialcontrol.credit_card_invoice_agreements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementInstallmentResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementResponse;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardLimitResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
class CreditCardInvoiceAgreementPhase13ApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private CreditCardInvoiceService invoiceService;
  @Autowired private CreditCardInvoiceRepository invoiceRepository;

  // --- L01 / L34 / L35 / D11 used_limit ---

  @Test
  void shouldL01CreateValidAgreementWithFinancedContractedAndNextCycleFirstInstallment()
      throws Exception {
    // L01 parcelamento válido; L34 1ª parcela na próxima fatura; L35 contractedTotal > financed;
    // D11 used_limit usa contractedTotal
    Fixture fx = bootstrap("l01", "5000.00", "2000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "1000.00");
    assertThat(source.status()).isEqualTo(CreditCardInvoiceStatus.CLOSED);
    assertThat(source.remainingAmount()).isEqualByComparingTo("1000.00");

    AgreementResponse agreement =
        createAgreement(fx, source.id(), "400.00", 10, "120.00", status().isCreated());

    assertThat(agreement.status()).isEqualTo(CreditCardInvoiceAgreementStatus.ACTIVE);
    assertThat(agreement.sourceInvoiceId()).isEqualTo(source.id());
    assertThat(agreement.entryAmount()).isEqualByComparingTo("400.00");
    assertThat(agreement.financedAmount()).isEqualByComparingTo("600.00");
    assertThat(agreement.installmentCount()).isEqualTo(10);
    assertThat(agreement.installmentAmount()).isEqualByComparingTo("120.00");
    assertThat(agreement.contractedTotal()).isEqualByComparingTo("1200.00");
    assertThat(agreement.additionalCost()).isEqualByComparingTo("600.00");
    assertThat(agreement.additionalCostPercent()).isEqualByComparingTo("1.0000");
    assertThat(agreement.installments()).hasSize(10);
    assertThat(agreement.installments())
        .allSatisfy(i -> assertThat(i.amount()).isEqualByComparingTo("120.00"));
    assertThat(agreement.installments().getFirst().invoiceId()).isNotEqualTo(source.id());

    CreditCardInvoiceResponse settled = getInvoice(fx.token(), source.id());
    assertThat(settled.status()).isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
    assertThat(settled.remainingAmount()).isEqualByComparingTo("0.00");

    CreditCardLimitResponse limit = cardLimit(fx);
    assertThat(limit.usedLimit()).isEqualByComparingTo("1200.00");
    assertThat(balance(fx.token(), fx.accountId())).isEqualByComparingTo("1600.00");
  }

  // --- L02 ---

  @Test
  void shouldL02CreateAgreementWithZeroEntryAmount() throws Exception {
    Fixture fx = bootstrap("l02", "5000.00", "1000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "1000.00");

    AgreementResponse agreement =
        createAgreement(fx, source.id(), "0.00", 10, "120.00", status().isCreated());

    assertThat(agreement.entryAmount()).isEqualByComparingTo("0.00");
    assertThat(agreement.financedAmount()).isEqualByComparingTo("1000.00");
    assertThat(agreement.contractedTotal()).isEqualByComparingTo("1200.00");
    assertThat(agreement.additionalCost()).isEqualByComparingTo("200.00");
    assertThat(getInvoice(fx.token(), source.id()).status())
        .isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
    assertThat(balance(fx.token(), fx.accountId())).isEqualByComparingTo("1000.00");
  }

  // --- L03 ---

  @Test
  void shouldL03RejectEntryEqualToRemaining() throws Exception {
    Fixture fx = bootstrap("l03");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "500.00");

    mockMvc
        .perform(
            post("/api/v1/invoices/" + source.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(agreementJson(fx.accountId(), "500.00", 10, "50.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value("Entrada igual ao saldo restante: use o pagamento da fatura."));

    assertThat(getInvoice(fx.token(), source.id()).status())
        .isEqualTo(CreditCardInvoiceStatus.CLOSED);
    assertThat(listAgreements(fx, source.id())).isEmpty();
  }

  // --- L04 ---

  @Test
  void shouldL04RejectEntryGreaterThanRemaining() throws Exception {
    Fixture fx = bootstrap("l04");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "500.00");

    mockMvc
        .perform(
            post("/api/v1/invoices/" + source.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(agreementJson(fx.accountId(), "500.01", 10, "50.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value("A entrada não pode ultrapassar o saldo restante da fatura."));
  }

  // --- L05 ---

  @Test
  void shouldL05RejectAgreementOnOpenInvoice() throws Exception {
    Fixture fx = bootstrap("l05");
    createCardExpense(fx, "100.00", "2026-08-11", 1);
    CreditCardInvoiceResponse open = currentInvoice(fx);
    assertThat(open.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);

    mockMvc
        .perform(
            post("/api/v1/invoices/" + open.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(agreementJson(fx.accountId(), "10.00", 5, "20.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value("Somente fatura fechada com saldo em aberto pode ser negociada."));
  }

  // --- L06 / L07 / L08 ---

  @Test
  void shouldL06L07L08PayOpenInvoiceFullyWithoutSettledThenCloseToPaid() throws Exception {
    Fixture fx = bootstrap("l06-08");
    createCardExpense(fx, "80.00", "2026-07-05", 1);
    CreditCardInvoiceResponse open = currentInvoice(fx);
    assertThat(open.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);
    assertThat(open.remainingAmount()).isEqualByComparingTo("80.00");

    // L07: PayInvoiceRequest has no settled — body only accountId/amount/paymentDate
    mockMvc
        .perform(
            post("/api/v1/invoices/" + open.id() + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payInvoiceJson(fx.accountId(), "80.00")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(80.00))
        .andExpect(jsonPath("$.settled").doesNotExist());

    // L06: remaining 0 keeps OPEN until close
    CreditCardInvoiceResponse afterPay = getInvoice(fx.token(), open.id());
    assertThat(afterPay.remainingAmount()).isEqualByComparingTo("0.00");
    assertThat(afterPay.status()).isEqualTo(CreditCardInvoiceStatus.OPEN);

    // L08
    invoiceService.closeDueInvoices();
    assertThat(getInvoice(fx.token(), open.id()).status()).isEqualTo(CreditCardInvoiceStatus.PAID);
  }

  // --- L09 / L10 ---

  @Test
  void shouldL09L10KeepOriginalExpenseInstallmentsAndCreateIndependentAgreementExpense()
      throws Exception {
    Fixture fx = bootstrap("l09-10", "5000.00", "2000.00");
    ExpenseResponse original = createCardExpense(fx, "900.00", "2026-07-05", 3);
    CreditCardInvoiceResponse july = currentInvoice(fx);
    closeUntilStatus(fx.token(), july.id(), CreditCardInvoiceStatus.CLOSED);
    CreditCardInvoiceResponse closed = getInvoice(fx.token(), july.id());

    ExpenseInstallmentResponse[] before = listExpenseInstallments(fx, original.id());
    assertThat(before).hasSize(3);

    AgreementResponse agreement =
        createAgreement(fx, closed.id(), "100.00", 5, "50.00", status().isCreated());

    ExpenseInstallmentResponse[] after = listExpenseInstallments(fx, original.id());
    assertThat(after).hasSize(3);
    assertThat(after[0].id()).isEqualTo(before[0].id());
    assertThat(after[1].id()).isEqualTo(before[1].id());
    assertThat(after[2].id()).isEqualTo(before[2].id());
    assertThat(agreement.expenseId()).isNotEqualTo(original.id());

    ExpenseInstallmentResponse[] agreementInstallments =
        listExpenseInstallments(fx, agreement.expenseId());
    assertThat(agreementInstallments).hasSize(5);
    assertThat(agreementInstallments)
        .allSatisfy(i -> assertThat(i.expenseId()).isEqualTo(agreement.expenseId()));
  }

  // --- L11 / L12 / D11 coexistence ---

  @Test
  void shouldL11L12CreateSecondAgreementWithoutRenegotiatingPreviousActive() throws Exception {
    Fixture fx = bootstrap("l11-12", "5000.00", "3000.00");

    createCardExpense(fx, "1000.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    june = getInvoice(fx.token(), june.id());

    AgreementResponse first =
        createAgreement(fx, june.id(), "0.00", 2, "600.00", status().isCreated());
    assertThat(first.status()).isEqualTo(CreditCardInvoiceAgreementStatus.ACTIVE);

    createCardExpense(fx, "500.00", "2026-07-05", 1);
    CreditCardInvoiceResponse july = invoiceByClosing(fx, LocalDate.of(2026, 7, 10));
    closeUntilStatus(fx.token(), july.id(), CreditCardInvoiceStatus.CLOSED);
    july = getInvoice(fx.token(), july.id());

    AgreementResponse second =
        createAgreement(fx, july.id(), "50.00", 11, "100.00", status().isCreated());
    assertThat(second.status()).isEqualTo(CreditCardInvoiceAgreementStatus.ACTIVE);
    assertThat(second.id()).isNotEqualTo(first.id());

    AgreementResponse stillActive = getAgreement(fx.token(), first.id());
    assertThat(stillActive.status()).isEqualTo(CreditCardInvoiceAgreementStatus.ACTIVE);
    assertThat(stillActive.supersededByAgreementId()).isNull();
  }

  // --- L13–L16 / L36 renegotiation ---

  @Test
  void shouldL13ToL16AndL36RenegotiateAllActiveWithConsolidatedFinancedAmount() throws Exception {
    Fixture fx = bootstrap("l13-16", "5000.00", "5000.00");

    createCardExpense(fx, "1000.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    june = getInvoice(fx.token(), june.id());

    // 2 installments: installment 1 on July, installment 2 is future
    AgreementResponse first =
        createAgreement(fx, june.id(), "0.00", 2, "600.00", status().isCreated());
    UUID firstInstallment1Id = first.installments().getFirst().id();
    UUID firstInstallment1InvoiceId = first.installments().getFirst().invoiceId();

    createCardExpense(fx, "500.00", "2026-07-05", 1);
    closeUntilStatus(fx.token(), firstInstallment1InvoiceId, CreditCardInvoiceStatus.CLOSED);
    CreditCardInvoiceResponse july = getInvoice(fx.token(), firstInstallment1InvoiceId);
    // July remaining = agreement 600 + purchase 500
    assertThat(july.remainingAmount()).isEqualByComparingTo("1100.00");

    // futureOriginal=600; bank net=300 → discount=300
    // settlement=1000; financed=1000+300=1300; contracted=2×700=1400
    AgreementResponse renegotiated =
        renegotiate(fx, july.id(), "100.00", 2, "700.00", "300.00", status().isCreated());

    assertThat(renegotiated.status()).isEqualTo(CreditCardInvoiceAgreementStatus.ACTIVE);
    assertThat(renegotiated.entryAmount()).isEqualByComparingTo("100.00");
    assertThat(renegotiated.financedAmount()).isEqualByComparingTo("1300.00");
    assertThat(renegotiated.contractedTotal()).isEqualByComparingTo("1400.00");
    assertThat(renegotiated.additionalCost()).isEqualByComparingTo("100.00");

    AgreementResponse previous = getAgreement(fx.token(), first.id());
    assertThat(previous.status()).isEqualTo(CreditCardInvoiceAgreementStatus.RENEGOTIATED);
    assertThat(previous.supersededByAgreementId()).isEqualTo(renegotiated.id());

    // Current-invoice installment settled via entry+settlement; future zeroed
    // (discount+incorporation)
    AgreementResponse refreshedPrevious = getAgreement(fx.token(), first.id());
    assertThat(refreshedPrevious.installments().getFirst().id()).isEqualTo(firstInstallment1Id);
    assertThat(refreshedPrevious.installments().getFirst().status()).isEqualTo(ExpenseStatus.PAID);
    assertThat(refreshedPrevious.installments().getFirst().remainingAmount())
        .isEqualByComparingTo("0.00");
    assertThat(refreshedPrevious.installments().get(1).status()).isEqualTo(ExpenseStatus.PAID);
    assertThat(refreshedPrevious.installments().get(1).remainingAmount())
        .isEqualByComparingTo("0.00");

    assertThat(getInvoice(fx.token(), july.id()).status())
        .isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
    assertThat(getInvoice(fx.token(), july.id()).remainingAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldOfficialJanFebMarRenegotiationScenario() throws Exception {
    Fixture fx = bootstrap("rn254-official", "10000.00", "10000.00");

    // Janeiro: fatura 1.500; entrada 500; Agreement #1 = 10×200; financed=1.000
    createCardExpense(fx, "1500.00", "2026-01-05", 1);
    CreditCardInvoiceResponse january = invoiceByClosing(fx, LocalDate.of(2026, 1, 10));
    closeUntilStatus(fx.token(), january.id(), CreditCardInvoiceStatus.CLOSED);
    january = getInvoice(fx.token(), january.id());
    assertThat(january.remainingAmount()).isEqualByComparingTo("1500.00");

    AgreementResponse agreement1 =
        createAgreement(fx, january.id(), "500.00", 10, "200.00", status().isCreated());
    assertThat(agreement1.financedAmount()).isEqualByComparingTo("1000.00");
    assertThat(agreement1.contractedTotal()).isEqualByComparingTo("2000.00");
    UUID febInvoiceId = agreement1.installments().getFirst().invoiceId();
    assertThat(febInvoiceId).isNotEqualTo(january.id());

    // Fevereiro: compras 1.000 + parcela 1/10 = 200 → remaining 1.200
    createCardExpense(fx, "1000.00", "2026-02-05", 1);
    forceInvoiceClosedForTest(febInvoiceId);
    CreditCardInvoiceResponse february = getInvoice(fx.token(), febInvoiceId);
    assertThat(february.remainingAmount()).isEqualByComparingTo("1200.00");

    // futuros 9×200=1.800; net=900; financed=1.600; settlement=700; 10×320
    AgreementResponse agreement2 =
        renegotiate(fx, february.id(), "500.00", 10, "320.00", "900.00", status().isCreated());

    assertThat(agreement2.financedAmount()).isEqualByComparingTo("1600.00");
    assertThat(agreement2.contractedTotal()).isEqualByComparingTo("3200.00");
    assertThat(agreement2.additionalCost()).isEqualByComparingTo("1600.00");
    assertThat(agreement2.installmentAmount()).isEqualByComparingTo("320.00");
    assertThat(agreement2.installments()).hasSize(10);

    AgreementResponse previous = getAgreement(fx.token(), agreement1.id());
    assertThat(previous.status()).isEqualTo(CreditCardInvoiceAgreementStatus.RENEGOTIATED);
    assertThat(previous.supersededByAgreementId()).isEqualTo(agreement2.id());
    assertThat(previous.installments())
        .allSatisfy(i -> assertThat(i.remainingAmount()).isEqualByComparingTo("0.00"));

    // futures (2/10..10/10) have financial discount + incorporation reasons
    AgreementInstallmentResponse futureSample = previous.installments().get(1);
    var adjustments = listInstallmentAdjustments(fx, previous.expenseId(), futureSample.id());
    assertThat(adjustments)
        .extracting(a -> a.reason())
        .contains("Desconto financeiro por renegociação", "Incorporação de saldo à renegociação");
    BigDecimal discountTotal =
        adjustments.stream()
            .filter(a -> "Desconto financeiro por renegociação".equals(a.reason()))
            .map(a -> a.amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal incorporationTotal =
        adjustments.stream()
            .filter(a -> "Incorporação de saldo à renegociação".equals(a.reason()))
            .map(a -> a.amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    // One future installment: 100 discount + 100 incorporation (equal split of 1800→900/900)
    assertThat(discountTotal).isEqualByComparingTo("100.00");
    assertThat(incorporationTotal).isEqualByComparingTo("100.00");

    CreditCardLimitResponse limit = cardLimit(fx);
    assertThat(limit.usedLimit()).isEqualByComparingTo("3200.00");

    // Março: 1/10 do Agreement #2 = 320; parcela 2/10 do #1 não compromete (remaining 0)
    UUID marchInvoiceId = agreement2.installments().getFirst().invoiceId();
    assertThat(marchInvoiceId).isNotEqualTo(february.id());
    CreditCardInvoiceResponse march = getInvoice(fx.token(), marchInvoiceId);
    var marchItems = listInvoiceItems(fx, march.id());
    assertThat(marchItems)
        .anySatisfy(
            item -> {
              assertThat(item.expenseId()).isEqualTo(agreement2.expenseId());
              assertThat(item.amount()).isEqualByComparingTo("320.00");
              assertThat(item.remainingAmount()).isEqualByComparingTo("320.00");
            });
    assertThat(marchItems)
        .filteredOn(item -> item.expenseId().equals(agreement1.expenseId()))
        .allSatisfy(
            item -> {
              assertThat(item.remainingAmount()).isEqualByComparingTo("0.00");
              assertThat(item.status()).isEqualTo(ExpenseStatus.PAID);
            });
  }

  @Test
  void shouldRejectContractedTotalBelowFinancedAmountOnNewAgreement() throws Exception {
    Fixture fx = bootstrap("ct-lt-fin-new", "5000.00", "2000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "1000.00");
    // financed=600; contracted=10×50=500 < 600
    mockMvc
        .perform(
            post("/api/v1/invoices/" + source.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(agreementJson(fx.accountId(), "400.00", 10, "50.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value("O total contratado não pode ser menor que o valor financiado."));
  }

  @Test
  void shouldRejectContractedTotalBelowFinancedAmountOnRenegotiation() throws Exception {
    Fixture fx = bootstrap("ct-lt-fin-reneg", "5000.00", "5000.00");
    createCardExpense(fx, "1000.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    AgreementResponse first =
        createAgreement(
            fx, getInvoice(fx.token(), june.id()).id(), "0.00", 2, "600.00", status().isCreated());
    UUID julyId = first.installments().getFirst().invoiceId();
    createCardExpense(fx, "500.00", "2026-07-05", 1);
    closeUntilStatus(fx.token(), julyId, CreditCardInvoiceStatus.CLOSED);
    // remaining 1100; entry 100; futures net 600 → financed 1600; contracted 2×700=1400 < 1600
    mockMvc
        .perform(
            post("/api/v1/invoices/" + julyId + "/renegotiations")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(renegotiateJson(fx.accountId(), "100.00", 2, "700.00", "600.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value("O total contratado não pode ser menor que o valor financiado."));
  }

  @Test
  void shouldRejectAnticipatedFuturesNetAboveFutureOriginal() throws Exception {
    Fixture fx = bootstrap("net-gt-original", "5000.00", "5000.00");
    createCardExpense(fx, "1000.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    AgreementResponse first =
        createAgreement(
            fx, getInvoice(fx.token(), june.id()).id(), "0.00", 2, "600.00", status().isCreated());
    UUID julyId = first.installments().getFirst().invoiceId();
    createCardExpense(fx, "100.00", "2026-07-05", 1);
    closeUntilStatus(fx.token(), julyId, CreditCardInvoiceStatus.CLOSED);
    // futureOriginal=600; net=600.01
    mockMvc
        .perform(
            post("/api/v1/invoices/" + julyId + "/renegotiations")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(renegotiateJson(fx.accountId(), "50.00", 2, "700.00", "600.01")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "O valor líquido dos futuros não pode ultrapassar o saldo original dos"
                        + " futuros."));
  }

  @Test
  void shouldRejectNegativeAnticipatedFuturesNetAmount() throws Exception {
    Fixture fx = bootstrap("net-negative", "5000.00", "5000.00");
    createCardExpense(fx, "1000.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    AgreementResponse first =
        createAgreement(
            fx, getInvoice(fx.token(), june.id()).id(), "0.00", 2, "600.00", status().isCreated());
    UUID julyId = first.installments().getFirst().invoiceId();
    createCardExpense(fx, "100.00", "2026-07-05", 1);
    closeUntilStatus(fx.token(), julyId, CreditCardInvoiceStatus.CLOSED);
    mockMvc
        .perform(
            post("/api/v1/invoices/" + julyId + "/renegotiations")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(renegotiateJson(fx.accountId(), "50.00", 2, "700.00", "-0.01")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRenegotiateWithZeroFuturesNetAsFullFinancialDiscount() throws Exception {
    Fixture fx = bootstrap("net-zero", "5000.00", "5000.00");
    createCardExpense(fx, "1000.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    AgreementResponse first =
        createAgreement(
            fx, getInvoice(fx.token(), june.id()).id(), "0.00", 2, "600.00", status().isCreated());
    UUID julyId = first.installments().getFirst().invoiceId();
    createCardExpense(fx, "500.00", "2026-07-05", 1);
    closeUntilStatus(fx.token(), julyId, CreditCardInvoiceStatus.CLOSED);
    // settlement=1000; net=0 → financed=1000; contracted=1200
    AgreementResponse renegotiated =
        renegotiate(fx, julyId, "100.00", 2, "600.00", "0.00", status().isCreated());
    assertThat(renegotiated.financedAmount()).isEqualByComparingTo("1000.00");
    assertThat(getAgreement(fx.token(), first.id()).installments().get(1).remainingAmount())
        .isEqualByComparingTo("0.00");
  }

  @Test
  void shouldConsolidateFuturesFromMultipleActiveAgreements() throws Exception {
    Fixture fx = bootstrap("multi-active", "10000.00", "10000.00");

    createCardExpense(fx, "600.00", "2026-05-05", 1);
    CreditCardInvoiceResponse may = invoiceByClosing(fx, LocalDate.of(2026, 5, 10));
    closeUntilStatus(fx.token(), may.id(), CreditCardInvoiceStatus.CLOSED);
    AgreementResponse a1 =
        createAgreement(
            fx, getInvoice(fx.token(), may.id()).id(), "0.00", 2, "400.00", status().isCreated());

    createCardExpense(fx, "400.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    // nova negociação: does not touch a1
    AgreementResponse a2 =
        createAgreement(
            fx, getInvoice(fx.token(), june.id()).id(), "0.00", 2, "400.00", status().isCreated());
    assertThat(getAgreement(fx.token(), a1.id()).status())
        .isEqualTo(CreditCardInvoiceAgreementStatus.ACTIVE);

    // July: a1-2 (400) + a2-1 (400) + purchase 100 = 900
    createCardExpense(fx, "100.00", "2026-07-05", 1);
    UUID julyId = a2.installments().getFirst().invoiceId();
    closeUntilStatus(fx.token(), julyId, CreditCardInvoiceStatus.CLOSED);
    CreditCardInvoiceResponse july = getInvoice(fx.token(), julyId);
    assertThat(july.remainingAmount()).isEqualByComparingTo("900.00");

    // futures: a2 installment 2 only (400) — a1's July installment is on current invoice
    // net=200 → financed=(900-50)+200=1050; contracted=2×550=1100
    AgreementResponse a3 =
        renegotiate(fx, july.id(), "50.00", 2, "550.00", "200.00", status().isCreated());
    assertThat(a3.financedAmount()).isEqualByComparingTo("1050.00");
    assertThat(getAgreement(fx.token(), a1.id()).status())
        .isEqualTo(CreditCardInvoiceAgreementStatus.RENEGOTIATED);
    assertThat(getAgreement(fx.token(), a2.id()).status())
        .isEqualTo(CreditCardInvoiceAgreementStatus.RENEGOTIATED);
    assertThat(getAgreement(fx.token(), a2.id()).installments().get(1).remainingAmount())
        .isEqualByComparingTo("0.00");
  }

  // --- L17–L23 anticipate ---

  @Test
  void shouldL17ToL23AnticipateWithDiscountPartialCompleteOverpayAndAlreadyPaid() throws Exception {
    Fixture fx = bootstrap("l17-23", "5000.00", "5000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "1000.00");
    AgreementResponse agreement =
        createAgreement(fx, source.id(), "0.00", 10, "200.00", status().isCreated());
    AgreementInstallmentResponse first = agreement.installments().getFirst();
    assertThat(first.remainingAmount()).isEqualByComparingTo("200.00");

    // L17 / L18 / L21: settled=true pays 150 → discount 50 (25%)
    AgreementResponse afterSettled =
        anticipate(fx, agreement.id(), first.id(), "150.00", true, status().isOk());
    AgreementInstallmentResponse paid =
        afterSettled.installments().stream()
            .filter(i -> i.id().equals(first.id()))
            .findFirst()
            .orElseThrow();
    assertThat(paid.status()).isEqualTo(ExpenseStatus.PAID);
    assertThat(paid.remainingAmount()).isEqualByComparingTo("0.00");

    AgreementInstallmentResponse second = afterSettled.installments().get(1);

    // L19 partial without settled
    AgreementResponse afterPartial =
        anticipate(fx, agreement.id(), second.id(), "80.00", false, status().isOk());
    AgreementInstallmentResponse partial =
        afterPartial.installments().stream()
            .filter(i -> i.id().equals(second.id()))
            .findFirst()
            .orElseThrow();
    assertThat(partial.status()).isEqualTo(ExpenseStatus.PARTIALLY_PAID);
    assertThat(partial.remainingAmount()).isEqualByComparingTo("120.00");

    // L20 complete remaining
    AgreementResponse afterComplete =
        anticipate(fx, agreement.id(), second.id(), "120.00", false, status().isOk());
    assertThat(
            afterComplete.installments().stream()
                .filter(i -> i.id().equals(second.id()))
                .findFirst()
                .orElseThrow()
                .status())
        .isEqualTo(ExpenseStatus.PAID);

    // L22 overpay
    AgreementInstallmentResponse third = afterComplete.installments().get(2);
    mockMvc
        .perform(
            post("/api/v1/agreements/"
                    + agreement.id()
                    + "/installments/"
                    + third.id()
                    + "/anticipate")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(anticipateJson(fx.accountId(), "200.01", false)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(
            jsonPath("$.message")
                .value("O pagamento não pode ultrapassar o saldo restante da parcela."));

    // L23 already paid
    mockMvc
        .perform(
            post("/api/v1/agreements/"
                    + agreement.id()
                    + "/installments/"
                    + first.id()
                    + "/anticipate")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(anticipateJson(fx.accountId(), "10.00", false)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value("A parcela já está paga."));
  }

  // --- L24 ---

  @Test
  void shouldL24AllowNegativeAvailableLimitAfterAgreementContractedTotal() throws Exception {
    Fixture fx = bootstrap("l24-over", "100.00", "2000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "500.00");
    createAgreement(fx, source.id(), "0.00", 10, "80.00", status().isCreated());

    CreditCardLimitResponse limit = cardLimit(fx);
    assertThat(limit.creditLimit()).isEqualByComparingTo("100.00");
    assertThat(limit.usedLimit()).isEqualByComparingTo("800.00");
    assertThat(limit.availableLimit()).isEqualByComparingTo("-700.00");
  }

  // --- L25 / L26 ---

  @Test
  void shouldL25L26ListAgreementsAndExposeRenegotiationHistoryViaSupersededBy() throws Exception {
    Fixture fx = bootstrap("l25-26", "5000.00", "5000.00");

    createCardExpense(fx, "1000.00", "2026-06-05", 1);
    CreditCardInvoiceResponse june = invoiceByClosing(fx, LocalDate.of(2026, 6, 10));
    closeUntilStatus(fx.token(), june.id(), CreditCardInvoiceStatus.CLOSED);
    june = getInvoice(fx.token(), june.id());
    AgreementResponse first =
        createAgreement(fx, june.id(), "0.00", 2, "600.00", status().isCreated());

    // L25: list on source invoice
    List<AgreementResponse> onJune = listAgreements(fx, june.id());
    assertThat(onJune).hasSize(1);
    assertThat(onJune.getFirst().id()).isEqualTo(first.id());
    assertThat(getAgreement(fx.token(), first.id()).id()).isEqualTo(first.id());

    createCardExpense(fx, "100.00", "2026-07-05", 1);
    UUID julyId = first.installments().getFirst().invoiceId();
    closeUntilStatus(fx.token(), julyId, CreditCardInvoiceStatus.CLOSED);
    CreditCardInvoiceResponse july = getInvoice(fx.token(), julyId);
    AgreementResponse second =
        renegotiate(fx, july.id(), "50.00", 2, "400.00", "0.00", status().isCreated());

    AgreementResponse history = getAgreement(fx.token(), first.id());
    assertThat(history.status()).isEqualTo(CreditCardInvoiceAgreementStatus.RENEGOTIATED);
    assertThat(history.supersededByAgreementId()).isEqualTo(second.id());

    List<AgreementResponse> onJuly = listAgreements(fx, july.id());
    assertThat(onJuly).extracting(AgreementResponse::id).containsExactly(second.id());
  }

  // --- L27 / L28 / L29 ownership ---

  @Test
  void shouldL27L28L29Return404ForOtherUserInvoiceAgreementAndAccount() throws Exception {
    Fixture owner = bootstrap("l27-owner", "5000.00", "2000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(owner, "400.00");
    AgreementResponse agreement =
        createAgreement(owner, source.id(), "50.00", 5, "80.00", status().isCreated());

    Fixture other = bootstrap("l27-other");

    mockMvc
        .perform(
            get("/api/v1/invoices/" + source.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/v1/invoices/" + source.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(agreementJson(other.accountId(), "10.00", 3, "100.00")))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/v1/agreements/" + agreement.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
        .andExpect(status().isNotFound());

    // L29: owner using other user's account on a new closed invoice
    Fixture owner2 = bootstrap("l29-owner", "5000.00", "2000.00");
    CreditCardInvoiceResponse closed = closeJulyInvoice(owner2, "300.00");
    mockMvc
        .perform(
            post("/api/v1/invoices/" + closed.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner2.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(agreementJson(other.accountId(), "10.00", 3, "100.00")))
        .andExpect(status().isNotFound());
  }

  // --- L30 concurrency create agreement ---

  @Test
  void shouldL30PreventConcurrentAgreementsOnSameClosedInvoice() throws Exception {
    Fixture fx = bootstrap("l30", "5000.00", "3000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "1000.00");
    String body = agreementJson(fx.accountId(), "100.00", 10, "100.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first =
          pool.submit(() -> agreementStatus(fx.token(), source.id(), body, start));
      Future<Integer> second =
          pool.submit(() -> agreementStatus(fx.token(), source.id(), body, start));
      start.countDown();
      int statusA = first.get(30, TimeUnit.SECONDS);
      int statusB = second.get(30, TimeUnit.SECONDS);
      assertThat(List.of(statusA, statusB)).contains(201);
      assertThat(statusA == 201 ? statusB : statusA).isEqualTo(400);
    } finally {
      pool.shutdownNow();
    }
    assertThat(listAgreements(fx, source.id())).hasSize(1);
    assertThat(getInvoice(fx.token(), source.id()).status())
        .isEqualTo(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
  }

  // --- L31 concurrency anticipate ---

  @Test
  void shouldL31PreventConcurrentAnticipatesFromExceedingRemaining() throws Exception {
    Fixture fx = bootstrap("l31", "5000.00", "5000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "1000.00");
    AgreementResponse agreement =
        createAgreement(fx, source.id(), "0.00", 10, "200.00", status().isCreated());
    UUID installmentId = agreement.installments().getFirst().id();
    String body = anticipateJson(fx.accountId(), "200.00", false);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first =
          pool.submit(
              () -> anticipateStatus(fx.token(), agreement.id(), installmentId, body, start));
      Future<Integer> second =
          pool.submit(
              () -> anticipateStatus(fx.token(), agreement.id(), installmentId, body, start));
      start.countDown();
      int statusA = first.get(30, TimeUnit.SECONDS);
      int statusB = second.get(30, TimeUnit.SECONDS);
      assertThat(List.of(statusA, statusB)).contains(200);
      assertThat(statusA == 200 ? statusB : statusA).isEqualTo(400);
    } finally {
      pool.shutdownNow();
    }
    AgreementInstallmentResponse after =
        getAgreement(fx.token(), agreement.id()).installments().getFirst();
    assertThat(after.status()).isEqualTo(ExpenseStatus.PAID);
    assertThat(after.remainingAmount()).isEqualByComparingTo("0.00");
  }

  // --- L32 rollback soft ---

  @Test
  void shouldL32RejectEntryEqualRemainingWithoutCreatingAgreement() throws Exception {
    // Hard mid-flight rollback is not forced here; soft check: invalid entry leaves no agreement.
    Fixture fx = bootstrap("l32");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "400.00");
    BigDecimal balanceBefore = balance(fx.token(), fx.accountId());

    mockMvc
        .perform(
            post("/api/v1/invoices/" + source.id() + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(agreementJson(fx.accountId(), "400.00", 4, "100.00")))
        .andExpect(status().isBadRequest());

    assertThat(listAgreements(fx, source.id())).isEmpty();
    assertThat(getInvoice(fx.token(), source.id()).status())
        .isEqualTo(CreditCardInvoiceStatus.CLOSED);
    assertThat(getInvoice(fx.token(), source.id()).remainingAmount())
        .isEqualByComparingTo("400.00");
    assertThat(balance(fx.token(), fx.accountId())).isEqualByComparingTo(balanceBefore);
  }

  // --- L33 immutability ---

  @Test
  void shouldL33RejectPaymentOnSettledByAgreementInvoice() throws Exception {
    Fixture fx = bootstrap("l33", "5000.00", "2000.00");
    CreditCardInvoiceResponse source = closeJulyInvoice(fx, "500.00");
    createAgreement(fx, source.id(), "50.00", 5, "100.00", status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/invoices/" + source.id() + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payInvoiceJson(fx.accountId(), "10.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
        .andExpect(jsonPath("$.message").value("Fatura paga não pode ser alterada."));
  }

  // --- helpers ---

  private int agreementStatus(String token, UUID invoiceId, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/invoices/" + invoiceId + "/agreements")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private int anticipateStatus(
      String token, UUID agreementId, UUID installmentId, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/agreements/"
                    + agreementId
                    + "/installments/"
                    + installmentId
                    + "/anticipate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private CreditCardInvoiceResponse closeJulyInvoice(Fixture fx, String amount) throws Exception {
    createCardExpense(fx, amount, "2026-07-05", 1);
    CreditCardInvoiceResponse invoice = currentInvoice(fx);
    closeUntilStatus(fx.token(), invoice.id(), CreditCardInvoiceStatus.CLOSED);
    return getInvoice(fx.token(), invoice.id());
  }

  /**
   * Forces a specific invoice to CLOSED for long Agreement plans. With many SCHEDULED cycles, the
   * scheduler may open a later invoice whose closing_date is still in the future (relative to
   * system "today"), blocking the target invoice from ever opening via closeDueInvoices alone.
   * Future OPEN invoices are reverted to SCHEDULED so later Agreement installments can still link.
   */
  private void forceInvoiceClosedForTest(UUID invoiceId) {
    CreditCardInvoice target = invoiceRepository.findById(invoiceId).orElseThrow();
    UUID cardId = target.getCreditCard().getId();
    UUID userId = target.getUserId();
    java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
    for (CreditCardInvoice invoice :
        invoiceRepository.findAllByCreditCard_IdAndUserIdOrderByClosingDateAscIdAsc(
            cardId, userId)) {
      if (invoice.getStatus() == CreditCardInvoiceStatus.OPEN
          && !invoice.getId().equals(invoiceId)) {
        if (today.isBefore(invoice.getClosingDate())) {
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

  /**
   * Scheduler opens SCHEDULED then closes OPEN in separate passes. After an Agreement seeds the
   * next cycle as SCHEDULED, one call only opens it — call until the target status is reached.
   */
  private void closeUntilStatus(String token, UUID invoiceId, CreditCardInvoiceStatus expected)
      throws Exception {
    for (int i = 0; i < 8; i++) {
      CreditCardInvoiceResponse current = getInvoice(token, invoiceId);
      if (current.status() == expected) {
        return;
      }
      invoiceService.closeDueInvoices();
    }
    if (expected == CreditCardInvoiceStatus.CLOSED) {
      forceInvoiceClosedForTest(invoiceId);
    }
    assertThat(getInvoice(token, invoiceId).status()).isEqualTo(expected);
  }

  private AgreementResponse createAgreement(
      Fixture fx,
      UUID invoiceId,
      String entryAmount,
      int installmentCount,
      String installmentAmount,
      org.springframework.test.web.servlet.ResultMatcher statusMatcher)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/invoices/" + invoiceId + "/agreements")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        agreementJson(
                            fx.accountId(), entryAmount, installmentCount, installmentAmount)))
            .andExpect(statusMatcher)
            .andReturn();
    return read(result, AgreementResponse.class);
  }

  private AgreementResponse renegotiate(
      Fixture fx,
      UUID invoiceId,
      String entryAmount,
      int installmentCount,
      String installmentAmount,
      String anticipatedFuturesNetAmount,
      org.springframework.test.web.servlet.ResultMatcher statusMatcher)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/invoices/" + invoiceId + "/renegotiations")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        renegotiateJson(
                            fx.accountId(),
                            entryAmount,
                            installmentCount,
                            installmentAmount,
                            anticipatedFuturesNetAmount)))
            .andExpect(statusMatcher)
            .andReturn();
    return read(result, AgreementResponse.class);
  }

  private AgreementResponse anticipate(
      Fixture fx,
      UUID agreementId,
      UUID installmentId,
      String amount,
      boolean settled,
      org.springframework.test.web.servlet.ResultMatcher statusMatcher)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/agreements/"
                        + agreementId
                        + "/installments/"
                        + installmentId
                        + "/anticipate")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(anticipateJson(fx.accountId(), amount, settled)))
            .andExpect(statusMatcher)
            .andReturn();
    return read(result, AgreementResponse.class);
  }

  private List<AgreementResponse> listAgreements(Fixture fx, UUID invoiceId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/invoices/" + invoiceId + "/agreements")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return Arrays.asList(
        jsonMapper.readValue(result.getResponse().getContentAsString(), AgreementResponse[].class));
  }

  private AgreementResponse getAgreement(String token, UUID agreementId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/agreements/" + agreementId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, AgreementResponse.class);
  }

  private CreditCardLimitResponse cardLimit(Fixture fx) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + fx.cardId() + "/limit")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, CreditCardLimitResponse.class);
  }

  private Fixture bootstrap(String prefix) throws Exception {
    return bootstrap(prefix, "5000.00", "1000.00");
  }

  private Fixture bootstrap(String prefix, String cardLimit, String accountBalance)
      throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail(prefix), "senha-segura");
    CategoryResponse category = createExpenseCategory(token, "Cartão");
    AccountResponse account = createAccount(token, accountBalance);
    CreditCardResponse card = createCard(token, cardLimit);
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

  private ExpenseInstallmentResponse[] listInvoiceItems(Fixture fx, UUID invoiceId)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/invoices/" + invoiceId + "/items")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return jsonMapper.readValue(
        result.getResponse().getContentAsString(), ExpenseInstallmentResponse[].class);
  }

  private List<AdjustmentResponse> listInstallmentAdjustments(
      Fixture fx, UUID expenseId, UUID installmentId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/expenses/"
                        + expenseId
                        + "/installments/"
                        + installmentId
                        + "/adjustments")
                    .header(HttpHeaders.AUTHORIZATION, bearer(fx.token())))
            .andExpect(status().isOk())
            .andReturn();
    return Arrays.asList(
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), AdjustmentResponse[].class));
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

  private CreditCardInvoiceResponse invoiceByClosing(Fixture fx, LocalDate closingDate)
      throws Exception {
    return listInvoices(fx).stream()
        .filter(invoice -> closingDate.equals(invoice.closingDate()))
        .findFirst()
        .orElseThrow();
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

  private static String agreementJson(
      UUID accountId, String entryAmount, int installmentCount, String installmentAmount) {
    return """
        {"entryAmount":%s,"accountId":"%s","entryPaymentDate":"2026-08-15","installmentCount":%s,"installmentAmount":%s}
        """
        .formatted(entryAmount, accountId, installmentCount, installmentAmount);
  }

  private static String renegotiateJson(
      UUID accountId,
      String entryAmount,
      int installmentCount,
      String installmentAmount,
      String anticipatedFuturesNetAmount) {
    return """
        {"entryAmount":%s,"accountId":"%s","entryPaymentDate":"2026-08-15","installmentCount":%s,"installmentAmount":%s,"anticipatedFuturesNetAmount":%s}
        """
        .formatted(
            entryAmount,
            accountId,
            installmentCount,
            installmentAmount,
            anticipatedFuturesNetAmount);
  }

  private static String anticipateJson(UUID accountId, String amount, boolean settled) {
    return """
        {"accountId":"%s","amount":%s,"paymentDate":"2026-08-20","settled":%s}
        """
        .formatted(accountId, amount, settled);
  }

  private record Fixture(String token, UUID categoryId, UUID accountId, UUID cardId) {}
}
