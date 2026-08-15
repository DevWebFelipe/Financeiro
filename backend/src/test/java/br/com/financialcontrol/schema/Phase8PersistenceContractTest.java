package br.com.financialcontrol.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class Phase8PersistenceContractTest {

  private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private ExpenseInstallmentRepository expenseInstallmentRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private ExpenseInstallmentAdjustmentRepository adjustmentRepository;

  @Test
  void shouldExposePaymentsStatusWithOfficialValuesOnly() {
    assertThat(columnsOf("payments")).contains("status");
    assertThat(constraintDefinition("payments", "ck_payments_status"))
        .contains("ACTIVE")
        .contains("REVERSED");
  }

  @Test
  @Transactional
  void shouldDefaultNewPaymentToActiveWithoutTouchingType() {
    User owner = persistUser("phase8-payment@example.com");
    Account account = persistAccount(owner.getId(), "Conta");
    Category category = persistCategory(owner.getId(), "Casa", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, account, new BigDecimal("100.00"));
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, new BigDecimal("100.00"));

    Payment payment = new Payment();
    payment.setId(UuidV7.create());
    payment.setUserId(owner.getId());
    payment.setExpense(expense);
    payment.setInstallment(installment);
    payment.setAccount(account);
    payment.setAmount(new BigDecimal("100.00"));
    payment.setPaymentDate(TODAY);
    payment.setType(null);
    payment.setCreatedAt(NOW);
    paymentRepository.saveAndFlush(payment);

    Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.ACTIVE);
    assertThat(reloaded.getType()).isNull();

    String typeNullable =
        jdbcTemplate.queryForObject(
            """
            SELECT is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'payments'
              AND column_name = 'type'
            """,
            String.class);
    assertThat(typeNullable).isEqualTo("YES");

    Integer typeCheckCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_constraint
            WHERE conrelid = 'payments'::regclass
              AND contype = 'c'
              AND conname <> 'ck_payments_status'
              AND pg_get_constraintdef(oid) ILIKE '%(type%'
            """,
            Integer.class);
    assertThat(typeCheckCount).isZero();
  }

  @Test
  void shouldCreateExpenseInstallmentAdjustmentsTableWithOwnershipAndChecks() {
    assertThat(columnsOf("expense_installment_adjustments"))
        .contains("id", "user_id", "installment_id", "type", "amount", "status", "created_at")
        .doesNotContain(
            "paid_amount",
            "remaining_amount",
            "discount_total",
            "surcharge_total",
            "early_payment_savings",
            "current_balance");

    assertThat(
            constraintDefinition(
                "expense_installment_adjustments", "ck_expense_installment_adjustments_type"))
        .contains("DISCOUNT")
        .contains("SURCHARGE");
    assertThat(
            constraintDefinition(
                "expense_installment_adjustments", "ck_expense_installment_adjustments_status"))
        .contains("ACTIVE")
        .contains("REVERSED");
    assertThat(constraintNames("expense_installment_adjustments"))
        .contains("fk_expense_installment_adjustments_installment_ownership");
  }

  @Test
  @Transactional
  void shouldPersistAdjustmentForOwnedInstallment() {
    User owner = persistUser("phase8-adj@example.com");
    Category category = persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, new BigDecimal("300.00"));
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, new BigDecimal("300.00"));

    ExpenseInstallmentAdjustment adjustment = new ExpenseInstallmentAdjustment();
    adjustment.setId(UuidV7.create());
    adjustment.setUserId(owner.getId());
    adjustment.setInstallment(installment);
    adjustment.setType(AdjustmentType.DISCOUNT);
    adjustment.setAmount(new BigDecimal("40.00"));
    adjustment.setStatus(AdjustmentStatus.ACTIVE);
    adjustment.setCreatedAt(NOW);
    adjustmentRepository.saveAndFlush(adjustment);

    ExpenseInstallmentAdjustment reloaded =
        adjustmentRepository.findById(adjustment.getId()).orElseThrow();
    assertThat(reloaded.getType()).isEqualTo(AdjustmentType.DISCOUNT);
    assertThat(reloaded.getStatus()).isEqualTo(AdjustmentStatus.ACTIVE);
    assertThat(reloaded.getAmount()).isEqualByComparingTo("40.00");
    assertThat(reloaded.getUserId()).isEqualTo(owner.getId());
    assertThat(reloaded.getInstallment().getId()).isEqualTo(installment.getId());
  }

  @Test
  @Transactional
  void shouldRejectInvalidAdjustmentType() {
    Fixture fixture = persistFixture("phase8-type@example.com");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO expense_installment_adjustments
                      (id, user_id, installment_id, type, amount, status, created_at)
                    VALUES (?, ?, ?, 'INTEREST', 10.00, 'ACTIVE', ?)
                    """,
                    UuidV7.create(),
                    fixture.userId(),
                    fixture.installmentId(),
                    Timestamp.from(NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void shouldRejectInvalidAdjustmentStatus() {
    Fixture fixture = persistFixture("phase8-status@example.com");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO expense_installment_adjustments
                      (id, user_id, installment_id, type, amount, status, created_at)
                    VALUES (?, ?, ?, 'DISCOUNT', 10.00, 'PENDING', ?)
                    """,
                    UuidV7.create(),
                    fixture.userId(),
                    fixture.installmentId(),
                    Timestamp.from(NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void shouldRejectZeroAdjustmentAmount() {
    Fixture fixture = persistFixture("phase8-zero@example.com");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO expense_installment_adjustments
                      (id, user_id, installment_id, type, amount, status, created_at)
                    VALUES (?, ?, ?, 'DISCOUNT', 0.00, 'ACTIVE', ?)
                    """,
                    UuidV7.create(),
                    fixture.userId(),
                    fixture.installmentId(),
                    Timestamp.from(NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void shouldRejectNegativeAdjustmentAmount() {
    Fixture fixture = persistFixture("phase8-neg@example.com");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO expense_installment_adjustments
                      (id, user_id, installment_id, type, amount, status, created_at)
                    VALUES (?, ?, ?, 'SURCHARGE', -1.00, 'ACTIVE', ?)
                    """,
                    UuidV7.create(),
                    fixture.userId(),
                    fixture.installmentId(),
                    Timestamp.from(NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void shouldRejectAdjustmentWithCrossUserOwnership() {
    User owner = persistUser("phase8-owner@example.com");
    User other = persistUser("phase8-other@example.com");
    Category category = persistCategory(owner.getId(), "Cruzado", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, new BigDecimal("80.00"));
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, new BigDecimal("80.00"));

    ExpenseInstallmentAdjustment crossed = new ExpenseInstallmentAdjustment();
    crossed.setId(UuidV7.create());
    crossed.setUserId(other.getId());
    crossed.setInstallment(installment);
    crossed.setType(AdjustmentType.SURCHARGE);
    crossed.setAmount(new BigDecimal("5.00"));
    crossed.setStatus(AdjustmentStatus.ACTIVE);
    crossed.setCreatedAt(NOW);

    assertThatThrownBy(() -> adjustmentRepository.saveAndFlush(crossed))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void shouldRejectAdjustmentReferencingMissingInstallment() {
    User owner = persistUser("phase8-missing@example.com");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO expense_installment_adjustments
                      (id, user_id, installment_id, type, amount, status, created_at)
                    VALUES (?, ?, ?, 'DISCOUNT', 1.00, 'ACTIVE', ?)
                    """,
                    UuidV7.create(),
                    owner.getId(),
                    UuidV7.create(),
                    Timestamp.from(NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void shouldEnforceUniqueExpenseIdAndInstallmentNumber() {
    User owner = persistUser("phase8-unique@example.com");
    Category category = persistCategory(owner.getId(), "Parcelas", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, new BigDecimal("200.00"));
    persistInstallment(expense, 1, 2, new BigDecimal("100.00"));

    assertThat(constraintNames("expense_installments"))
        .contains("uq_expense_installments_expense_number");

    assertThatThrownBy(() -> persistInstallment(expense, 1, 2, new BigDecimal("100.00")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldNotCreateDerivedColumnsOnExpenseInstallmentOrPayment() {
    assertThat(columnsOf("expenses"))
        .doesNotContain(
            "paid_amount",
            "remaining_amount",
            "discount_total",
            "surcharge_total",
            "early_payment_savings",
            "current_balance");
    assertThat(columnsOf("expense_installments"))
        .doesNotContain(
            "paid_amount",
            "remaining_amount",
            "discount_total",
            "surcharge_total",
            "early_payment_savings",
            "current_balance");
    assertThat(columnsOf("payments"))
        .doesNotContain(
            "paid_amount",
            "remaining_amount",
            "discount_total",
            "surcharge_total",
            "early_payment_savings",
            "current_balance");
  }

  @Test
  @Transactional
  void shouldApplyDatabaseDefaultActiveWhenStatusOmittedOnInsert() {
    User owner = persistUser("phase8-default@example.com");
    Account account = persistAccount(owner.getId(), "Default");
    Category category = persistCategory(owner.getId(), "DefaultCat", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, account, new BigDecimal("15.00"));
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, new BigDecimal("15.00"));
    UUID paymentId = UuidV7.create();

    jdbcTemplate.update(
        """
        INSERT INTO payments
          (id, user_id, expense_id, installment_id, account_id, amount,
           payment_date, type, notes, created_at)
        VALUES (?, ?, ?, ?, ?, 15.00, ?, NULL, NULL, ?)
        """,
        paymentId,
        owner.getId(),
        expense.getId(),
        installment.getId(),
        account.getId(),
        TODAY,
        Timestamp.from(NOW));

    String status =
        jdbcTemplate.queryForObject(
            "SELECT status FROM payments WHERE id = ?", String.class, paymentId);
    assertThat(status).isEqualTo("ACTIVE");
  }

  @Test
  @Transactional
  void shouldRejectInvalidPaymentStatus() {
    User owner = persistUser("phase8-pay-status@example.com");
    Account account = persistAccount(owner.getId(), "Caixa");
    Category category = persistCategory(owner.getId(), "Status", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, account, new BigDecimal("10.00"));
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, new BigDecimal("10.00"));

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO payments
                      (id, user_id, expense_id, installment_id, account_id, amount,
                       payment_date, type, notes, created_at, status)
                    VALUES (?, ?, ?, ?, ?, 10.00, ?, NULL, NULL, ?, 'CANCELLED')
                    """,
                    UuidV7.create(),
                    owner.getId(),
                    expense.getId(),
                    installment.getId(),
                    account.getId(),
                    TODAY,
                    Timestamp.from(NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Fixture persistFixture(String email) {
    User owner = persistUser(email);
    Category category = persistCategory(owner.getId(), "Servicos", CategoryType.EXPENSE);
    Expense expense = persistExpense(owner, category, null, new BigDecimal("50.00"));
    ExpenseInstallment installment = persistInstallment(expense, 1, 1, new BigDecimal("50.00"));
    return new Fixture(owner.getId(), installment.getId());
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
    account.setInitialBalance(new BigDecimal("500.00"));
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
      User owner, Category category, Account account, BigDecimal totalAmount) {
    Expense expense = new Expense();
    expense.setId(UuidV7.create());
    expense.setUserId(owner.getId());
    expense.setCategory(category);
    expense.setAccount(account);
    expense.setDescription("Despesa");
    expense.setTotalAmount(totalAmount);
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
      Expense expense, int number, int total, BigDecimal amount) {
    ExpenseInstallment installment = new ExpenseInstallment();
    installment.setId(UuidV7.create());
    installment.setUserId(expense.getUserId());
    installment.setExpense(expense);
    installment.setInstallmentNumber(number);
    installment.setTotalInstallments(total);
    installment.setAmount(amount);
    installment.setDueDate(TODAY);
    installment.setStatus(ExpenseStatus.OPEN);
    installment.setCreatedAt(NOW);
    installment.setUpdatedAt(NOW);
    return expenseInstallmentRepository.saveAndFlush(installment);
  }

  private List<String> columnsOf(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = ?
        """,
        String.class,
        table);
  }

  private List<String> constraintNames(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = ?::regclass
        """,
        String.class,
        table);
  }

  private String constraintDefinition(String table, String constraintName) {
    return jdbcTemplate.queryForObject(
        """
        SELECT pg_get_constraintdef(oid)
        FROM pg_constraint
        WHERE conrelid = ?::regclass
          AND conname = ?
        """,
        String.class,
        table,
        constraintName);
  }

  private record Fixture(UUID userId, UUID installmentId) {}
}
