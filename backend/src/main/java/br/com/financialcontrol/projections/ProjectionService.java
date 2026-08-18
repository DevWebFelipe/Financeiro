package br.com.financialcontrol.projections;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountRepository;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.InstallmentBalanceService;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.incomes.Income;
import br.com.financialcontrol.incomes.IncomeMovementRepository;
import br.com.financialcontrol.incomes.IncomeMovementType;
import br.com.financialcontrol.incomes.IncomeRepository;
import br.com.financialcontrol.projections.dto.ProjectionEventPageResponse;
import br.com.financialcontrol.projections.dto.ProjectionEventResponse;
import br.com.financialcontrol.projections.dto.ProjectionResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectionService {

  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";
  static final String INVALID_PAGE_SIZE_MAX = "O tamanho da página não pode ser maior que 100.";
  static final String INVALID_DATA = ProjectionPeriodResolver.INVALID_DATA;
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final int MAX_PAGE_SIZE = 100;
  private static final Set<ExpenseStatus> EXCLUDED_EXPENSE_STATUSES =
      EnumSet.of(ExpenseStatus.CANCELLED, ExpenseStatus.REFUNDED);
  private static final List<CreditCardInvoiceStatus> INVOICE_CANDIDATE_STATUSES =
      List.of(
          CreditCardInvoiceStatus.SCHEDULED,
          CreditCardInvoiceStatus.OPEN,
          CreditCardInvoiceStatus.CLOSED);

  private final AccountRepository accountRepository;
  private final AccountService accountService;
  private final IncomeRepository incomeRepository;
  private final IncomeMovementRepository incomeMovementRepository;
  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final InstallmentBalanceService installmentBalanceService;
  private final CreditCardInvoiceRepository invoiceRepository;
  private final CreditCardInvoiceService creditCardInvoiceService;
  private final ProjectionCalculator projectionCalculator;
  private final Clock clock;

  public ProjectionService(
      AccountRepository accountRepository,
      AccountService accountService,
      IncomeRepository incomeRepository,
      IncomeMovementRepository incomeMovementRepository,
      ExpenseInstallmentRepository expenseInstallmentRepository,
      InstallmentBalanceService installmentBalanceService,
      CreditCardInvoiceRepository invoiceRepository,
      CreditCardInvoiceService creditCardInvoiceService,
      ProjectionCalculator projectionCalculator,
      Clock clock) {
    this.accountRepository = accountRepository;
    this.accountService = accountService;
    this.incomeRepository = incomeRepository;
    this.incomeMovementRepository = incomeMovementRepository;
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.installmentBalanceService = installmentBalanceService;
    this.invoiceRepository = invoiceRepository;
    this.creditCardInvoiceService = creditCardInvoiceService;
    this.projectionCalculator = projectionCalculator;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ProjectionResponse project(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      Integer months,
      UUID accountId,
      int page,
      int size) {
    if (page < 0) {
      throw new BusinessRuleException(INVALID_PAGE);
    }
    if (size < 1) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE);
    }
    if (size > MAX_PAGE_SIZE) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE_MAX);
    }

    LocalDate asOfDate = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ProjectionHorizon horizon =
        ProjectionPeriodResolver.resolve(asOfDate, startDate, endDate, year, month, months);
    UUID userId = authenticatedUser.userId();
    BalanceSnapshot snapshot = loadBalances(userId, accountId);
    List<ProjectionEventInput> events =
        accountId == null ? loadEvents(userId, asOfDate, horizon.endDate()) : List.of();
    ProjectionCalculator.ProjectionComputation computation =
        projectionCalculator.calculate(
            snapshot.currentBalance(), snapshot.reservedAmount(), horizon, events);
    return new ProjectionResponse(
        computation.startDate(),
        computation.endDate(),
        computation.summary(),
        computation.months(),
        computation.quarters(),
        paginate(computation.datedEvents(), page, size),
        computation.undatedEvents());
  }

  private BalanceSnapshot loadBalances(UUID userId, UUID accountId) {
    if (accountId != null) {
      return accountRepository
          .findByIdAndUserId(accountId, userId)
          .map(
              account ->
                  new BalanceSnapshot(
                      accountService.calculateCurrentBalance(account),
                      accountService.calculateReservedAmount(account)))
          .orElse(BalanceSnapshot.ZERO);
    }
    BigDecimal current = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal reserved = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (Account account : accountRepository.findAllByUserIdOrderByCreatedAtAsc(userId)) {
      current = money(current.add(accountService.calculateCurrentBalance(account)));
      reserved = money(reserved.add(accountService.calculateReservedAmount(account)));
    }
    return new BalanceSnapshot(current, reserved);
  }

  private List<ProjectionEventInput> loadEvents(
      UUID userId, LocalDate asOfDate, LocalDate rangeEnd) {
    List<ProjectionEventInput> events = new ArrayList<>();
    events.addAll(incomeEvents(userId, asOfDate, rangeEnd));
    events.addAll(installmentEvents(userId, asOfDate, rangeEnd));
    events.addAll(invoiceEvents(userId, asOfDate, rangeEnd));
    return events;
  }

  private List<ProjectionEventInput> incomeEvents(
      UUID userId, LocalDate asOfDate, LocalDate rangeEnd) {
    List<Income> incomes =
        incomeRepository.findAllExpectedByUserIdAndExpectedDateLessThanEqual(userId, rangeEnd);
    Map<UUID, MovementTotals> totals = loadIncomeTotals(userId, incomes);
    List<ProjectionEventInput> events = new ArrayList<>();
    for (Income income : incomes) {
      MovementTotals movement = totals.getOrDefault(income.getId(), MovementTotals.ZERO);
      BigDecimal remaining =
          money(income.getAmount().add(movement.accrued()).subtract(movement.received()));
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      boolean overdue = income.getExpectedDate().isBefore(asOfDate);
      events.add(
          new ProjectionEventInput(
              income.getId(),
              ProjectionEventType.INCOME,
              income.getDescription(),
              remaining,
              ProjectionDirection.IN,
              income.getExpectedDate(),
              overdue));
    }
    return events;
  }

  private List<ProjectionEventInput> installmentEvents(
      UUID userId, LocalDate asOfDate, LocalDate rangeEnd) {
    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByUserIdAndPaymentMethodsExcludingStatusesDueOnOrBefore(
            userId,
            List.of(PaymentMethod.ACCOUNT, PaymentMethod.NONE),
            EXCLUDED_EXPENSE_STATUSES,
            rangeEnd);
    List<ProjectionEventInput> events = new ArrayList<>();
    for (ExpenseInstallment installment : installments) {
      BigDecimal remaining = installmentBalanceService.remaining(installment);
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      Expense expense = installment.getExpense();
      boolean overdue = installment.getDueDate().isBefore(asOfDate);
      events.add(
          new ProjectionEventInput(
              installment.getId(),
              ProjectionEventType.EXPENSE,
              expense.getDescription(),
              remaining,
              ProjectionDirection.OUT,
              installment.getDueDate(),
              overdue));
    }
    return events;
  }

  private List<ProjectionEventInput> invoiceEvents(
      UUID userId, LocalDate asOfDate, LocalDate rangeEnd) {
    List<CreditCardInvoice> invoices =
        invoiceRepository.findAllByUserIdAndStatusInWithCardDueOnOrBefore(
            userId, INVOICE_CANDIDATE_STATUSES, rangeEnd);
    List<ProjectionEventInput> events = new ArrayList<>();
    for (CreditCardInvoice invoice : invoices) {
      BigDecimal remaining = creditCardInvoiceService.remainingAmount(invoice);
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      boolean overdue = invoice.getDueDate().isBefore(asOfDate);
      events.add(
          new ProjectionEventInput(
              invoice.getId(),
              ProjectionEventType.CREDIT_CARD_INVOICE,
              invoice.getCreditCard().getName(),
              remaining,
              ProjectionDirection.OUT,
              invoice.getDueDate(),
              overdue));
    }
    return events;
  }

  private Map<UUID, MovementTotals> loadIncomeTotals(UUID userId, List<Income> incomes) {
    if (incomes.isEmpty()) {
      return Map.of();
    }
    List<UUID> ids = incomes.stream().map(Income::getId).toList();
    Map<UUID, MovementTotals> totals = new HashMap<>();
    for (Object[] row :
        incomeMovementRepository.sumActiveAmountsByIncomeIdsAndUserId(userId, ids)) {
      UUID incomeId = (UUID) row[0];
      IncomeMovementType type = (IncomeMovementType) row[1];
      BigDecimal amount = money((BigDecimal) row[2]);
      MovementTotals current = totals.getOrDefault(incomeId, MovementTotals.ZERO);
      if (type == IncomeMovementType.ACCRUAL) {
        totals.put(incomeId, new MovementTotals(amount, current.received()));
      } else if (type == IncomeMovementType.RECEIPT) {
        totals.put(incomeId, new MovementTotals(current.accrued(), amount));
      }
    }
    return totals;
  }

  private static ProjectionEventPageResponse paginate(
      List<ProjectionEventResponse> events, int page, int size) {
    int totalItems = events.size();
    int totalPages = totalItems == 0 ? 0 : (int) Math.ceil(totalItems / (double) size);
    int from = page * size;
    List<ProjectionEventResponse> items =
        from >= totalItems
            ? List.of()
            : List.copyOf(events.subList(from, Math.min(from + size, totalItems)));
    return new ProjectionEventPageResponse(items, page, size, totalItems, totalPages);
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private record BalanceSnapshot(BigDecimal currentBalance, BigDecimal reservedAmount) {
    static final BalanceSnapshot ZERO =
        new BalanceSnapshot(
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
  }

  private record MovementTotals(BigDecimal accrued, BigDecimal received) {
    static final MovementTotals ZERO =
        new MovementTotals(
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
  }
}
