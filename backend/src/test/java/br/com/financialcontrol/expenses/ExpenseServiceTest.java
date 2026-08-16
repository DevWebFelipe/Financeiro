package br.com.financialcontrol.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.accounts.AccountType;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryService;
import br.com.financialcontrol.categories.CategoryType;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_cards.CardPurchaseAccountRefundRepository;
import br.com.financialcontrol.credit_cards.CreditCardCreditApplicationRepository;
import br.com.financialcontrol.credit_cards.CreditCardService;
import br.com.financialcontrol.expenses.dto.CreateExpenseRequest;
import br.com.financialcontrol.expenses.dto.ExpensePageResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.expenses.dto.PayExpenseRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseRequest;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.PaymentStatus;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
  private static final LocalDate EXPENSE_DATE = LocalDate.of(2026, 8, 10);
  private static final LocalDate DUE_DATE = LocalDate.of(2026, 8, 20);
  private static final LocalDate PAST_DUE = LocalDate.of(2026, 8, 1);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 8, 12);
  private static final UUID USER_A = UUID.fromString("01800000-0000-7000-8000-00000000000a");
  private static final UUID USER_B = UUID.fromString("01800000-0000-7000-8000-00000000000b");
  private static final UUID EXPENSE_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ee");
  private static final UUID INSTALLMENT_ID =
      UUID.fromString("01800000-0000-7000-8000-000000000011");
  private static final UUID CATEGORY_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ca");
  private static final UUID ACCOUNT_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ac");
  private static final UUID OTHER_ACCOUNT_ID =
      UUID.fromString("01800000-0000-7000-8000-0000000000a2");
  private static final UUID PAYMENT_ID = UUID.fromString("01800000-0000-7000-8000-0000000000f1");

  @Mock private ExpenseRepository expenseRepository;
  @Mock private ExpenseInstallmentRepository expenseInstallmentRepository;
  @Mock private ExpenseInstallmentAdjustmentRepository adjustmentRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private AccountService accountService;
  @Mock private CategoryService categoryService;
  @Mock private CreditCardService creditCardService;
  @Mock private CreditCardInvoiceService creditCardInvoiceService;
  @Mock private InstallmentBalanceService installmentBalanceService;
  @Mock private CreditCardInvoicePaymentAllocationRepository invoicePaymentAllocationRepository;
  @Mock private CreditCardCreditApplicationRepository creditApplicationRepository;
  @Mock private CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository;

  private ExpenseService expenseService;

  @BeforeEach
  void setUp() {
    expenseService = serviceWith(Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldCreateAccountExpenseOpenWithInstallmentAndWithoutPayment() {
    stubCreateDependencies(activeAccount());

    ExpenseResponse response =
        expenseService.create(new AuthenticatedUser(USER_A), accountCreateRequest());

    Expense expense = captureSavedExpense();
    ExpenseInstallment installment = captureSavedInstallment();

    assertThat(expense.getId()).isNotNull();
    assertThat(expense.getId().version()).isEqualTo(7);
    assertThat(expense.getUserId()).isEqualTo(USER_A).isNotEqualTo(USER_B);
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.OPEN);
    assertThat(expense.getPaymentMethod()).isEqualTo(PaymentMethod.ACCOUNT);
    assertThat(expense.getAccount().getId()).isEqualTo(ACCOUNT_ID);
    assertThat(expense.getCreditCard()).isNull();
    assertThat(expense.getTotalAmount()).isEqualByComparingTo("150.00");
    assertThat(expense.getResponsibleType()).isEqualTo(ResponsibleType.MINE);
    assertThat(expense.getResponsibleName()).isNull();
    assertThat(expense.getBarcode()).isEqualTo("23793381286000000000000000000000000000000000");

    assertThat(installment.getId().version()).isEqualTo(7);
    assertThat(installment.getUserId()).isEqualTo(USER_A);
    assertThat(installment.getExpense()).isSameAs(expense);
    assertThat(installment.getInvoice()).isNull();
    assertThat(installment.getInstallmentNumber()).isEqualTo(1);
    assertThat(installment.getTotalInstallments()).isEqualTo(1);
    assertThat(installment.getAmount()).isEqualByComparingTo("150.00");
    assertThat(installment.getDueDate()).isEqualTo(DUE_DATE);
    assertThat(installment.getStatus()).isEqualTo(ExpenseStatus.OPEN);

    assertThat(response.status()).isEqualTo(ExpenseStatus.OPEN);
    assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
    assertThat(response.overdue()).isFalse();
    assertThat(response.installmentId()).isEqualTo(installment.getId());
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void shouldCreateNoneExpenseWithoutAccountId() {
    when(categoryService.requireActiveOwnedExpenseCategory(USER_A, CATEGORY_ID))
        .thenReturn(expenseCategory());
    stubSaves();

    ExpenseResponse response =
        expenseService.create(new AuthenticatedUser(USER_A), noneCreateRequest());

    Expense expense = captureSavedExpense();
    assertThat(expense.getPaymentMethod()).isEqualTo(PaymentMethod.NONE);
    assertThat(expense.getAccount()).isNull();
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.OPEN);
    assertThat(response.accountId()).isNull();
    verify(accountService, never()).requireActiveOwnedAccount(any(), any());
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void shouldRejectAccountExpenseWithoutAccount() {
    when(categoryService.requireActiveOwnedExpenseCategory(USER_A, CATEGORY_ID))
        .thenReturn(expenseCategory());

    assertThatThrownBy(
            () ->
                expenseService.create(
                    new AuthenticatedUser(USER_A),
                    new CreateExpenseRequest(
                        CATEGORY_ID,
                        "Luz",
                        new BigDecimal("150.00"),
                        EXPENSE_DATE,
                        DUE_DATE,
                        PaymentMethod.ACCOUNT,
                        null,
                        ResponsibleType.MINE,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ACCOUNT_REQUIRED_FOR_ACCOUNT_METHOD);
    verify(expenseRepository, never()).save(any());
  }

  @Test
  void shouldRejectNoneExpenseWithAccountOnCreate() {
    when(categoryService.requireActiveOwnedExpenseCategory(USER_A, CATEGORY_ID))
        .thenReturn(expenseCategory());

    assertThatThrownBy(
            () ->
                expenseService.create(
                    new AuthenticatedUser(USER_A),
                    new CreateExpenseRequest(
                        CATEGORY_ID,
                        "Luz",
                        new BigDecimal("150.00"),
                        EXPENSE_DATE,
                        DUE_DATE,
                        PaymentMethod.NONE,
                        ACCOUNT_ID,
                        ResponsibleType.MINE,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ACCOUNT_NOT_ALLOWED_FOR_NONE);
    verify(expenseRepository, never()).save(any());
  }

  @Test
  void shouldRejectCreditCardExpense() {
    when(categoryService.requireActiveOwnedExpenseCategory(USER_A, CATEGORY_ID))
        .thenReturn(expenseCategory());

    assertThatThrownBy(
            () ->
                expenseService.create(
                    new AuthenticatedUser(USER_A),
                    new CreateExpenseRequest(
                        CATEGORY_ID,
                        "Compra",
                        new BigDecimal("150.00"),
                        EXPENSE_DATE,
                        DUE_DATE,
                        PaymentMethod.CREDIT_CARD,
                        null,
                        ResponsibleType.MINE,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.CREDIT_CARD_REQUIRED);
    verify(expenseRepository, never()).save(any());
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void shouldRequireOtherResponsibleName() {
    when(categoryService.requireActiveOwnedExpenseCategory(USER_A, CATEGORY_ID))
        .thenReturn(expenseCategory());
    when(accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID)).thenReturn(activeAccount());

    assertThatThrownBy(
            () ->
                expenseService.create(
                    new AuthenticatedUser(USER_A),
                    new CreateExpenseRequest(
                        CATEGORY_ID,
                        "Luz",
                        new BigDecimal("150.00"),
                        EXPENSE_DATE,
                        DUE_DATE,
                        PaymentMethod.ACCOUNT,
                        ACCOUNT_ID,
                        ResponsibleType.OTHER,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.OTHER_REQUIRES_NAME);
    verify(expenseRepository, never()).save(any());
  }

  @Test
  void shouldPersistOtherResponsibleNameAndIgnoreNameForMine() {
    stubCreateDependencies(activeAccount());

    expenseService.create(
        new AuthenticatedUser(USER_A),
        new CreateExpenseRequest(
            CATEGORY_ID,
            "Luz",
            new BigDecimal("80.00"),
            EXPENSE_DATE,
            DUE_DATE,
            PaymentMethod.ACCOUNT,
            ACCOUNT_ID,
            ResponsibleType.OTHER,
            "Vizinho",
            null,
            null,
            null,
            null));

    assertThat(captureSavedExpense().getResponsibleName()).isEqualTo("Vizinho");
  }

  @Test
  void shouldGetOwnedExpense() {
    Expense expense = openAccountExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_A)).thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);

    ExpenseResponse response = expenseService.get(new AuthenticatedUser(USER_A), EXPENSE_ID);

    assertThat(response.id()).isEqualTo(EXPENSE_ID);
    assertThat(response.status()).isEqualTo(ExpenseStatus.OPEN);
    assertThat(response.installmentId()).isEqualTo(INSTALLMENT_ID);
  }

  @Test
  void shouldListOwnedExpensesWithDefaultPagination() {
    Expense expense = openAccountExpense();
    when(expenseRepository.searchByUser(
            eq(USER_A),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            any(Pageable.class)))
        .thenReturn(
            new PageImpl<>(
                List.of(expense),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")),
                1));
    when(expenseInstallmentRepository
            .findAllByExpense_IdInAndUserIdOrderByExpense_IdAscInstallmentNumberAsc(
                any(), eq(USER_A)))
        .thenReturn(List.of(singleInstallment(expense)));

    ExpensePageResponse response =
        expenseService.list(
            new AuthenticatedUser(USER_A), null, null, null, null, null, null, null, null, 0, 20);

    assertThat(response.items()).hasSize(1);
    assertThat(response.page()).isEqualTo(0);
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(expenseRepository)
        .searchByUser(
            eq(USER_A),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
        .isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void shouldUpdateOpenExpenseAndKeepInstallmentConsistent() {
    Expense expense = openAccountExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);
    when(expenseInstallmentRepository.findSingleByExpenseIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(installment));
    when(categoryService.requireActiveOwnedExpenseCategory(USER_A, CATEGORY_ID))
        .thenReturn(expenseCategory());
    when(accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID)).thenReturn(activeAccount());
    stubSaves();

    LocalDate newDue = LocalDate.of(2026, 9, 1);
    ExpenseResponse response =
        expenseService.update(
            new AuthenticatedUser(USER_A),
            EXPENSE_ID,
            new UpdateExpenseRequest(
                CATEGORY_ID,
                "Energia",
                new BigDecimal("199.90"),
                EXPENSE_DATE,
                newDue,
                PaymentMethod.ACCOUNT,
                ACCOUNT_ID,
                ResponsibleType.GIULIA,
                "ignorado",
                "123",
                "nota",
                null));

    assertThat(response.description()).isEqualTo("Energia");
    assertThat(response.totalAmount()).isEqualByComparingTo("199.90");
    assertThat(response.dueDate()).isEqualTo(newDue);
    assertThat(response.responsibleType()).isEqualTo(ResponsibleType.GIULIA);
    assertThat(response.responsibleName()).isNull();
    assertThat(expense.getTotalAmount()).isEqualByComparingTo("199.90");
    assertThat(installment.getAmount()).isEqualByComparingTo("199.90");
    assertThat(installment.getDueDate()).isEqualTo(newDue);
  }

  @Test
  void shouldRejectUpdateWhenNotOpen() {
    for (ExpenseStatus status :
        List.of(
            ExpenseStatus.PARTIALLY_PAID,
            ExpenseStatus.PAID,
            ExpenseStatus.CANCELLED,
            ExpenseStatus.REFUNDED)) {
      Expense expense = openAccountExpense();
      expense.setStatus(status);
      when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
          .thenReturn(Optional.of(expense));

      assertThatThrownBy(
              () ->
                  expenseService.update(
                      new AuthenticatedUser(USER_A), EXPENSE_ID, accountUpdateRequest()))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage(ExpenseService.ONLY_OPEN_CAN_BE_EDITED);
    }
    verify(expenseRepository, never()).save(any());
  }

  @Test
  void shouldPayAccountExpenseInFullUsingExpenseAccountWhenOmitted() {
    Expense expense = openAccountExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    stubPay(expense, installment, BigDecimal.ZERO, "1500.00");

    ExpenseResponse response =
        expenseService.pay(new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(null, "150.00"));

    Payment payment = captureSavedPayment();
    assertThat(payment.getUserId()).isEqualTo(USER_A);
    assertThat(payment.getExpense()).isSameAs(expense);
    assertThat(payment.getInstallment()).isSameAs(installment);
    assertThat(payment.getAccount().getId()).isEqualTo(ACCOUNT_ID);
    assertThat(payment.getAmount()).isEqualByComparingTo("150.00");
    assertThat(payment.getType()).isNull();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ACTIVE);
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.PAID);
    assertThat(installment.getStatus()).isEqualTo(ExpenseStatus.PAID);
    assertThat(response.status()).isEqualTo(ExpenseStatus.PAID);
    verify(accountService).requireActiveOwnedAccount(USER_A, ACCOUNT_ID);
  }

  @Test
  void shouldPayPartiallyThenAllowLaterPaymentUntilPaid() {
    Expense expense = openAccountExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    stubPay(expense, installment, BigDecimal.ZERO, "1500.00");

    ExpenseResponse partial =
        expenseService.pay(
            new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(ACCOUNT_ID, "50.00"));

    assertThat(partial.status()).isEqualTo(ExpenseStatus.PARTIALLY_PAID);
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.PARTIALLY_PAID);

    stubPay(expense, installment, new BigDecimal("50.00"), "1450.00");
    ExpenseResponse paid =
        expenseService.pay(
            new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(ACCOUNT_ID, "100.00"));

    assertThat(paid.status()).isEqualTo(ExpenseStatus.PAID);
    assertThat(installment.getStatus()).isEqualTo(ExpenseStatus.PAID);
  }

  @Test
  void shouldRejectPaymentExceedingRemainingAmount() {
    Expense expense = openAccountExpense();
    stubPayLookup(expense, singleInstallment(expense), new BigDecimal("100.00"));

    assertThatThrownBy(
            () ->
                expenseService.pay(
                    new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(ACCOUNT_ID, "50.01")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.PAYMENT_EXCEEDS_DUE);
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void shouldRejectPaymentWhenBalanceIsInsufficient() {
    Expense expense = openAccountExpense();
    stubPayLookup(expense, singleInstallment(expense), BigDecimal.ZERO);
    when(accountService.calculateCurrentBalance(any(Account.class)))
        .thenReturn(new BigDecimal("149.99"));

    assertThatThrownBy(
            () ->
                expenseService.pay(
                    new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(ACCOUNT_ID, "150.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.INSUFFICIENT_BALANCE);
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void shouldAllowAccountPaymentOnDifferentAccount() {
    Expense expense = openAccountExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    Account otherAccount = activeAccount();
    otherAccount.setId(OTHER_ACCOUNT_ID);
    when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);
    when(expenseInstallmentRepository.findByIdAndExpense_IdAndUserIdForUpdate(
            INSTALLMENT_ID, EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(installment));
    stubZeroAdjustments(INSTALLMENT_ID);
    when(paymentRepository.sumActiveAmountByInstallmentIdAndUserId(INSTALLMENT_ID, USER_A))
        .thenReturn(BigDecimal.ZERO)
        .thenReturn(new BigDecimal("150.00"));
    when(accountService.requireActiveOwnedAccount(USER_A, OTHER_ACCOUNT_ID))
        .thenReturn(otherAccount);
    when(accountService.calculateCurrentBalance(otherAccount))
        .thenReturn(new BigDecimal("1500.00"));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubSaves();

    ExpenseResponse response =
        expenseService.pay(
            new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(OTHER_ACCOUNT_ID, "150.00"));

    Payment payment = captureSavedPayment();
    assertThat(payment.getAccount().getId()).isEqualTo(OTHER_ACCOUNT_ID);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ACTIVE);
    assertThat(response.status()).isEqualTo(ExpenseStatus.PAID);
  }

  @Test
  void shouldPayNoneExpenseWithoutFillingExpenseAccount() {
    Expense expense = openNoneExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    stubPay(expense, installment, BigDecimal.ZERO, "1500.00");
    when(accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID)).thenReturn(activeAccount());

    ExpenseResponse response =
        expenseService.pay(
            new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(ACCOUNT_ID, "150.00"));

    Payment payment = captureSavedPayment();
    assertThat(payment.getAccount().getId()).isEqualTo(ACCOUNT_ID);
    assertThat(expense.getAccount()).isNull();
    assertThat(expense.getPaymentMethod()).isEqualTo(PaymentMethod.NONE);
    assertThat(response.accountId()).isNull();
    assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.NONE);
    assertThat(response.status()).isEqualTo(ExpenseStatus.PAID);
  }

  @Test
  void shouldRequireAccountWhenPayingNoneExpense() {
    Expense expense = openNoneExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);
    when(expenseInstallmentRepository.findByIdAndExpense_IdAndUserIdForUpdate(
            INSTALLMENT_ID, EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(installment));

    assertThatThrownBy(
            () ->
                expenseService.pay(
                    new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(null, "150.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(ExpenseService.ACCOUNT_REQUIRED_FOR_PAYMENT);
  }

  @Test
  void shouldRejectPaymentWhenStatusDoesNotAllowPay() {
    for (ExpenseStatus status :
        List.of(ExpenseStatus.PAID, ExpenseStatus.CANCELLED, ExpenseStatus.REFUNDED)) {
      Expense expense = openAccountExpense();
      expense.setStatus(status);
      when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
          .thenReturn(Optional.of(expense));

      assertThatThrownBy(
              () ->
                  expenseService.pay(
                      new AuthenticatedUser(USER_A), EXPENSE_ID, payRequest(ACCOUNT_ID, "150.00")))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage(ExpenseService.ONLY_OPEN_OR_PARTIAL_CAN_BE_PAID);
    }
  }

  @Test
  void shouldCancelOpenExpenseWithoutCreatingPayment() {
    Expense expense = openAccountExpense();
    ExpenseInstallment installment = singleInstallment(expense);
    when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);
    stubSaves();

    ExpenseResponse response = expenseService.cancel(new AuthenticatedUser(USER_A), EXPENSE_ID);

    assertThat(response.status()).isEqualTo(ExpenseStatus.CANCELLED);
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CANCELLED);
    assertThat(installment.getStatus()).isEqualTo(ExpenseStatus.CANCELLED);
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void shouldRejectCancelWhenNotOpen() {
    for (ExpenseStatus status :
        List.of(
            ExpenseStatus.PARTIALLY_PAID,
            ExpenseStatus.PAID,
            ExpenseStatus.CANCELLED,
            ExpenseStatus.REFUNDED)) {
      Expense expense = openAccountExpense();
      expense.setStatus(status);
      when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
          .thenReturn(Optional.of(expense));

      assertThatThrownBy(() -> expenseService.cancel(new AuthenticatedUser(USER_A), EXPENSE_ID))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage(ExpenseService.ONLY_OPEN_CAN_BE_CANCELLED);
    }
  }

  @Test
  void shouldRefundPaidAndPartiallyPaidWithoutDeletingPaymentsOrReturningToOpen() {
    for (ExpenseStatus status : List.of(ExpenseStatus.PARTIALLY_PAID, ExpenseStatus.PAID)) {
      Expense expense = openAccountExpense();
      expense.setStatus(status);
      ExpenseInstallment installment = singleInstallment(expense);
      installment.setStatus(status);
      when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
          .thenReturn(Optional.of(expense));
      stubInstallments(expense, installment);
      when(paymentRepository.sumActiveAmountByInstallmentIdAndUserId(INSTALLMENT_ID, USER_A))
          .thenReturn(new BigDecimal("50.00"));
      stubSaves();

      ExpenseResponse response = expenseService.refund(new AuthenticatedUser(USER_A), EXPENSE_ID);

      assertThat(response.status()).isEqualTo(ExpenseStatus.REFUNDED);
      assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.REFUNDED);
      assertThat(installment.getStatus()).isEqualTo(ExpenseStatus.REFUNDED);
      verify(paymentRepository, never()).delete(any());
      verify(paymentRepository, never()).deleteById(any());
    }
  }

  @Test
  void shouldRejectRefundWhenStatusDoesNotAllowIt() {
    for (ExpenseStatus status :
        List.of(ExpenseStatus.OPEN, ExpenseStatus.CANCELLED, ExpenseStatus.REFUNDED)) {
      Expense expense = openAccountExpense();
      expense.setStatus(status);
      when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
          .thenReturn(Optional.of(expense));

      assertThatThrownBy(() -> expenseService.refund(new AuthenticatedUser(USER_A), EXPENSE_ID))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage(ExpenseService.ONLY_PAID_OR_PARTIAL_CAN_BE_REFUNDED);
    }
  }

  @Test
  void shouldKeepPaymentHistoryAfterRefund() {
    Expense expense = openAccountExpense();
    expense.setStatus(ExpenseStatus.REFUNDED);
    Payment payment = ownedPayment(expense);
    when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_A)).thenReturn(Optional.of(expense));
    when(paymentRepository.findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(EXPENSE_ID, USER_A))
        .thenReturn(List.of(payment));

    List<PaymentResponse> payments =
        expenseService.listPayments(new AuthenticatedUser(USER_A), EXPENSE_ID);

    assertThat(payments).hasSize(1);
    assertThat(payments.getFirst().id()).isEqualTo(PAYMENT_ID);
    assertThat(payments.getFirst().amount()).isEqualByComparingTo("150.00");
  }

  @Test
  void shouldGetOwnedPaymentWithoutExposingType() {
    when(paymentRepository.findByIdAndUserId(PAYMENT_ID, USER_A))
        .thenReturn(Optional.of(ownedPayment(openAccountExpense())));

    PaymentResponse response = expenseService.getPayment(new AuthenticatedUser(USER_A), PAYMENT_ID);

    assertThat(response.id()).isEqualTo(PAYMENT_ID);
    assertThat(response.expenseId()).isEqualTo(EXPENSE_ID);
    assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  void shouldRejectAccessToExpenseOfAnotherUser() {
    when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_B)).thenReturn(Optional.empty());
    when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_B))
        .thenReturn(Optional.empty());
    when(paymentRepository.findByIdAndUserId(PAYMENT_ID, USER_B)).thenReturn(Optional.empty());

    AuthenticatedUser userB = new AuthenticatedUser(USER_B);
    assertThatThrownBy(() -> expenseService.get(userB, EXPENSE_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(ExpenseService.EXPENSE_NOT_FOUND);
    assertThatThrownBy(() -> expenseService.cancel(userB, EXPENSE_ID))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> expenseService.refund(userB, EXPENSE_ID))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> expenseService.pay(userB, EXPENSE_ID, payRequest(ACCOUNT_ID, "10.00")))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> expenseService.getPayment(userB, PAYMENT_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(ExpenseService.PAYMENT_NOT_FOUND);
  }

  @Test
  void shouldMarkOpenAndPartiallyPaidAsOverdueAfterDueDate() {
    Expense open = openAccountExpense();
    open.setDueDate(PAST_DUE);
    ExpenseInstallment installment = singleInstallment(open);
    when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_A)).thenReturn(Optional.of(open));
    stubInstallments(open, installment);

    assertThat(expenseService.get(new AuthenticatedUser(USER_A), EXPENSE_ID).overdue()).isTrue();

    open.setStatus(ExpenseStatus.PARTIALLY_PAID);
    installment.setStatus(ExpenseStatus.PARTIALLY_PAID);
    assertThat(expenseService.get(new AuthenticatedUser(USER_A), EXPENSE_ID).overdue()).isTrue();
  }

  @Test
  void shouldNotMarkPaidCancelledOrRefundedAsOverdue() {
    Expense expense = openAccountExpense();
    expense.setDueDate(PAST_DUE);
    ExpenseInstallment installment = singleInstallment(expense);
    when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_A)).thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);

    for (ExpenseStatus status :
        List.of(ExpenseStatus.PAID, ExpenseStatus.CANCELLED, ExpenseStatus.REFUNDED)) {
      expense.setStatus(status);
      installment.setStatus(status);
      assertThat(expenseService.get(new AuthenticatedUser(USER_A), EXPENSE_ID).overdue()).isFalse();
    }
  }

  @Test
  void shouldNotMarkOpenExpenseAsOverdueOnDueDate() {
    Expense expense = openAccountExpense();
    expense.setDueDate(LocalDate.of(2026, 8, 14));
    ExpenseInstallment installment = singleInstallment(expense);
    when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_A)).thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);

    assertThat(expenseService.get(new AuthenticatedUser(USER_A), EXPENSE_ID).overdue()).isFalse();
  }

  @Test
  void shouldComputeOverdueUsingAmericaSaoPauloNotUtcDate() {
    ExpenseService zoned =
        serviceWith(Clock.fixed(Instant.parse("2026-08-14T02:00:00Z"), ZoneOffset.UTC));
    Expense expense = openAccountExpense();
    expense.setDueDate(LocalDate.of(2026, 8, 13));
    ExpenseInstallment installment = singleInstallment(expense);
    when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_A)).thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);

    assertThat(zoned.get(new AuthenticatedUser(USER_A), EXPENSE_ID).overdue()).isFalse();

    expense.setDueDate(LocalDate.of(2026, 8, 12));
    installment.setDueDate(LocalDate.of(2026, 8, 12));
    assertThat(zoned.get(new AuthenticatedUser(USER_A), EXPENSE_ID).overdue()).isTrue();
  }

  private ExpenseService serviceWith(Clock clock) {
    return new ExpenseService(
        expenseRepository,
        expenseInstallmentRepository,
        adjustmentRepository,
        paymentRepository,
        accountService,
        categoryService,
        creditCardService,
        creditCardInvoiceService,
        installmentBalanceService,
        invoicePaymentAllocationRepository,
        creditApplicationRepository,
        cardPurchaseAccountRefundRepository,
        clock);
  }

  private void stubCreateDependencies(Account account) {
    when(categoryService.requireActiveOwnedExpenseCategory(USER_A, CATEGORY_ID))
        .thenReturn(expenseCategory());
    when(accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID)).thenReturn(account);
    stubSaves();
  }

  private void stubSaves() {
    lenient()
        .when(expenseRepository.save(any(Expense.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(expenseInstallmentRepository.save(any(ExpenseInstallment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(expenseInstallmentRepository.saveAll(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private void stubInstallments(Expense expense, ExpenseInstallment installment) {
    when(expenseInstallmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.getId(), USER_A))
        .thenReturn(List.of(installment));
  }

  private void stubZeroAdjustments(UUID installmentId) {
    lenient()
        .when(
            adjustmentRepository.sumActiveDiscountAmountByInstallmentIdAndUserId(
                installmentId, USER_A))
        .thenReturn(BigDecimal.ZERO);
    lenient()
        .when(
            adjustmentRepository.sumActiveSurchargeAmountByInstallmentIdAndUserId(
                installmentId, USER_A))
        .thenReturn(BigDecimal.ZERO);
  }

  private void stubPayLookup(
      Expense expense, ExpenseInstallment installment, BigDecimal alreadyPaid) {
    when(expenseRepository.findByIdAndUserIdForUpdate(EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(expense));
    stubInstallments(expense, installment);
    when(expenseInstallmentRepository.findByIdAndExpense_IdAndUserIdForUpdate(
            INSTALLMENT_ID, EXPENSE_ID, USER_A))
        .thenReturn(Optional.of(installment));
    stubZeroAdjustments(INSTALLMENT_ID);
    when(paymentRepository.sumActiveAmountByInstallmentIdAndUserId(INSTALLMENT_ID, USER_A))
        .thenReturn(alreadyPaid);
    if (expense.getPaymentMethod() == PaymentMethod.ACCOUNT) {
      when(accountService.requireActiveOwnedAccount(USER_A, ACCOUNT_ID))
          .thenReturn(expense.getAccount());
    }
  }

  private void stubPay(
      Expense expense, ExpenseInstallment installment, BigDecimal alreadyPaid, String balance) {
    stubPayLookup(expense, installment, alreadyPaid);
    when(accountService.calculateCurrentBalance(any(Account.class)))
        .thenReturn(new BigDecimal(balance));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(
            invocation -> {
              Payment payment = invocation.getArgument(0);
              when(paymentRepository.sumActiveAmountByInstallmentIdAndUserId(
                      INSTALLMENT_ID, USER_A))
                  .thenReturn(alreadyPaid.add(payment.getAmount()));
              return payment;
            });
    stubSaves();
  }

  private Expense captureSavedExpense() {
    ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
    verify(expenseRepository).save(captor.capture());
    return captor.getValue();
  }

  private ExpenseInstallment captureSavedInstallment() {
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(expenseInstallmentRepository).saveAll(captor.capture());
    @SuppressWarnings("unchecked")
    List<ExpenseInstallment> saved = captor.getValue();
    return saved.getFirst();
  }

  private Payment captureSavedPayment() {
    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(captor.capture());
    return captor.getValue();
  }

  private static CreateExpenseRequest accountCreateRequest() {
    return new CreateExpenseRequest(
        CATEGORY_ID,
        "Luz",
        new BigDecimal("150.00"),
        EXPENSE_DATE,
        DUE_DATE,
        PaymentMethod.ACCOUNT,
        ACCOUNT_ID,
        ResponsibleType.MINE,
        "texto ignorado",
        "23793381286000000000000000000000000000000000",
        null,
        null,
        null);
  }

  private static CreateExpenseRequest noneCreateRequest() {
    return new CreateExpenseRequest(
        CATEGORY_ID,
        "Luz",
        new BigDecimal("150.00"),
        EXPENSE_DATE,
        DUE_DATE,
        PaymentMethod.NONE,
        null,
        ResponsibleType.MINE,
        null,
        null,
        null,
        null,
        null);
  }

  private static UpdateExpenseRequest accountUpdateRequest() {
    return new UpdateExpenseRequest(
        CATEGORY_ID,
        "Luz",
        new BigDecimal("150.00"),
        EXPENSE_DATE,
        DUE_DATE,
        PaymentMethod.ACCOUNT,
        ACCOUNT_ID,
        ResponsibleType.MINE,
        null,
        null,
        null,
        null);
  }

  private static PayExpenseRequest payRequest(UUID accountId, String amount) {
    return new PayExpenseRequest(accountId, new BigDecimal(amount), PAYMENT_DATE, null);
  }

  private static Category expenseCategory() {
    Category category = new Category();
    category.setId(CATEGORY_ID);
    category.setUserId(USER_A);
    category.setName("Moradia");
    category.setType(CategoryType.EXPENSE);
    category.setActive(true);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);
    return category;
  }

  private static Account activeAccount() {
    Account account = new Account();
    account.setId(ACCOUNT_ID);
    account.setUserId(USER_A);
    account.setName("Nubank");
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal("1500.00"));
    account.setActive(true);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);
    return account;
  }

  private static Expense openAccountExpense() {
    Expense expense = new Expense();
    expense.setId(EXPENSE_ID);
    expense.setUserId(USER_A);
    expense.setCategory(expenseCategory());
    expense.setAccount(activeAccount());
    expense.setDescription("Luz");
    expense.setTotalAmount(new BigDecimal("150.00"));
    expense.setExpenseDate(EXPENSE_DATE);
    expense.setDueDate(DUE_DATE);
    expense.setPaymentMethod(PaymentMethod.ACCOUNT);
    expense.setStatus(ExpenseStatus.OPEN);
    expense.setResponsibleType(ResponsibleType.MINE);
    expense.setCreatedAt(NOW);
    expense.setUpdatedAt(NOW);
    return expense;
  }

  private static Expense openNoneExpense() {
    Expense expense = openAccountExpense();
    expense.setAccount(null);
    expense.setPaymentMethod(PaymentMethod.NONE);
    return expense;
  }

  private static ExpenseInstallment singleInstallment(Expense expense) {
    ExpenseInstallment installment = new ExpenseInstallment();
    installment.setId(INSTALLMENT_ID);
    installment.setUserId(USER_A);
    installment.setExpense(expense);
    installment.setInstallmentNumber(1);
    installment.setTotalInstallments(1);
    installment.setAmount(expense.getTotalAmount());
    installment.setDueDate(expense.getDueDate());
    installment.setStatus(expense.getStatus());
    installment.setCreatedAt(NOW);
    installment.setUpdatedAt(NOW);
    return installment;
  }

  private static Payment ownedPayment(Expense expense) {
    Payment payment = new Payment();
    payment.setId(PAYMENT_ID);
    payment.setUserId(USER_A);
    payment.setExpense(expense);
    payment.setInstallment(singleInstallment(expense));
    payment.setAccount(activeAccount());
    payment.setAmount(new BigDecimal("150.00"));
    payment.setPaymentDate(PAYMENT_DATE);
    payment.setType(null);
    payment.setCreatedAt(NOW);
    return payment;
  }
}
