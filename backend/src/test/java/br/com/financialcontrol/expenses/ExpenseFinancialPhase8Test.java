package br.com.financialcontrol.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.CategoryType;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import br.com.financialcontrol.expenses.dto.CreateExpenseRequest;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.expenses.dto.PayExpenseRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseInstallmentRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseRequest;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.PaymentStatus;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class ExpenseFinancialPhase8Test {

  @Autowired private MockMvc mockMvc;
  @Autowired private ExpenseService expenseService;
  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private ExpenseInstallmentRepository installmentRepository;
  @Autowired private ExpenseInstallmentAdjustmentRepository adjustmentRepository;
  @Autowired private PaymentRepository paymentRepository;

  @Test
  void shouldComputeRemainingFromActivePaymentsAndAdjustments() throws Exception {
    Context ctx = bootstrap("remaining");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "1000.00", 1, "2026-08-20");
    UUID installmentId = expense.installmentId();

    assertThat(remaining(user, expense.id(), installmentId)).isEqualByComparingTo("1000.00");

    expenseService.createAdjustment(
        user, expense.id(), installmentId, AdjustmentType.DISCOUNT, new BigDecimal("100.00"));
    assertThat(remaining(user, expense.id(), installmentId)).isEqualByComparingTo("900.00");

    expenseService.createAdjustment(
        user, expense.id(), installmentId, AdjustmentType.SURCHARGE, new BigDecimal("50.00"));
    assertThat(remaining(user, expense.id(), installmentId)).isEqualByComparingTo("950.00");

    expenseService.pay(user, expense.id(), pay(ctx.accountId(), "300.00"));
    assertThat(remaining(user, expense.id(), installmentId)).isEqualByComparingTo("650.00");

    expenseService.pay(user, expense.id(), pay(ctx.accountId(), "400.00"));
    assertThat(remaining(user, expense.id(), installmentId)).isEqualByComparingTo("250.00");

    ExpenseInstallmentAdjustment discount =
        adjustmentRepository
            .findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(installmentId, ctx.userId())
            .stream()
            .filter(a -> a.getType() == AdjustmentType.DISCOUNT)
            .findFirst()
            .orElseThrow();
    expenseService.reverseAdjustment(user, expense.id(), installmentId, discount.getId());
    assertThat(remaining(user, expense.id(), installmentId)).isEqualByComparingTo("350.00");

    Payment firstPayment =
        paymentRepository
            .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(expense.id(), ctx.userId())
            .getFirst();
    expenseService.reversePayment(user, firstPayment.getId());
    assertThat(remaining(user, expense.id(), installmentId)).isEqualByComparingTo("650.00");
    assertThat(
            paymentRepository
                .findByIdAndUserId(firstPayment.getId(), ctx.userId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(PaymentStatus.REVERSED);
    assertThat(
            paymentRepository
                .findByIdAndUserId(firstPayment.getId(), ctx.userId())
                .orElseThrow()
                .getAmount())
        .isEqualByComparingTo("300.00");
  }

  @Test
  void shouldPayInstallmentWithMultiplePaymentsDifferentAccountsAndStatusTransitions()
      throws Exception {
    Context ctx = bootstrap("pay-multi");
    AccountResponse second = createAccount(ctx.token(), "2000.00");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "1000.00", 3, "2026-08-20");
    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.id(), ctx.userId());
    UUID firstId = installments.getFirst().getId();

    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/installments/" + firstId + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(ctx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(second.id(), "100.00")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

    assertThat(balance(ctx.token(), ctx.accountId())).isEqualByComparingTo("5000.00");
    assertThat(balance(ctx.token(), second.id())).isEqualByComparingTo("1900.00");

    expenseService.payInstallment(user, expense.id(), firstId, pay(ctx.accountId(), "233.34"));
    ExpenseInstallment first =
        installmentRepository
            .findByIdAndExpense_IdAndUserId(firstId, expense.id(), ctx.userId())
            .orElseThrow();
    assertThat(first.getStatus()).isEqualTo(ExpenseStatus.PAID);
    assertThat(first.getAmount()).isEqualByComparingTo("333.34");
    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getTotalAmount())
        .isEqualByComparingTo("1000.00");

    assertThatThrownBy(
            () ->
                expenseService.payInstallment(
                    user, expense.id(), firstId, pay(ctx.accountId(), "0.01")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.PAYMENT_EXCEEDS_DUE);

    UUID secondInstallmentId = installments.get(1).getId();
    expenseService.createAdjustment(
        user,
        expense.id(),
        secondInstallmentId,
        AdjustmentType.SURCHARGE,
        new BigDecimal("100.00"));
    expenseService.payInstallment(
        user, expense.id(), secondInstallmentId, pay(ctx.accountId(), "333.33"));
    ExpenseInstallment secondInst =
        installmentRepository
            .findByIdAndExpense_IdAndUserId(secondInstallmentId, expense.id(), ctx.userId())
            .orElseThrow();
    assertThat(secondInst.getStatus()).isEqualTo(ExpenseStatus.PARTIALLY_PAID);
    assertThat(remaining(user, expense.id(), secondInstallmentId)).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldReversePaymentRestoreRemainingAndBalanceAndRejectTerminalExpense() throws Exception {
    Context ctx = bootstrap("reverse-pay");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "200.00", 1, "2026-08-20");
    expenseService.pay(user, expense.id(), pay(ctx.accountId(), "200.00"));
    assertThat(balance(ctx.token(), ctx.accountId())).isEqualByComparingTo("4800.00");

    Payment payment =
        paymentRepository
            .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(expense.id(), ctx.userId())
            .getFirst();
    PaymentResponse reversed = expenseService.reversePayment(user, payment.getId());
    assertThat(reversed.status()).isEqualTo(PaymentStatus.REVERSED);
    assertThat(balance(ctx.token(), ctx.accountId())).isEqualByComparingTo("5000.00");
    assertThat(remaining(user, expense.id(), expense.installmentId()))
        .isEqualByComparingTo("200.00");
    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getStatus())
        .isEqualTo(ExpenseStatus.OPEN);

    assertThatThrownBy(() -> expenseService.reversePayment(user, payment.getId()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.PAYMENT_ALREADY_REVERSED);

    expenseService.pay(user, expense.id(), pay(ctx.accountId(), "200.00"));
    expenseService.refund(user, expense.id());
    Payment active =
        paymentRepository
            .findAllByExpense_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
                expense.id(), ctx.userId(), PaymentStatus.ACTIVE)
            .getFirst();
    assertThatThrownBy(() -> expenseService.reversePayment(user, active.getId()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.PAYMENT_REVERSE_NOT_ALLOWED);
  }

  @Test
  void shouldManageAdjustmentsWithoutAffectingBalanceOrOriginalAmounts() throws Exception {
    Context ctx = bootstrap("adj");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "1000.00", 1, "2026-08-20");
    BigDecimal totalBefore =
        expenseRepository.findById(expense.id()).orElseThrow().getTotalAmount();
    BigDecimal installmentBefore =
        installmentRepository
            .findByIdAndExpense_IdAndUserId(expense.installmentId(), expense.id(), ctx.userId())
            .orElseThrow()
            .getAmount();
    BigDecimal balanceBefore = balance(ctx.token(), ctx.accountId());

    AdjustmentResponse discount =
        expenseService.createAdjustment(
            user,
            expense.id(),
            expense.installmentId(),
            AdjustmentType.DISCOUNT,
            new BigDecimal("100.00"));
    AdjustmentResponse surcharge =
        expenseService.createAdjustment(
            user,
            expense.id(),
            expense.installmentId(),
            AdjustmentType.SURCHARGE,
            new BigDecimal("40.00"));

    assertThat(balance(ctx.token(), ctx.accountId())).isEqualByComparingTo(balanceBefore);
    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getTotalAmount())
        .isEqualByComparingTo(totalBefore);
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(expense.installmentId(), expense.id(), ctx.userId())
                .orElseThrow()
                .getAmount())
        .isEqualByComparingTo(installmentBefore);
    assertThat(remaining(user, expense.id(), expense.installmentId()))
        .isEqualByComparingTo("940.00");

    assertThatThrownBy(
            () ->
                expenseService.createAdjustment(
                    user,
                    expense.id(),
                    expense.installmentId(),
                    AdjustmentType.DISCOUNT,
                    new BigDecimal("941.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ADJUSTMENT_INVALID_OBLIGATION);

    assertThatThrownBy(
            () ->
                expenseService.createAdjustment(
                    user,
                    expense.id(),
                    expense.installmentId(),
                    AdjustmentType.DISCOUNT,
                    BigDecimal.ZERO))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ADJUSTMENT_AMOUNT_MUST_BE_POSITIVE);

    expenseService.pay(user, expense.id(), pay(ctx.accountId(), "400.00"));
    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getStatus())
        .isEqualTo(ExpenseStatus.PARTIALLY_PAID);

    expenseService.reverseAdjustment(user, expense.id(), expense.installmentId(), surcharge.id());
    assertThat(
            adjustmentRepository
                .findByIdAndUserId(surcharge.id(), ctx.userId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(AdjustmentStatus.REVERSED);
    assertThat(
            adjustmentRepository
                .findByIdAndUserId(surcharge.id(), ctx.userId())
                .orElseThrow()
                .getAmount())
        .isEqualByComparingTo("40.00");
    assertThat(remaining(user, expense.id(), expense.installmentId()))
        .isEqualByComparingTo("500.00");
    assertThat(balance(ctx.token(), ctx.accountId())).isEqualByComparingTo("4600.00");

    assertThatThrownBy(
            () ->
                expenseService.reverseAdjustment(
                    user, expense.id(), expense.installmentId(), surcharge.id()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ADJUSTMENT_ALREADY_REVERSED);

    expenseService.reverseAdjustment(user, expense.id(), expense.installmentId(), discount.id());
    assertThat(remaining(user, expense.id(), expense.installmentId()))
        .isEqualByComparingTo("600.00");

    expenseService.refund(user, expense.id());
    ExpenseInstallmentAdjustment another =
        adjustmentRepository
            .findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
                expense.installmentId(), ctx.userId())
            .getFirst();
    assertThatThrownBy(
            () ->
                expenseService.reverseAdjustment(
                    user, expense.id(), expense.installmentId(), another.getId()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ADJUSTMENT_REVERSE_NOT_ALLOWED);
  }

  @Test
  void shouldRejectPutOneToOneWhenActiveDiscountWouldInvalidateObligation() throws Exception {
    Context ctx = bootstrap("put-disc");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "100.00", 1, "2026-08-20");
    expenseService.createAdjustment(
        user,
        expense.id(),
        expense.installmentId(),
        AdjustmentType.DISCOUNT,
        new BigDecimal("40.00"));

    assertThatThrownBy(
            () -> expenseService.update(user, expense.id(), updateRequest(ctx, expense, "30.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.INVALID_INSTALLMENT_OBLIGATION);

    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getTotalAmount())
        .isEqualByComparingTo("100.00");
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(expense.installmentId(), expense.id(), ctx.userId())
                .orElseThrow()
                .getAmount())
        .isEqualByComparingTo("100.00");

    expenseService.update(user, expense.id(), updateRequest(ctx, expense, "50.00"));
    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getTotalAmount())
        .isEqualByComparingTo("50.00");
    assertThat(remaining(user, expense.id(), expense.installmentId()))
        .isEqualByComparingTo("10.00");
  }

  @Test
  void shouldRejectInstallmentAmountEditWhenItBreaksSumEvenWithActiveDiscount() throws Exception {
    Context ctx = bootstrap("inst-disc");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "1000.00", 3, "2026-01-31");
    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.id(), ctx.userId());
    ExpenseInstallment first = installments.getFirst();
    expenseService.createAdjustment(
        user, expense.id(), first.getId(), AdjustmentType.DISCOUNT, new BigDecimal("100.00"));

    // Single-parcel amount change cannot keep SUM = total (RN227); sum guard fires first.
    assertThatThrownBy(
            () ->
                expenseService.updateInstallment(
                    user,
                    expense.id(),
                    first.getId(),
                    new UpdateExpenseInstallmentRequest(
                        new BigDecimal("83.34"), first.getDueDate())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.INSTALLMENT_SUM_MISMATCH);

    // dueDate-only edit with ACTIVE DISCOUNT must remain allowed (obligation unchanged).
    expenseService.updateInstallment(
        user,
        expense.id(),
        first.getId(),
        new UpdateExpenseInstallmentRequest(first.getAmount(), LocalDate.of(2026, 1, 30)));
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(first.getId(), expense.id(), ctx.userId())
                .orElseThrow()
                .getDueDate())
        .isEqualTo(LocalDate.of(2026, 1, 30));
    assertThat(remaining(user, expense.id(), first.getId())).isEqualByComparingTo("233.34");
  }

  @Test
  void shouldRefundMixedWithReversedAndUnpaidInstallments() throws Exception {
    Context ctx = bootstrap("refund-rev");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "1200.00", 4, "2026-01-10");
    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.id(), ctx.userId());
    UUID p1 = installments.get(0).getId();
    UUID p2 = installments.get(1).getId();
    UUID p3 = installments.get(2).getId();
    UUID p4 = installments.get(3).getId();

    expenseService.payInstallment(user, expense.id(), p1, pay(ctx.accountId(), "300.00"));
    expenseService.payInstallment(user, expense.id(), p2, pay(ctx.accountId(), "300.00"));
    Payment p2Payment =
        paymentRepository
            .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(expense.id(), ctx.userId())
            .stream()
            .filter(p -> p.getInstallment().getId().equals(p2))
            .findFirst()
            .orElseThrow();
    expenseService.reversePayment(user, p2Payment.getId());
    expenseService.payInstallment(user, expense.id(), p4, pay(ctx.accountId(), "300.00"));

    expenseService.refund(user, expense.id());

    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getStatus())
        .isEqualTo(ExpenseStatus.REFUNDED);
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(p1, expense.id(), ctx.userId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(ExpenseStatus.REFUNDED);
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(p2, expense.id(), ctx.userId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(ExpenseStatus.OPEN);
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(p3, expense.id(), ctx.userId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(ExpenseStatus.OPEN);
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(p4, expense.id(), ctx.userId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(ExpenseStatus.REFUNDED);
  }

  @Test
  void shouldCancelMultiInstallmentOpenExpenseAndAllParcels() throws Exception {
    Context ctx = bootstrap("cancel-n");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "900.00", 3, "2026-08-20");

    expenseService.cancel(user, expense.id());

    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getStatus())
        .isEqualTo(ExpenseStatus.CANCELLED);
    assertThat(
            installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
                expense.id(), ctx.userId()))
        .allMatch(i -> i.getStatus() == ExpenseStatus.CANCELLED);
  }

  @Test
  void shouldRejectLegacyPayEndpointForMultiInstallmentExpense() throws Exception {
    Context ctx = bootstrap("pay-n");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "900.00", 3, "2026-08-20");

    assertThatThrownBy(() -> expenseService.pay(user, expense.id(), pay(ctx.accountId(), "100.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.PAY_REQUIRES_SINGLE_INSTALLMENT);

    mockMvc
        .perform(
            post("/api/v1/expenses/" + expense.id() + "/pay")
                .header(HttpHeaders.AUTHORIZATION, bearer(ctx.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payJson(ctx.accountId(), "100.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  @Test
  void shouldRefundMixedInstallmentsAndBlockPayAdjustmentOverdueOnOpenUnpaidParcel()
      throws Exception {
    Context ctx = bootstrap("refund-mixed");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "900.00", 3, "2026-01-10");
    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.id(), ctx.userId());
    UUID paidInstallmentId = installments.getFirst().getId();
    UUID openInstallmentId = installments.get(1).getId();

    expenseService.payInstallment(
        user, expense.id(), paidInstallmentId, pay(ctx.accountId(), "300.00"));
    expenseService.refund(user, expense.id());

    Expense refunded = expenseRepository.findById(expense.id()).orElseThrow();
    assertThat(refunded.getStatus()).isEqualTo(ExpenseStatus.REFUNDED);
    assertThat(
            installmentRepository
                .findByIdAndExpense_IdAndUserId(paidInstallmentId, expense.id(), ctx.userId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(ExpenseStatus.REFUNDED);
    ExpenseInstallment openParcel =
        installmentRepository
            .findByIdAndExpense_IdAndUserId(openInstallmentId, expense.id(), ctx.userId())
            .orElseThrow();
    assertThat(openParcel.getStatus()).isEqualTo(ExpenseStatus.OPEN);
    assertThat(
            paymentRepository.findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(
                expense.id(), ctx.userId()))
        .hasSize(1);

    assertThatThrownBy(
            () ->
                expenseService.payInstallment(
                    user, expense.id(), openInstallmentId, pay(ctx.accountId(), "10.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ONLY_OPEN_OR_PARTIAL_CAN_BE_PAID);

    assertThatThrownBy(
            () ->
                expenseService.createAdjustment(
                    user,
                    expense.id(),
                    openInstallmentId,
                    AdjustmentType.DISCOUNT,
                    new BigDecimal("10.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.INSTALLMENT_NOT_ADJUSTABLE);

    assertThatThrownBy(
            () ->
                expenseService.updateInstallment(
                    user,
                    expense.id(),
                    openInstallmentId,
                    new UpdateExpenseInstallmentRequest(
                        openParcel.getAmount(), openParcel.getDueDate())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.TERMINAL_EXPENSE_INSTALLMENT_IMMUTABLE);

    ExpenseInstallmentResponse openResponse =
        expenseService.getInstallment(user, expense.id(), openInstallmentId);
    assertThat(openResponse.overdue()).isFalse();
    assertThat(expenseService.get(user, expense.id()).overdue()).isFalse();
  }

  @Test
  void shouldDeriveOverdueFromFinancialRemaining() throws Exception {
    Context ctx = bootstrap("overdue");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "100.00", 2, "2020-01-31");
    List<ExpenseInstallment> installments =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.id(), ctx.userId());

    assertThat(expenseService.get(user, expense.id()).overdue()).isTrue();
    assertThat(
            expenseService
                .getInstallment(user, expense.id(), installments.getFirst().getId())
                .overdue())
        .isTrue();

    expenseService.payInstallment(
        user, expense.id(), installments.getFirst().getId(), pay(ctx.accountId(), "50.00"));
    assertThat(
            expenseService
                .getInstallment(user, expense.id(), installments.getFirst().getId())
                .remainingAmount())
        .isEqualByComparingTo("0.00");
    assertThat(
            expenseService
                .getInstallment(user, expense.id(), installments.getFirst().getId())
                .overdue())
        .isFalse();
    assertThat(expenseService.get(user, expense.id()).overdue()).isTrue();

    expenseService.payInstallment(
        user, expense.id(), installments.get(1).getId(), pay(ctx.accountId(), "50.00"));
    assertThat(expenseService.get(user, expense.id()).overdue()).isFalse();
  }

  @Test
  void shouldRejectConcurrentPaymentsExceedingRemaining() throws Exception {
    Context ctx = bootstrap("concurrency");
    ExpenseResponse expense = createExpense(ctx, "500.00", 1, "2026-08-20");
    String body = payJson(ctx.accountId(), "300.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    AtomicInteger successes = new AtomicInteger();
    try {
      Future<Integer> first =
          pool.submit(() -> payLegacyStatus(ctx.token(), expense.id(), body, start));
      Future<Integer> second =
          pool.submit(() -> payLegacyStatus(ctx.token(), expense.id(), body, start));
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
    BigDecimal activeTotal =
        paymentRepository
            .findAllByExpense_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
                expense.id(), ctx.userId(), PaymentStatus.ACTIVE)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(activeTotal).isEqualByComparingTo("300.00");
  }

  @Test
  void shouldHideFinancialFactsFromOtherUser() throws Exception {
    Context owner = bootstrap("own");
    Context other = bootstrap("other");
    AuthenticatedUser ownerUser = new AuthenticatedUser(owner.userId());
    AuthenticatedUser otherUser = new AuthenticatedUser(other.userId());
    ExpenseResponse expense = createExpense(owner, "100.00", 1, "2026-08-20");
    expenseService.pay(ownerUser, expense.id(), pay(owner.accountId(), "40.00"));
    Payment payment =
        paymentRepository
            .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(expense.id(), owner.userId())
            .getFirst();
    AdjustmentResponse adjustment =
        expenseService.createAdjustment(
            ownerUser,
            expense.id(),
            expense.installmentId(),
            AdjustmentType.DISCOUNT,
            new BigDecimal("10.00"));

    assertThatThrownBy(() -> expenseService.getPayment(otherUser, payment.getId()))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> expenseService.reversePayment(otherUser, payment.getId()))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(
            () ->
                expenseService.reverseAdjustment(
                    otherUser, expense.id(), expense.installmentId(), adjustment.id()))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(
            () -> expenseService.pay(otherUser, expense.id(), pay(other.accountId(), "10.00")))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void shouldExposePaymentStatusInHistoryAndReverseEndpoint() throws Exception {
    Context ctx = bootstrap("api-status");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "80.00", 1, "2026-08-20");
    expenseService.pay(user, expense.id(), pay(ctx.accountId(), "80.00"));
    Payment payment =
        paymentRepository
            .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(expense.id(), ctx.userId())
            .getFirst();

    mockMvc
        .perform(
            get("/api/v1/expenses/" + expense.id() + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(ctx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));

    mockMvc
        .perform(
            post("/api/v1/payments/" + payment.getId() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(ctx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVERSED"));

    mockMvc
        .perform(
            get("/api/v1/expenses/" + expense.id() + "/installments")
                .header(HttpHeaders.AUTHORIZATION, bearer(ctx.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].remainingAmount").value(80.00));
  }

  @Test
  void shouldRejectRefundOfOpenExpenseEvenWhenMultiInstallmentWithoutPayments() throws Exception {
    Context ctx = bootstrap("refund-open");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "300.00", 3, "2026-08-20");

    assertThatThrownBy(() -> expenseService.refund(user, expense.id()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ONLY_PAID_OR_PARTIAL_CAN_BE_REFUNDED);
    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getStatus())
        .isEqualTo(ExpenseStatus.OPEN);
  }

  @Test
  void shouldTreatFullyReversedPaymentsAsOpenAndRejectRefund() throws Exception {
    Context ctx = bootstrap("refund-reversed-only");
    AuthenticatedUser user = new AuthenticatedUser(ctx.userId());
    ExpenseResponse expense = createExpense(ctx, "100.00", 2, "2026-08-20");
    UUID installmentId =
        installmentRepository
            .findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(expense.id(), ctx.userId())
            .getFirst()
            .getId();

    expenseService.payInstallment(user, expense.id(), installmentId, pay(ctx.accountId(), "50.00"));
    Payment payment =
        paymentRepository
            .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(expense.id(), ctx.userId())
            .getFirst();
    expenseService.reversePayment(user, payment.getId());

    assertThat(expenseRepository.findById(expense.id()).orElseThrow().getStatus())
        .isEqualTo(ExpenseStatus.OPEN);
    assertThatThrownBy(() -> expenseService.refund(user, expense.id()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ONLY_PAID_OR_PARTIAL_CAN_BE_REFUNDED);
  }

  @Test
  void shouldSplitVerySmallTotalAcrossInstallmentsWithoutLosingCents() {
    assertThat(ExpenseService.splitInstallmentAmounts(new BigDecimal("0.01"), 3))
        .containsExactly(new BigDecimal("0.01"), new BigDecimal("0.00"), new BigDecimal("0.00"));
    assertThat(ExpenseService.splitInstallmentAmounts(new BigDecimal("0.02"), 3))
        .containsExactly(new BigDecimal("0.02"), new BigDecimal("0.00"), new BigDecimal("0.00"));
    assertThat(ExpenseService.splitInstallmentAmounts(new BigDecimal("0.05"), 2))
        .containsExactly(new BigDecimal("0.03"), new BigDecimal("0.02"));
  }

  private BigDecimal remaining(AuthenticatedUser user, UUID expenseId, UUID installmentId) {
    return expenseService.getInstallment(user, expenseId, installmentId).remainingAmount();
  }

  private int payLegacyStatus(String token, UUID expenseId, String body, CountDownLatch start)
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

  private ExpenseResponse createExpense(
      Context ctx, String total, int installments, String dueDate) {
    return expenseService.create(
        new AuthenticatedUser(ctx.userId()),
        new CreateExpenseRequest(
            ctx.categoryId(),
            "Despesa",
            new BigDecimal(total),
            LocalDate.of(2026, 8, 1),
            LocalDate.parse(dueDate),
            PaymentMethod.ACCOUNT,
            ctx.accountId(),
            ResponsibleType.MINE,
            null,
            null,
            null,
            installments,
            null));
  }

  private static UpdateExpenseRequest updateRequest(
      Context ctx, ExpenseResponse expense, String totalAmount) {
    return new UpdateExpenseRequest(
        ctx.categoryId(),
        expense.description(),
        new BigDecimal(totalAmount),
        expense.expenseDate(),
        expense.dueDate(),
        expense.paymentMethod(),
        ctx.accountId(),
        expense.responsibleType(),
        expense.responsibleName(),
        expense.barcode(),
        expense.notes(),
        null);
  }

  private static PayExpenseRequest pay(UUID accountId, String amount) {
    return new PayExpenseRequest(
        accountId, new BigDecimal(amount), LocalDate.of(2026, 8, 12), null);
  }

  private static String payJson(UUID accountId, String amount) {
    return """
        {"accountId":"%s","amount":%s,"paymentDate":"2026-08-12"}
        """
        .formatted(accountId, amount);
  }

  private Context bootstrap(String prefix) throws Exception {
    String email = uniqueEmail(prefix);
    String token = registerAndLogin("User", email, "senha-segura");
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
    return new Context(token, userId, category.id(), account.id());
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
    MvcResult login =
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
    return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
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
    return new CategoryResponse(
        UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id")),
        name,
        CategoryType.EXPENSE,
        true,
        null,
        null);
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
    String body = result.getResponse().getContentAsString();
    return new AccountResponse(
        UUID.fromString(JsonPath.read(body, "$.id")),
        JsonPath.read(body, "$.name"),
        br.com.financialcontrol.accounts.AccountType.BANK_ACCOUNT,
        new BigDecimal(initialBalance),
        true,
        null,
        null);
  }

  private BigDecimal balance(String token, UUID accountId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + accountId + "/balance")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    AccountBalanceResponse response =
        new AccountBalanceResponse(
            accountId,
            new BigDecimal(
                JsonPath.read(result.getResponse().getContentAsString(), "$.balance").toString()));
    return response.balance();
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private record Context(String token, UUID userId, UUID categoryId, UUID accountId) {}
}
