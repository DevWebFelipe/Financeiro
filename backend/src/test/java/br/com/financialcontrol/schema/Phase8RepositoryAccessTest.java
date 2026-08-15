package br.com.financialcontrol.schema;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountRepository;
import br.com.financialcontrol.accounts.AccountType;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryRepository;
import br.com.financialcontrol.categories.CategoryType;
import br.com.financialcontrol.expenses.AdjustmentStatus;
import br.com.financialcontrol.expenses.AdjustmentType;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentAdjustment;
import br.com.financialcontrol.expenses.ExpenseInstallmentAdjustmentRepository;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseRepository;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.PaymentStatus;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class Phase8RepositoryAccessTest {

  private static final Instant NOW = Instant.parse("2026-08-15T15:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private ExpenseInstallmentRepository installmentRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private ExpenseInstallmentAdjustmentRepository adjustmentRepository;

  @Test
  void shouldListInstallmentsOrderedByNumberForOwnerOnly() {
    User owner = persistUser("repo-inst-owner@example.com");
    User other = persistUser("repo-inst-other@example.com");
    Category category = persistCategory(owner.getId(), "Parcelas", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, "300.00");
    ExpenseInstallment first = persistInstallment(expense, 1, 3, "100.00");
    ExpenseInstallment second = persistInstallment(expense, 2, 3, "100.00");
    ExpenseInstallment third = persistInstallment(expense, 3, 3, "100.00");

    List<ExpenseInstallment> listed =
        installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.getId(), owner.getId());

    assertThat(listed)
        .extracting(ExpenseInstallment::getId)
        .containsExactly(first.getId(), second.getId(), third.getId());
    assertThat(
            installmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
                expense.getId(), other.getId()))
        .isEmpty();
  }

  @Test
  void shouldFindInstallmentByIdExpenseAndUserWithOwnership() {
    User owner = persistUser("repo-inst-get@example.com");
    User other = persistUser("repo-inst-get-other@example.com");
    Category category = persistCategory(owner.getId(), "Get", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, "50.00");
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, "50.00");

    assertThat(
            installmentRepository.findByIdAndExpense_IdAndUserId(
                installment.getId(), expense.getId(), owner.getId()))
        .isPresent();
    assertThat(
            installmentRepository.findByIdAndExpense_IdAndUserId(
                installment.getId(), expense.getId(), other.getId()))
        .isEmpty();
    assertThat(
            installmentRepository.findByIdAndExpense_IdAndUserId(
                installment.getId(), UuidV7.create(), owner.getId()))
        .isEmpty();
    assertThat(
            installmentRepository.findByExpense_IdAndUserIdAndInstallmentNumber(
                expense.getId(), owner.getId(), 1))
        .isPresent()
        .get()
        .extracting(ExpenseInstallment::getId)
        .isEqualTo(installment.getId());
    assertThat(
            installmentRepository.findByExpense_IdAndUserIdAndInstallmentNumber(
                expense.getId(), other.getId(), 1))
        .isEmpty();
  }

  @Test
  void shouldLockInstallmentAndExpenseForUpdateWithinTransaction() {
    User owner = persistUser("repo-lock@example.com");
    Category category = persistCategory(owner.getId(), "Lock", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, "80.00");
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, "80.00");

    assertThat(expenseRepository.findByIdAndUserIdForUpdate(expense.getId(), owner.getId()))
        .isPresent();
    assertThat(
            installmentRepository.findByIdAndExpense_IdAndUserIdForUpdate(
                installment.getId(), expense.getId(), owner.getId()))
        .isPresent();
    assertThat(
            installmentRepository.findSingleByExpenseIdAndUserIdForUpdate(
                expense.getId(), owner.getId()))
        .isPresent();
    assertThat(
            installmentRepository.findByIdAndExpense_IdAndUserIdForUpdate(
                installment.getId(), expense.getId(), UuidV7.create()))
        .isEmpty();
  }

  @Test
  void shouldListAndSumPaymentsByStatusWithoutMixingInstallmentsOrUsers() {
    User owner = persistUser("repo-pay-owner@example.com");
    User other = persistUser("repo-pay-other@example.com");
    Account account = persistAccount(owner.getId(), "Conta");
    Category category = persistCategory(owner.getId(), "Pay", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, account, "200.00");
    ExpenseInstallment first = persistInstallment(expense, 1, 2, "100.00");
    ExpenseInstallment second = persistInstallment(expense, 2, 2, "100.00");

    Payment activeFirst =
        persistPayment(owner, expense, first, account, "40.00", PaymentStatus.ACTIVE, NOW);
    Payment reversedFirst =
        persistPayment(
            owner, expense, first, account, "10.00", PaymentStatus.REVERSED, NOW.plusSeconds(1));
    Payment activeSecond =
        persistPayment(
            owner, expense, second, account, "70.00", PaymentStatus.ACTIVE, NOW.plusSeconds(2));

    assertThat(
            paymentRepository.findAllByInstallment_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
                first.getId(), owner.getId(), PaymentStatus.ACTIVE))
        .extracting(Payment::getId)
        .containsExactly(activeFirst.getId());
    assertThat(
            paymentRepository.findAllByInstallment_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
                first.getId(), owner.getId(), PaymentStatus.REVERSED))
        .extracting(Payment::getId)
        .containsExactly(reversedFirst.getId());
    assertThat(
            paymentRepository.findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
                first.getId(), owner.getId()))
        .extracting(Payment::getId)
        .containsExactly(activeFirst.getId(), reversedFirst.getId());
    assertThat(
            paymentRepository.findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
                first.getId(), other.getId()))
        .isEmpty();
    assertThat(
            paymentRepository.findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
                second.getId(), owner.getId()))
        .extracting(Payment::getId)
        .containsExactly(activeSecond.getId());

    assertThat(
            paymentRepository.sumActiveAmountByInstallmentIdAndUserId(first.getId(), owner.getId()))
        .isEqualByComparingTo("40.00");
    assertThat(paymentRepository.sumAmountByInstallmentIdAndUserId(first.getId(), owner.getId()))
        .isEqualByComparingTo("50.00");
    assertThat(
            paymentRepository.sumActiveAmountByInstallmentIdAndUserId(first.getId(), other.getId()))
        .isEqualByComparingTo("0");
  }

  @Test
  void shouldExposeActiveAccountPaymentAggregationWithoutChangingPhase7Query() {
    User owner = persistUser("repo-pay-balance@example.com");
    Account account = persistAccount(owner.getId(), "Saldo");
    Category category = persistCategory(owner.getId(), "Balance", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, account, "100.00");
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, "100.00");

    persistPayment(owner, expense, installment, account, "30.00", PaymentStatus.ACTIVE, NOW);
    persistPayment(
        owner, expense, installment, account, "20.00", PaymentStatus.REVERSED, NOW.plusSeconds(1));

    assertThat(
            paymentRepository.sumValidExpensePaymentsByAccountIdAndUserId(
                account.getId(), owner.getId()))
        .isEqualByComparingTo("50.00");
    assertThat(
            paymentRepository.sumActiveValidExpensePaymentsByAccountIdAndUserId(
                account.getId(), owner.getId()))
        .isEqualByComparingTo("30.00");
  }

  @Test
  void shouldListAndSumAdjustmentsByStatusTypeWithoutMixingInstallmentsOrUsers() {
    User owner = persistUser("repo-adj-owner@example.com");
    User other = persistUser("repo-adj-other@example.com");
    Category category = persistCategory(owner.getId(), "Adj", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, "500.00");
    ExpenseInstallment first = persistInstallment(expense, 1, 2, "250.00");
    ExpenseInstallment second = persistInstallment(expense, 2, 2, "250.00");

    ExpenseInstallmentAdjustment activeDiscount =
        persistAdjustment(
            owner, first, AdjustmentType.DISCOUNT, AdjustmentStatus.ACTIVE, "40.00", NOW);
    ExpenseInstallmentAdjustment reversedDiscount =
        persistAdjustment(
            owner,
            first,
            AdjustmentType.DISCOUNT,
            AdjustmentStatus.REVERSED,
            "5.00",
            NOW.plusSeconds(1));
    ExpenseInstallmentAdjustment activeSurcharge =
        persistAdjustment(
            owner,
            first,
            AdjustmentType.SURCHARGE,
            AdjustmentStatus.ACTIVE,
            "15.00",
            NOW.plusSeconds(2));
    ExpenseInstallmentAdjustment otherInstallmentDiscount =
        persistAdjustment(
            owner,
            second,
            AdjustmentType.DISCOUNT,
            AdjustmentStatus.ACTIVE,
            "8.00",
            NOW.plusSeconds(3));

    assertThat(
            adjustmentRepository.findAllByInstallment_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
                first.getId(), owner.getId(), AdjustmentStatus.ACTIVE))
        .extracting(ExpenseInstallmentAdjustment::getId)
        .containsExactly(activeDiscount.getId(), activeSurcharge.getId());
    assertThat(
            adjustmentRepository.findAllByInstallment_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
                first.getId(), owner.getId(), AdjustmentStatus.REVERSED))
        .extracting(ExpenseInstallmentAdjustment::getId)
        .containsExactly(reversedDiscount.getId());
    assertThat(
            adjustmentRepository.findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
                first.getId(), owner.getId()))
        .extracting(ExpenseInstallmentAdjustment::getId)
        .containsExactly(activeDiscount.getId(), reversedDiscount.getId(), activeSurcharge.getId());
    assertThat(
            adjustmentRepository.findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
                first.getId(), other.getId()))
        .isEmpty();
    assertThat(
            adjustmentRepository.findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
                second.getId(), owner.getId()))
        .extracting(ExpenseInstallmentAdjustment::getId)
        .containsExactly(otherInstallmentDiscount.getId());

    assertThat(
            adjustmentRepository.sumActiveDiscountAmountByInstallmentIdAndUserId(
                first.getId(), owner.getId()))
        .isEqualByComparingTo("40.00");
    assertThat(
            adjustmentRepository.sumActiveSurchargeAmountByInstallmentIdAndUserId(
                first.getId(), owner.getId()))
        .isEqualByComparingTo("15.00");
    assertThat(
            adjustmentRepository.sumActiveDiscountAmountByInstallmentIdAndUserId(
                second.getId(), owner.getId()))
        .isEqualByComparingTo("8.00");
    assertThat(
            adjustmentRepository.sumActiveDiscountAmountByInstallmentIdAndUserId(
                first.getId(), other.getId()))
        .isEqualByComparingTo("0");
  }

  @Test
  void shouldLockAdjustmentWithOwnership() {
    User owner = persistUser("repo-adj-lock@example.com");
    User other = persistUser("repo-adj-lock-other@example.com");
    Category category = persistCategory(owner.getId(), "AdjLock", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, "90.00");
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, "90.00");
    ExpenseInstallmentAdjustment adjustment =
        persistAdjustment(
            owner, installment, AdjustmentType.DISCOUNT, AdjustmentStatus.ACTIVE, "9.00", NOW);

    assertThat(adjustmentRepository.findByIdAndUserIdForUpdate(adjustment.getId(), owner.getId()))
        .isPresent();
    assertThat(
            adjustmentRepository.findByIdAndInstallment_IdAndUserIdForUpdate(
                adjustment.getId(), installment.getId(), owner.getId()))
        .isPresent();
    assertThat(adjustmentRepository.findByIdAndUserIdForUpdate(adjustment.getId(), other.getId()))
        .isEmpty();
    assertThat(
            adjustmentRepository.findByIdAndInstallment_IdAndUserId(
                adjustment.getId(), installment.getId(), owner.getId()))
        .isPresent();
  }

  @Test
  void shouldKeepExpenseOwnershipQueries() {
    User owner = persistUser("repo-exp-owner@example.com");
    User other = persistUser("repo-exp-other@example.com");
    Category category = persistCategory(owner.getId(), "Exp", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, "10.00");

    assertThat(expenseRepository.findByIdAndUserId(expense.getId(), owner.getId())).isPresent();
    assertThat(expenseRepository.findByIdAndUserId(expense.getId(), other.getId())).isEmpty();
    assertThat(expenseRepository.findByIdAndUserIdForUpdate(expense.getId(), owner.getId()))
        .isPresent();
  }

  private User persistUser(String email) {
    User user = new User();
    user.setId(UuidV7.create());
    user.setName("User");
    user.setEmail(email);
    user.setPasswordHash("not-a-real-hash");
    user.setActive(true);
    user.setCreatedAt(NOW);
    user.setUpdatedAt(NOW);
    return userRepository.saveAndFlush(user);
  }

  private Account persistAccount(UUID userId, String name) {
    Account account = new Account();
    account.setId(UuidV7.create());
    account.setUserId(userId);
    account.setName(name);
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal("1000.00"));
    account.setActive(true);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);
    return accountRepository.saveAndFlush(account);
  }

  private Category persistCategory(UUID userId, String name, CategoryType type) {
    Category category = new Category();
    category.setId(UuidV7.create());
    category.setUserId(userId);
    category.setName(name);
    category.setType(type);
    category.setActive(true);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);
    return categoryRepository.saveAndFlush(category);
  }

  private Expense persistExpense(
      User owner, Category category, Account account, String totalAmount) {
    Expense expense = new Expense();
    expense.setId(UuidV7.create());
    expense.setUserId(owner.getId());
    expense.setCategory(category);
    expense.setAccount(account);
    expense.setDescription("Despesa");
    expense.setTotalAmount(new BigDecimal(totalAmount));
    expense.setExpenseDate(TODAY);
    expense.setDueDate(TODAY);
    expense.setPaymentMethod(account == null ? PaymentMethod.NONE : PaymentMethod.ACCOUNT);
    expense.setStatus(ExpenseStatus.OPEN);
    expense.setResponsibleType(ResponsibleType.MINE);
    expense.setCreatedAt(NOW);
    expense.setUpdatedAt(NOW);
    return expenseRepository.saveAndFlush(expense);
  }

  private ExpenseInstallment persistInstallment(
      Expense expense, int number, int total, String amount) {
    ExpenseInstallment installment = new ExpenseInstallment();
    installment.setId(UuidV7.create());
    installment.setUserId(expense.getUserId());
    installment.setExpense(expense);
    installment.setInstallmentNumber(number);
    installment.setTotalInstallments(total);
    installment.setAmount(new BigDecimal(amount));
    installment.setDueDate(TODAY);
    installment.setStatus(ExpenseStatus.OPEN);
    installment.setCreatedAt(NOW);
    installment.setUpdatedAt(NOW);
    return installmentRepository.saveAndFlush(installment);
  }

  private Payment persistPayment(
      User owner,
      Expense expense,
      ExpenseInstallment installment,
      Account account,
      String amount,
      PaymentStatus status,
      Instant createdAt) {
    Payment payment = new Payment();
    payment.setId(UuidV7.create());
    payment.setUserId(owner.getId());
    payment.setExpense(expense);
    payment.setInstallment(installment);
    payment.setAccount(account);
    payment.setAmount(new BigDecimal(amount));
    payment.setPaymentDate(TODAY);
    payment.setStatus(status);
    payment.setType(null);
    payment.setCreatedAt(createdAt);
    return paymentRepository.saveAndFlush(payment);
  }

  private ExpenseInstallmentAdjustment persistAdjustment(
      User owner,
      ExpenseInstallment installment,
      AdjustmentType type,
      AdjustmentStatus status,
      String amount,
      Instant createdAt) {
    ExpenseInstallmentAdjustment adjustment = new ExpenseInstallmentAdjustment();
    adjustment.setId(UuidV7.create());
    adjustment.setUserId(owner.getId());
    adjustment.setInstallment(installment);
    adjustment.setType(type);
    adjustment.setAmount(new BigDecimal(amount));
    adjustment.setStatus(status);
    adjustment.setCreatedAt(createdAt);
    return adjustmentRepository.saveAndFlush(adjustment);
  }
}
