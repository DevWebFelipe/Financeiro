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
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePayment;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_cards.CreditCard;
import br.com.financialcontrol.credit_cards.CreditCardRepository;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseRepository;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.financial_goals.FinancialGoal;
import br.com.financialcontrol.financial_goals.FinancialGoalRepository;
import br.com.financialcontrol.financial_goals.FinancialGoalStatus;
import br.com.financialcontrol.financial_goals.GoalContribution;
import br.com.financialcontrol.financial_goals.GoalContributionRepository;
import br.com.financialcontrol.incomes.Income;
import br.com.financialcontrol.incomes.IncomeRepository;
import br.com.financialcontrol.incomes.IncomeStatus;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.PaymentStatus;
import br.com.financialcontrol.transfers.Transfer;
import br.com.financialcontrol.transfers.TransferRepository;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class OwnershipAndPersistenceTest {

  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 13);

  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private CreditCardRepository creditCardRepository;
  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private ExpenseInstallmentRepository expenseInstallmentRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private TransferRepository transferRepository;
  @Autowired private IncomeRepository incomeRepository;
  @Autowired private CreditCardInvoiceRepository creditCardInvoiceRepository;
  @Autowired private CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;
  @Autowired private FinancialGoalRepository financialGoalRepository;
  @Autowired private GoalContributionRepository goalContributionRepository;

  @Test
  void shouldPersistAccountExpenseInstallmentAndPaymentForSameUser() {
    User owner = persistUser("owner@example.com");
    Account account = persistAccount(owner.getId(), "Nubank");
    Category category = persistCategory(owner.getId(), "Internet", CategoryType.EXPENSE);

    Expense expense = new Expense();
    expense.setId(UuidV7.create());
    expense.setUserId(owner.getId());
    expense.setCategory(category);
    expense.setAccount(account);
    expense.setDescription("Internet");
    expense.setTotalAmount(new BigDecimal("120.00"));
    expense.setExpenseDate(TODAY);
    expense.setDueDate(TODAY);
    expense.setPaymentMethod(PaymentMethod.ACCOUNT);
    expense.setStatus(ExpenseStatus.OPEN);
    expense.setResponsibleType(ResponsibleType.MINE);
    expense.setCreatedAt(NOW);
    expense.setUpdatedAt(NOW);
    expenseRepository.saveAndFlush(expense);

    ExpenseInstallment installment = persistInstallment(expense, null, new BigDecimal("120.00"));

    Payment payment = new Payment();
    payment.setId(UuidV7.create());
    payment.setUserId(owner.getId());
    payment.setExpense(expense);
    payment.setInstallment(installment);
    payment.setAccount(account);
    payment.setAmount(new BigDecimal("120.00"));
    payment.setPaymentDate(TODAY);
    payment.setType(null);
    payment.setCreatedAt(NOW);
    paymentRepository.saveAndFlush(payment);

    assertThat(expenseRepository.findById(expense.getId())).isPresent();
    assertThat(expenseInstallmentRepository.findById(installment.getId()))
        .get()
        .extracting(ExpenseInstallment::getInvoice)
        .isNull();
    assertThat(paymentRepository.findById(payment.getId()))
        .get()
        .satisfies(
            saved -> {
              assertThat(saved.getType()).isNull();
              assertThat(saved.getStatus()).isEqualTo(PaymentStatus.ACTIVE);
            });
  }

  @Test
  void shouldPersistCreditCardExpenseInstallmentLinkedToInvoice() {
    User owner = persistUser("card-owner@example.com");
    Category category = persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE);
    CreditCard card = persistCard(owner.getId());
    CreditCardInvoice invoice = persistInvoice(owner.getId(), card);

    Expense expense = new Expense();
    expense.setId(UuidV7.create());
    expense.setUserId(owner.getId());
    expense.setCategory(category);
    expense.setCreditCard(card);
    expense.setDescription("Mercado");
    expense.setTotalAmount(new BigDecimal("200.00"));
    expense.setExpenseDate(TODAY);
    expense.setDueDate(TODAY);
    expense.setPaymentMethod(PaymentMethod.CREDIT_CARD);
    expense.setStatus(ExpenseStatus.OPEN);
    expense.setResponsibleType(ResponsibleType.MINE);
    expense.setCreatedAt(NOW);
    expense.setUpdatedAt(NOW);
    expenseRepository.saveAndFlush(expense);

    ExpenseInstallment installment = persistInstallment(expense, invoice, new BigDecimal("200.00"));

    assertThat(expenseInstallmentRepository.findById(installment.getId()))
        .get()
        .extracting(saved -> saved.getInvoice().getId())
        .isEqualTo(invoice.getId());
  }

  @Test
  void shouldRejectExpenseWithCategoryOfAnotherUser() {
    User owner = persistUser("user-a@example.com");
    User other = persistUser("user-b@example.com");
    Category foreignCategory = persistCategory(other.getId(), "Outro", CategoryType.EXPENSE);

    Expense expense = new Expense();
    expense.setId(UuidV7.create());
    expense.setUserId(owner.getId());
    expense.setCategory(foreignCategory);
    expense.setDescription("Cruzado");
    expense.setTotalAmount(new BigDecimal("10.00"));
    expense.setExpenseDate(TODAY);
    expense.setDueDate(TODAY);
    expense.setPaymentMethod(PaymentMethod.NONE);
    expense.setStatus(ExpenseStatus.OPEN);
    expense.setResponsibleType(ResponsibleType.MINE);
    expense.setCreatedAt(NOW);
    expense.setUpdatedAt(NOW);

    assertThatThrownBy(() -> expenseRepository.saveAndFlush(expense))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRejectTransferBetweenAccountsOfDifferentUsers() {
    User owner = persistUser("transfer-a@example.com");
    User other = persistUser("transfer-b@example.com");
    Account source = persistAccount(owner.getId(), "Origem");
    Account foreignDestination = persistAccount(other.getId(), "Destino");

    Transfer transfer = new Transfer();
    transfer.setId(UuidV7.create());
    transfer.setUserId(owner.getId());
    transfer.setSourceAccount(source);
    transfer.setDestinationAccount(foreignDestination);
    transfer.setAmount(new BigDecimal("50.00"));
    transfer.setTransferDate(TODAY);
    transfer.setCreatedAt(NOW);

    assertThatThrownBy(() -> transferRepository.saveAndFlush(transfer))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldPersistTransferIncomeGoalAndInvoicePaymentForSameUser() {
    User owner = persistUser("full@example.com");
    Account source = persistAccount(owner.getId(), "Nubank");
    Account destination = persistAccount(owner.getId(), "Itau");
    Category incomeCategory = persistCategory(owner.getId(), "Salario", CategoryType.INCOME);
    CreditCard card = persistCard(owner.getId());
    CreditCardInvoice invoice = persistInvoice(owner.getId(), card);

    Transfer transfer = new Transfer();
    transfer.setId(UuidV7.create());
    transfer.setUserId(owner.getId());
    transfer.setSourceAccount(source);
    transfer.setDestinationAccount(destination);
    transfer.setAmount(new BigDecimal("80.00"));
    transfer.setTransferDate(TODAY);
    transfer.setCreatedAt(NOW);
    transferRepository.saveAndFlush(transfer);

    Income income = new Income();
    income.setId(UuidV7.create());
    income.setUserId(owner.getId());
    income.setCategory(incomeCategory);
    income.setDescription("Salario");
    income.setAmount(new BigDecimal("3000.00"));
    income.setExpectedDate(TODAY);
    income.setStatus(IncomeStatus.EXPECTED);
    income.setResponsibleType(ResponsibleType.MINE);
    income.setCreatedAt(NOW);
    income.setUpdatedAt(NOW);
    incomeRepository.saveAndFlush(income);

    Income incomeWithoutResponsible = new Income();
    incomeWithoutResponsible.setId(UuidV7.create());
    incomeWithoutResponsible.setUserId(owner.getId());
    incomeWithoutResponsible.setCategory(incomeCategory);
    incomeWithoutResponsible.setDescription("Freelance");
    incomeWithoutResponsible.setAmount(new BigDecimal("500.00"));
    incomeWithoutResponsible.setExpectedDate(TODAY);
    incomeWithoutResponsible.setStatus(IncomeStatus.EXPECTED);
    incomeWithoutResponsible.setCreatedAt(NOW);
    incomeWithoutResponsible.setUpdatedAt(NOW);
    incomeRepository.saveAndFlush(incomeWithoutResponsible);
    Income reloaded = incomeRepository.findById(incomeWithoutResponsible.getId()).orElseThrow();
    assertThat(reloaded.getResponsibleType()).isNull();
    assertThat(reloaded.getResponsibleName()).isNull();

    FinancialGoal goal = new FinancialGoal();
    goal.setId(UuidV7.create());
    goal.setUserId(owner.getId());
    goal.setName("Reserva");
    goal.setTargetAmount(new BigDecimal("1000.00"));
    goal.setStatus(FinancialGoalStatus.ACTIVE);
    goal.setCreatedAt(NOW);
    goal.setUpdatedAt(NOW);
    financialGoalRepository.saveAndFlush(goal);

    GoalContribution contribution = new GoalContribution();
    contribution.setId(UuidV7.create());
    contribution.setUserId(owner.getId());
    contribution.setGoal(goal);
    contribution.setAccount(source);
    contribution.setAmount(new BigDecimal("100.00"));
    contribution.setContributionDate(TODAY);
    contribution.setCreatedAt(NOW);
    goalContributionRepository.saveAndFlush(contribution);

    CreditCardInvoicePayment invoicePayment = new CreditCardInvoicePayment();
    invoicePayment.setId(UuidV7.create());
    invoicePayment.setUserId(owner.getId());
    invoicePayment.setInvoice(invoice);
    invoicePayment.setAccount(source);
    invoicePayment.setAmount(new BigDecimal("50.00"));
    invoicePayment.setPaymentDate(TODAY);
    invoicePayment.setCreatedAt(NOW);
    creditCardInvoicePaymentRepository.saveAndFlush(invoicePayment);

    assertThat(transferRepository.findById(transfer.getId())).isPresent();
    assertThat(incomeRepository.findById(income.getId())).isPresent();
    assertThat(goalContributionRepository.findById(contribution.getId())).isPresent();
    assertThat(creditCardInvoicePaymentRepository.findById(invoicePayment.getId())).isPresent();
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

  private CreditCard persistCard(UUID userId) {
    CreditCard card = new CreditCard();
    card.setId(UuidV7.create());
    card.setUserId(userId);
    card.setName("Nubank");
    card.setHolderName("Felipe");
    card.setLastFourDigits("1234");
    card.setCreditLimit(new BigDecimal("5000.00"));
    card.setClosingDay(10);
    card.setDueDay(17);
    card.setActive(true);
    card.setCreatedAt(NOW);
    card.setUpdatedAt(NOW);
    return creditCardRepository.saveAndFlush(card);
  }

  private CreditCardInvoice persistInvoice(UUID userId, CreditCard card) {
    CreditCardInvoice invoice = new CreditCardInvoice();
    invoice.setId(UuidV7.create());
    invoice.setUserId(userId);
    invoice.setCreditCard(card);
    invoice.setReferenceYear(2026);
    invoice.setReferenceMonth(8);
    invoice.setClosingDate(LocalDate.of(2026, 8, 10));
    invoice.setDueDate(LocalDate.of(2026, 8, 17));
    invoice.setStatus(CreditCardInvoiceStatus.OPEN);
    invoice.setCreatedAt(NOW);
    invoice.setUpdatedAt(NOW);
    return creditCardInvoiceRepository.saveAndFlush(invoice);
  }

  private ExpenseInstallment persistInstallment(
      Expense expense, CreditCardInvoice invoice, BigDecimal amount) {
    ExpenseInstallment installment = new ExpenseInstallment();
    installment.setId(UuidV7.create());
    installment.setUserId(expense.getUserId());
    installment.setExpense(expense);
    installment.setInvoice(invoice);
    installment.setInstallmentNumber(1);
    installment.setTotalInstallments(1);
    installment.setAmount(amount);
    installment.setDueDate(TODAY);
    installment.setStatus(ExpenseStatus.OPEN);
    installment.setCreatedAt(NOW);
    installment.setUpdatedAt(NOW);
    return expenseInstallmentRepository.saveAndFlush(installment);
  }
}
