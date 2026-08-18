package br.com.financialcontrol.reports;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountRepository;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.balance_adjustments.AccountBalanceAdjustment;
import br.com.financialcontrol.balance_adjustments.AccountBalanceAdjustmentRepository;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryType;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_card_invoice_agreements.CreditCardInvoiceAgreementRepository;
import br.com.financialcontrol.credit_card_invoice_agreements.CreditCardInvoiceAgreementSettlementAllocation;
import br.com.financialcontrol.credit_card_invoice_agreements.CreditCardInvoiceAgreementSettlementAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceAdjustment;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceAdjustmentAllocation;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceAdjustmentAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceAdjustmentRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePayment;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentAllocation;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.InvoicePaymentStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoiceAdjustmentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoicePaymentResponse;
import br.com.financialcontrol.credit_cards.CardPurchaseAccountRefund;
import br.com.financialcontrol.credit_cards.CardPurchaseAccountRefundRepository;
import br.com.financialcontrol.credit_cards.CreditCard;
import br.com.financialcontrol.credit_cards.CreditCardCreditApplication;
import br.com.financialcontrol.credit_cards.CreditCardCreditApplicationRepository;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentAdjustment;
import br.com.financialcontrol.expenses.ExpenseInstallmentAdjustmentRepository;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseRepository;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.InstallmentBalanceService;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import br.com.financialcontrol.incomes.Income;
import br.com.financialcontrol.incomes.IncomeMovement;
import br.com.financialcontrol.incomes.IncomeMovementRepository;
import br.com.financialcontrol.incomes.IncomeMovementType;
import br.com.financialcontrol.incomes.IncomeRepository;
import br.com.financialcontrol.incomes.IncomeStatus;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.projections.ProjectionService;
import br.com.financialcontrol.projections.dto.ProjectionResponse;
import br.com.financialcontrol.reports.dto.CardReportCreditApplicationResponse;
import br.com.financialcontrol.reports.dto.CardReportItemResponse;
import br.com.financialcontrol.reports.dto.CardReportPurchaseInstallmentResponse;
import br.com.financialcontrol.reports.dto.CardReportPurchaseResponse;
import br.com.financialcontrol.reports.dto.CardReportResponse;
import br.com.financialcontrol.reports.dto.CardReportSummaryResponse;
import br.com.financialcontrol.reports.dto.CashFlowHistoricalResponse;
import br.com.financialcontrol.reports.dto.CashFlowItemResponse;
import br.com.financialcontrol.reports.dto.CashFlowProjectedResponse;
import br.com.financialcontrol.reports.dto.CashFlowResponse;
import br.com.financialcontrol.reports.dto.CashFlowSummaryResponse;
import br.com.financialcontrol.reports.dto.CategoryReportItemResponse;
import br.com.financialcontrol.reports.dto.CategoryReportResponse;
import br.com.financialcontrol.reports.dto.CategoryReportSummaryResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportInstallmentResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportItemResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportSummaryResponse;
import br.com.financialcontrol.reports.dto.IncomeReportItemResponse;
import br.com.financialcontrol.reports.dto.IncomeReportResponse;
import br.com.financialcontrol.reports.dto.IncomeReportSummaryResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportAllocationResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportCardResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportCategoryGroupResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportHeaderResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportPurchaseResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportResponsibleGroupResponse;
import br.com.financialcontrol.reports.dto.ReportPeriodResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportItemResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportSummaryResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.transfers.Transfer;
import br.com.financialcontrol.transfers.TransferRepository;
import br.com.financialcontrol.transfers.TransferStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportsService {

  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";
  static final String INVALID_PAGE_SIZE_MAX = "O tamanho da página não pode ser maior que 100.";
  static final String INVALID_DATA = "Dados inválidos.";
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final int MAX_PAGE_SIZE = 100;
  private static final int PDF_UNIVERSE = Integer.MAX_VALUE;
  private static final int MAX_MONTHS = 12;

  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final ExpenseInstallmentAdjustmentRepository adjustmentRepository;
  private final PaymentRepository paymentRepository;
  private final InstallmentBalanceService installmentBalanceService;
  private final CreditCardInvoiceAgreementRepository agreementRepository;
  private final IncomeRepository incomeRepository;
  private final IncomeMovementRepository incomeMovementRepository;
  private final ExpenseRepository expenseRepository;
  private final CreditCardInvoiceRepository creditCardInvoiceRepository;
  private final CreditCardInvoicePaymentRepository invoicePaymentRepository;
  private final CreditCardCreditApplicationRepository creditApplicationRepository;
  private final CreditCardInvoiceAdjustmentRepository invoiceAdjustmentRepository;
  private final CreditCardInvoicePaymentAllocationRepository paymentAllocationRepository;
  private final CreditCardInvoiceAdjustmentAllocationRepository
      invoiceAdjustmentAllocationRepository;
  private final CreditCardInvoiceAgreementSettlementAllocationRepository
      settlementAllocationRepository;
  private final CreditCardInvoiceService creditCardInvoiceService;
  private final AccountService accountService;
  private final AccountRepository accountRepository;
  private final ProjectionService projectionService;
  private final TransferRepository transferRepository;
  private final CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository;
  private final AccountBalanceAdjustmentRepository balanceAdjustmentRepository;
  private final ReportsPdfRenderer reportsPdfRenderer;
  private final Clock clock;

  public ReportsService(
      ExpenseInstallmentRepository expenseInstallmentRepository,
      ExpenseInstallmentAdjustmentRepository adjustmentRepository,
      PaymentRepository paymentRepository,
      InstallmentBalanceService installmentBalanceService,
      CreditCardInvoiceAgreementRepository agreementRepository,
      IncomeRepository incomeRepository,
      IncomeMovementRepository incomeMovementRepository,
      ExpenseRepository expenseRepository,
      CreditCardInvoiceRepository creditCardInvoiceRepository,
      CreditCardInvoicePaymentRepository invoicePaymentRepository,
      CreditCardCreditApplicationRepository creditApplicationRepository,
      CreditCardInvoiceAdjustmentRepository invoiceAdjustmentRepository,
      CreditCardInvoicePaymentAllocationRepository paymentAllocationRepository,
      CreditCardInvoiceAdjustmentAllocationRepository invoiceAdjustmentAllocationRepository,
      CreditCardInvoiceAgreementSettlementAllocationRepository settlementAllocationRepository,
      CreditCardInvoiceService creditCardInvoiceService,
      AccountService accountService,
      AccountRepository accountRepository,
      ProjectionService projectionService,
      TransferRepository transferRepository,
      CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository,
      AccountBalanceAdjustmentRepository balanceAdjustmentRepository,
      ReportsPdfRenderer reportsPdfRenderer,
      Clock clock) {
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.paymentRepository = paymentRepository;
    this.installmentBalanceService = installmentBalanceService;
    this.agreementRepository = agreementRepository;
    this.incomeRepository = incomeRepository;
    this.incomeMovementRepository = incomeMovementRepository;
    this.expenseRepository = expenseRepository;
    this.creditCardInvoiceRepository = creditCardInvoiceRepository;
    this.invoicePaymentRepository = invoicePaymentRepository;
    this.creditApplicationRepository = creditApplicationRepository;
    this.invoiceAdjustmentRepository = invoiceAdjustmentRepository;
    this.paymentAllocationRepository = paymentAllocationRepository;
    this.invoiceAdjustmentAllocationRepository = invoiceAdjustmentAllocationRepository;
    this.settlementAllocationRepository = settlementAllocationRepository;
    this.creditCardInvoiceService = creditCardInvoiceService;
    this.accountService = accountService;
    this.accountRepository = accountRepository;
    this.projectionService = projectionService;
    this.transferRepository = transferRepository;
    this.cardPurchaseAccountRefundRepository = cardPurchaseAccountRefundRepository;
    this.balanceAdjustmentRepository = balanceAdjustmentRepository;
    this.reportsPdfRenderer = reportsPdfRenderer;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ExpenseReportResponse listExpenses(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      ExpenseStatus status,
      UUID categoryId,
      UUID accountId,
      UUID creditCardId,
      ResponsibleType responsibleType,
      String responsibleName,
      PaymentMethod paymentMethod,
      String sort,
      String direction,
      int page,
      int size) {
    return listExpenses(
        authenticatedUser,
        startDate,
        endDate,
        status,
        categoryId,
        accountId,
        creditCardId,
        responsibleType,
        responsibleName,
        paymentMethod,
        sort,
        direction,
        page,
        size,
        true);
  }

  private ExpenseReportResponse listExpenses(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      ExpenseStatus status,
      UUID categoryId,
      UUID accountId,
      UUID creditCardId,
      ResponsibleType responsibleType,
      String responsibleName,
      PaymentMethod paymentMethod,
      String sort,
      String direction,
      int page,
      int size,
      boolean paginate) {
    if (paginate) {
      validatePagination(page, size);
    }
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ReportPeriodResponse period = resolvePeriod(startDate, endDate, today);
    ExpenseReportSortField sortField = parseSort(sort);
    boolean descending = parseDescending(direction);
    UUID userId = authenticatedUser.userId();

    List<ExpenseInstallment> candidates =
        expenseInstallmentRepository.findAllByUserIdAndDueDateBetween(
            userId, period.startDate(), period.endDate());
    Map<UUID, List<ExpenseInstallment>> grouped = new LinkedHashMap<>();
    for (ExpenseInstallment installment : candidates) {
      Expense expense = installment.getExpense();
      if (!matchesExpenseFilters(
          expense,
          status,
          categoryId,
          accountId,
          creditCardId,
          responsibleType,
          responsibleName,
          paymentMethod)) {
        continue;
      }
      grouped.computeIfAbsent(expense.getId(), unused -> new ArrayList<>()).add(installment);
    }

    Set<UUID> agreementExpenseIds = agreementExpenseIds(userId, grouped.keySet());
    List<RankedItem> rankedItems = new ArrayList<>();
    PeriodTotals summaryTotals = PeriodTotals.zero();
    for (Map.Entry<UUID, List<ExpenseInstallment>> entry : grouped.entrySet()) {
      List<ExpenseInstallment> recorte = entry.getValue();
      recorte.sort(
          Comparator.comparingInt(ExpenseInstallment::getInstallmentNumber)
              .thenComparing(ExpenseInstallment::getId));
      Expense expense = recorte.getFirst().getExpense();
      ExpenseReportOrigin origin =
          agreementExpenseIds.contains(expense.getId())
              ? ExpenseReportOrigin.AGREEMENT
              : ExpenseReportOrigin.PURCHASE;
      List<ExpenseReportInstallmentResponse> installmentItems = new ArrayList<>();
      PeriodTotals itemTotals = PeriodTotals.zero();
      LocalDate minDueDate = recorte.getFirst().getDueDate();
      for (ExpenseInstallment installment : recorte) {
        InstallmentAmounts amounts = amountsOf(installment);
        installmentItems.add(toInstallmentResponse(installment, amounts));
        itemTotals = itemTotals.add(amounts.toPeriodTotals());
        if (countsTowardSummary(expense, installment)) {
          summaryTotals = summaryTotals.add(amounts.toPeriodTotals());
        }
        if (installment.getDueDate().isBefore(minDueDate)) {
          minDueDate = installment.getDueDate();
        }
      }
      PeriodTotals itemPeriod = itemTotals.withPaidFor(expense.getPaymentMethod());
      rankedItems.add(
          new RankedItem(
              toItemResponse(expense, origin, itemPeriod, installmentItems),
              minDueDate,
              expense.getCreatedAt()));
    }

    rankedItems.sort(comparator(sortField, descending));
    List<ExpenseReportItemResponse> allItems = rankedItems.stream().map(RankedItem::item).toList();
    ExpenseReportSummaryResponse summary = summaryTotals.toSummary();
    List<ExpenseReportItemResponse> pageItems = pageItems(allItems, page, size, paginate);
    int totalItems = allItems.size();
    return new ExpenseReportResponse(
        period,
        pageItems,
        paginate ? page : 0,
        paginate ? size : totalItems,
        totalItems,
        totalPages(totalItems, size, paginate),
        summary);
  }

  @Transactional(readOnly = true)
  public IncomeReportResponse listIncomes(
      AuthenticatedUser authenticatedUser,
      String dateType,
      LocalDate startDate,
      LocalDate endDate,
      String status,
      UUID categoryId,
      UUID accountId,
      ResponsibleType responsibleType,
      String responsibleName,
      String sort,
      String direction,
      int page,
      int size) {
    validatePagination(page, size);
    ReportDateType parsedDateType = parseDateType(dateType);
    IncomeStatus parsedStatus = parseIncomeStatus(status);
    IncomeReportSortField sortField = parseIncomeSort(sort, parsedDateType);
    boolean descending = parseDescending(direction);
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ReportPeriodResponse period = resolvePeriod(startDate, endDate, today);
    UUID userId = authenticatedUser.userId();
    LoadedIncomes loaded =
        loadIncomes(
            userId,
            period,
            parsedDateType,
            parsedStatus,
            categoryId,
            accountId,
            responsibleType,
            responsibleName);
    List<Income> incomes = loaded.incomes();
    Map<UUID, BigDecimal> periodReceivedByIncome = loaded.periodReceivedByIncome();
    Map<UUID, IncomeTotals> totalsByIncome = loaded.totalsByIncome();
    List<RankedIncomeItem> rankedItems = new ArrayList<>();
    IncomeTotals summaryTotals = IncomeTotals.ZERO;
    BigDecimal summaryPeriodReceived = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (Income income : incomes) {
      IncomeTotals totals = totalsByIncome.getOrDefault(income.getId(), IncomeTotals.ZERO);
      BigDecimal periodReceived =
          parsedDateType == ReportDateType.RECEIVED
              ? money(periodReceivedByIncome.getOrDefault(income.getId(), BigDecimal.ZERO))
              : null;
      rankedItems.add(
          new RankedIncomeItem(
              toIncomeItem(income, totals, periodReceived), income.getCreatedAt()));
      if (income.getStatus() != IncomeStatus.CANCELLED) {
        summaryTotals = summaryTotals.add(totals, money(income.getAmount()));
        if (periodReceived != null) {
          summaryPeriodReceived = money(summaryPeriodReceived.add(periodReceived));
        }
      }
    }

    rankedItems.sort(incomeComparator(sortField, descending));
    List<IncomeReportItemResponse> allItems =
        rankedItems.stream().map(RankedIncomeItem::item).toList();
    IncomeReportSummaryResponse summary =
        parsedDateType == ReportDateType.RECEIVED
            ? IncomeReportSummaryResponse.received(summaryPeriodReceived)
            : IncomeReportSummaryResponse.expected(
                summaryTotals.amount,
                summaryTotals.accrued,
                summaryTotals.received,
                summaryTotals.remaining());
    int totalItems = allItems.size();
    boolean paginate = paginated(size);
    return new IncomeReportResponse(
        period,
        parsedDateType,
        pageItems(allItems, page, size, paginate),
        paginate ? page : 0,
        paginate ? size : totalItems,
        totalItems,
        totalPages(totalItems, size, paginate),
        summary);
  }

  @Transactional(readOnly = true)
  public CategoryReportResponse listCategories(
      AuthenticatedUser authenticatedUser,
      String dateType,
      LocalDate startDate,
      LocalDate endDate,
      String sort,
      String direction,
      int page,
      int size) {
    validatePagination(page, size);
    ReportDateType parsedDateType = parseDateType(dateType);
    CategoryReportSortField sortField = parseCategorySort(sort);
    boolean descending = parseDescending(direction);
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ReportPeriodResponse period = resolvePeriod(startDate, endDate, today);
    UUID userId = authenticatedUser.userId();

    Map<UUID, CategoryExpenseBucket> expenseBuckets = new LinkedHashMap<>();
    PeriodTotals expenseSummary = PeriodTotals.zero();
    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByUserIdAndDueDateBetween(
            userId, period.startDate(), period.endDate());
    for (ExpenseInstallment installment : installments) {
      Expense expense = installment.getExpense();
      Category category = expense.getCategory();
      InstallmentAmounts amounts = amountsOf(installment);
      PeriodTotals periodTotals = amounts.toPeriodTotals();
      CategoryExpenseBucket bucket =
          expenseBuckets.computeIfAbsent(
              category.getId(),
              unused ->
                  new CategoryExpenseBucket(
                      category.getId(),
                      category.getName(),
                      category.isActive(),
                      PeriodTotals.zero()));
      bucket.itemTotals = bucket.itemTotals.add(periodTotals);
      if (countsTowardSummary(expense, installment)) {
        expenseSummary = expenseSummary.add(periodTotals);
      }
    }

    LoadedIncomes loaded =
        loadIncomes(userId, period, parsedDateType, null, null, null, null, null);
    Map<UUID, CategoryIncomeBucket> incomeBuckets = new LinkedHashMap<>();
    IncomeTotals incomeSummary = IncomeTotals.ZERO;
    BigDecimal incomePeriodReceived = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (Income income : loaded.incomes()) {
      Category category = income.getCategory();
      IncomeTotals totals = loaded.totalsByIncome().getOrDefault(income.getId(), IncomeTotals.ZERO);
      BigDecimal periodReceived =
          parsedDateType == ReportDateType.RECEIVED
              ? money(loaded.periodReceivedByIncome().getOrDefault(income.getId(), BigDecimal.ZERO))
              : null;
      CategoryIncomeBucket bucket =
          incomeBuckets.computeIfAbsent(
              category.getId(),
              unused ->
                  new CategoryIncomeBucket(
                      category.getId(),
                      category.getName(),
                      category.isActive(),
                      IncomeTotals.ZERO,
                      null));
      bucket.itemTotals = bucket.itemTotals.add(totals, money(income.getAmount()));
      if (periodReceived != null) {
        bucket.periodReceived =
            money(
                (bucket.periodReceived == null ? BigDecimal.ZERO : bucket.periodReceived)
                    .add(periodReceived));
      }
      if (income.getStatus() != IncomeStatus.CANCELLED) {
        incomeSummary = incomeSummary.add(totals, money(income.getAmount()));
        if (periodReceived != null) {
          incomePeriodReceived = money(incomePeriodReceived.add(periodReceived));
        }
      }
    }

    List<CategoryReportItemResponse> allItems = new ArrayList<>();
    for (CategoryExpenseBucket bucket : expenseBuckets.values()) {
      allItems.add(toExpenseCategoryItem(bucket));
    }
    for (CategoryIncomeBucket bucket : incomeBuckets.values()) {
      allItems.add(toIncomeCategoryItem(bucket, parsedDateType));
    }
    allItems.sort(categoryComparator(sortField, descending));

    IncomeReportSummaryResponse incomeSummaryResponse =
        parsedDateType == ReportDateType.RECEIVED
            ? IncomeReportSummaryResponse.received(incomePeriodReceived)
            : IncomeReportSummaryResponse.expected(
                incomeSummary.amount,
                incomeSummary.accrued,
                incomeSummary.received,
                incomeSummary.remaining());
    CategoryReportSummaryResponse summary =
        new CategoryReportSummaryResponse(expenseSummary.toSummary(), incomeSummaryResponse);
    int totalItems = allItems.size();
    boolean paginate = paginated(size);
    return new CategoryReportResponse(
        period,
        parsedDateType,
        pageItems(allItems, page, size, paginate),
        paginate ? page : 0,
        paginate ? size : totalItems,
        totalItems,
        totalPages(totalItems, size, paginate),
        summary);
  }

  @Transactional(readOnly = true)
  public ResponsibleReportResponse listResponsibles(
      AuthenticatedUser authenticatedUser,
      String nature,
      String dateType,
      LocalDate startDate,
      LocalDate endDate,
      String sort,
      String direction,
      int page,
      int size) {
    validatePagination(page, size);
    ReportNature parsedNature = parseNature(nature);
    ReportDateType parsedDateType = parseResponsibleDateType(parsedNature, dateType);
    ResponsibleReportSortField sortField = parseResponsibleSort(sort);
    boolean descending = parseDescending(direction);
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ReportPeriodResponse period = resolvePeriod(startDate, endDate, today);
    UUID userId = authenticatedUser.userId();
    boolean includeExpense =
        parsedNature == ReportNature.EXPENSE || parsedNature == ReportNature.BOTH;
    boolean includeIncome =
        parsedNature == ReportNature.INCOME || parsedNature == ReportNature.BOTH;

    Map<String, ResponsibleBucket> buckets = new LinkedHashMap<>();
    PeriodTotals expenseSummary = PeriodTotals.zero();
    if (includeExpense) {
      List<ExpenseInstallment> installments =
          expenseInstallmentRepository.findAllByUserIdAndDueDateBetween(
              userId, period.startDate(), period.endDate());
      for (ExpenseInstallment installment : installments) {
        Expense expense = installment.getExpense();
        ResponsibleGroup group =
            groupOf(expense.getResponsibleType(), expense.getResponsibleName());
        InstallmentAmounts amounts = amountsOf(installment);
        PeriodTotals periodTotals = amounts.toPeriodTotals();
        ResponsibleBucket bucket = bucketOf(buckets, group);
        bucket.expenseTotals = bucket.expenseTotals.add(periodTotals);
        bucket.hasExpense = true;
        if (countsTowardSummary(expense, installment)) {
          expenseSummary = expenseSummary.add(periodTotals);
        }
      }
    }

    IncomeTotals incomeSummary = IncomeTotals.ZERO;
    BigDecimal incomePeriodReceived = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    if (includeIncome) {
      LoadedIncomes loaded =
          loadIncomes(userId, period, parsedDateType, null, null, null, null, null);
      for (Income income : loaded.incomes()) {
        ResponsibleGroup group = groupOf(income.getResponsibleType(), income.getResponsibleName());
        IncomeTotals totals =
            loaded.totalsByIncome().getOrDefault(income.getId(), IncomeTotals.ZERO);
        BigDecimal periodReceived =
            parsedDateType == ReportDateType.RECEIVED
                ? money(
                    loaded.periodReceivedByIncome().getOrDefault(income.getId(), BigDecimal.ZERO))
                : null;
        ResponsibleBucket bucket = bucketOf(buckets, group);
        bucket.incomeTotals = bucket.incomeTotals.add(totals, money(income.getAmount()));
        bucket.hasIncome = true;
        if (periodReceived != null) {
          bucket.periodReceived =
              money(
                  (bucket.periodReceived == null ? BigDecimal.ZERO : bucket.periodReceived)
                      .add(periodReceived));
        }
        if (income.getStatus() != IncomeStatus.CANCELLED) {
          incomeSummary = incomeSummary.add(totals, money(income.getAmount()));
          if (periodReceived != null) {
            incomePeriodReceived = money(incomePeriodReceived.add(periodReceived));
          }
        }
      }
    }

    List<ResponsibleReportItemResponse> allItems = new ArrayList<>();
    for (ResponsibleBucket bucket : buckets.values()) {
      allItems.add(toResponsibleItem(bucket, parsedDateType, includeExpense, includeIncome));
    }
    allItems.sort(responsibleComparator(sortField, descending));

    ExpenseReportSummaryResponse expenseSummaryResponse =
        includeExpense ? expenseSummary.toSummary() : null;
    IncomeReportSummaryResponse incomeSummaryResponse = null;
    if (includeIncome) {
      incomeSummaryResponse =
          parsedDateType == ReportDateType.RECEIVED
              ? IncomeReportSummaryResponse.received(incomePeriodReceived)
              : IncomeReportSummaryResponse.expected(
                  incomeSummary.amount,
                  incomeSummary.accrued,
                  incomeSummary.received,
                  incomeSummary.remaining());
    }
    ResponsibleReportSummaryResponse summary =
        new ResponsibleReportSummaryResponse(expenseSummaryResponse, incomeSummaryResponse);
    int totalItems = allItems.size();
    boolean paginate = paginated(size);
    return new ResponsibleReportResponse(
        period,
        parsedNature,
        parsedDateType,
        pageItems(allItems, page, size, paginate),
        paginate ? page : 0,
        paginate ? size : totalItems,
        totalItems,
        totalPages(totalItems, size, paginate),
        summary);
  }

  @Transactional(readOnly = true)
  public CardReportResponse listCards(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      UUID creditCardId,
      String sort,
      String direction,
      int page,
      int size) {
    validatePagination(page, size);
    CardReportSortField sortField = parseCardSort(sort);
    boolean descending = parseDescending(direction);
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ReportPeriodResponse period = resolvePeriod(startDate, endDate, today);
    UUID userId = authenticatedUser.userId();
    Instant startInstant = period.startDate().atStartOfDay(FINANCIAL_ZONE).toInstant();
    Instant endInstant = period.endDate().plusDays(1).atStartOfDay(FINANCIAL_ZONE).toInstant();

    Map<UUID, CardBucket> buckets = new LinkedHashMap<>();
    for (Expense expense :
        expenseRepository.findCreditCardPurchasesByUserIdAndExpenseDateBetween(
            userId, period.startDate(), period.endDate(), creditCardId)) {
      bucketOf(buckets, expense.getCreditCard()).purchases.add(expense);
    }
    for (CreditCardInvoice invoice :
        creditCardInvoiceRepository.findAllByUserIdAndClosingDateBetween(
            userId, period.startDate(), period.endDate(), creditCardId)) {
      bucketOf(buckets, invoice.getCreditCard()).invoices.add(invoice);
    }
    for (CreditCardInvoicePayment payment :
        invoicePaymentRepository.findAllByUserIdAndPaymentDateBetween(
            userId, period.startDate(), period.endDate(), creditCardId)) {
      bucketOf(buckets, payment.getInvoice().getCreditCard()).payments.add(payment);
    }
    for (CreditCardCreditApplication application :
        creditApplicationRepository.findAllByUserIdAndCreatedAtBetween(
            userId, startInstant, endInstant, creditCardId)) {
      bucketOf(buckets, application.getCredit().getCreditCard()).credits.add(application);
    }
    for (ExpenseInstallmentAdjustment installmentAdjustment :
        adjustmentRepository.findCreditCardAdjustmentsByUserIdAndCreatedAtBetween(
            userId, startInstant, endInstant, creditCardId)) {
      bucketOf(buckets, installmentAdjustment.getInstallment().getExpense().getCreditCard())
          .installmentAdjustments
          .add(installmentAdjustment);
    }
    for (CreditCardInvoiceAdjustment invoiceAdjustment :
        invoiceAdjustmentRepository.findAllByUserIdAndCreatedAtBetween(
            userId, startInstant, endInstant, creditCardId)) {
      bucketOf(buckets, invoiceAdjustment.getInvoice().getCreditCard())
          .invoiceAdjustments
          .add(invoiceAdjustment);
    }

    List<UUID> purchaseIds =
        buckets.values().stream()
            .flatMap(bucket -> bucket.purchases.stream())
            .map(Expense::getId)
            .toList();
    Map<UUID, List<ExpenseInstallment>> installmentsByExpense = new LinkedHashMap<>();
    if (!purchaseIds.isEmpty()) {
      for (ExpenseInstallment installment :
          expenseInstallmentRepository
              .findAllByExpense_IdInAndUserIdOrderByExpense_IdAscInstallmentNumberAsc(
                  purchaseIds, userId)) {
        installmentsByExpense
            .computeIfAbsent(installment.getExpense().getId(), unused -> new ArrayList<>())
            .add(installment);
      }
    }

    List<CardReportItemResponse> allItems = new ArrayList<>();
    CardReportSummaryResponse envelopeSummary = emptyCardSummary();
    for (CardBucket bucket : buckets.values()) {
      CardReportItemResponse item = toCardItem(bucket, installmentsByExpense);
      allItems.add(item);
      envelopeSummary = addCardSummary(envelopeSummary, item.summary());
    }
    allItems.sort(cardComparator(sortField, descending));

    int totalItems = allItems.size();
    boolean paginate = paginated(size);
    return new CardReportResponse(
        period,
        pageItems(allItems, page, size, paginate),
        paginate ? page : 0,
        paginate ? size : totalItems,
        totalItems,
        totalPages(totalItems, size, paginate),
        envelopeSummary);
  }

  @Transactional(readOnly = true)
  public InvoiceReportResponse getInvoice(
      AuthenticatedUser authenticatedUser,
      UUID invoiceId,
      ResponsibleType responsibleType,
      String responsibleName) {
    UUID userId = authenticatedUser.userId();
    CreditCardInvoice invoice =
        creditCardInvoiceRepository
            .findByIdAndUserId(invoiceId, userId)
            .orElseThrow(() -> new NotFoundException("Fatura não encontrada."));
    CreditCardInvoiceResponse official = creditCardInvoiceService.toResponse(invoice);
    CreditCard card = invoice.getCreditCard();

    List<ExpenseInstallment> invoiceInstallments =
        expenseInstallmentRepository.findAllByInvoice_IdAndUserIdOrderByDueDateAscIdAsc(
            invoice.getId(), userId);
    List<ExpenseInstallment> filteredInstallments =
        invoiceInstallments.stream()
            .filter(
                installment ->
                    matchesResponsible(installment.getExpense(), responsibleType, responsibleName))
            .toList();

    List<InvoiceReportPurchaseResponse> purchases = toInvoicePurchases(filteredInstallments);
    List<InvoiceReportCategoryGroupResponse> byCategory = toInvoiceCategories(purchases);
    List<InvoiceReportResponsibleGroupResponse> byResponsible = toInvoiceResponsibles(purchases);

    List<UUID> filteredInstallmentIds =
        filteredInstallments.stream().map(ExpenseInstallment::getId).toList();
    List<AdjustmentResponse> installmentAdjustments =
        filteredInstallmentIds.isEmpty()
            ? List.of()
            : adjustmentRepository
                .findAllByInstallment_IdInAndUserIdOrderByCreatedAtAscIdAsc(
                    filteredInstallmentIds, userId)
                .stream()
                .map(AdjustmentResponse::from)
                .toList();

    List<InvoiceAdjustmentResponse> invoiceAdjustments =
        invoiceAdjustmentRepository
            .findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(invoice.getId(), userId)
            .stream()
            .map(InvoiceAdjustmentResponse::from)
            .toList();
    List<CreditCardCreditApplication> applications =
        creditApplicationRepository.findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(
            invoice.getId(), userId);
    List<CardReportCreditApplicationResponse> credits =
        applications.stream()
            .map(
                application ->
                    new CardReportCreditApplicationResponse(
                        application.getId(),
                        application.getCredit().getId(),
                        application.getInvoice().getId(),
                        application.getInstallment().getId(),
                        money(application.getAmount()),
                        application.getCreatedAt()))
            .toList();
    List<InvoicePaymentResponse> payments =
        invoicePaymentRepository
            .findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(invoice.getId(), userId)
            .stream()
            .map(InvoicePaymentResponse::from)
            .toList();
    List<InvoiceReportAllocationResponse> allocations =
        loadInvoiceAllocations(invoice.getId(), userId, applications);

    return new InvoiceReportResponse(
        invoice.getId(),
        new InvoiceReportCardResponse(
            card.getName(), card.getHolderName(), card.getLastFourDigits()),
        new InvoiceReportHeaderResponse(
            official.referenceYear(),
            official.referenceMonth(),
            official.closingDate(),
            official.dueDate(),
            official.status(),
            official.totalAmount(),
            official.paidAmount(),
            official.remainingAmount()),
        purchases,
        byCategory,
        byResponsible,
        installmentAdjustments,
        invoiceAdjustments,
        credits,
        payments,
        allocations);
  }

  @Transactional(readOnly = true)
  public CashFlowResponse listCashFlow(
      AuthenticatedUser authenticatedUser,
      String flowType,
      LocalDate startDate,
      LocalDate endDate,
      UUID accountId,
      String sort,
      String direction,
      int page,
      int size) {
    validatePagination(page, size);
    CashFlowFlowType parsedFlowType = parseCashFlowFlowType(flowType);
    CashFlowSortField sortField = parseCashFlowSort(sort);
    boolean descending = parseDescending(direction);
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ReportPeriodResponse period = resolvePeriod(startDate, endDate, today);
    boolean entirelyPast = period.endDate().isBefore(today);
    if (parsedFlowType == CashFlowFlowType.PROJECTED && entirelyPast) {
      throw new InvalidRequestException(INVALID_DATA);
    }

    UUID userId = authenticatedUser.userId();
    boolean unknownAccount = false;
    List<Account> accounts;
    if (accountId != null) {
      Optional<Account> owned = accountRepository.findByIdAndUserId(accountId, userId);
      if (owned.isEmpty()) {
        unknownAccount = true;
        accounts = List.of();
      } else {
        accounts = List.of(owned.get());
      }
    } else {
      accounts = accountRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
    }

    boolean includeHistorical = parsedFlowType != CashFlowFlowType.PROJECTED;
    boolean includeProjected = parsedFlowType != CashFlowFlowType.HISTORICAL;
    boolean historicalIntervalEmpty = period.startDate().isAfter(today);
    LocalDate historicalEnd =
        historicalIntervalEmpty ? null : period.endDate().isAfter(today) ? today : period.endDate();

    CashFlowHistoricalResponse historical = null;
    if (includeHistorical) {
      historical =
          buildHistoricalCashFlow(
              userId,
              accounts,
              accountId,
              unknownAccount,
              historicalIntervalEmpty,
              period.startDate(),
              historicalEnd,
              sortField,
              descending,
              page,
              size,
              paginated(size));
    }

    CashFlowProjectedResponse projected = null;
    if (includeProjected) {
      if (entirelyPast || unknownAccount) {
        projected = CashFlowProjectedResponse.emptyProjected();
      } else {
        ProjectionResponse projection =
            projectionService.project(
                authenticatedUser,
                period.startDate(),
                period.endDate(),
                null,
                null,
                null,
                accountId,
                0,
                20);
        projected = CashFlowProjectedResponse.from(projection);
      }
    }

    return new CashFlowResponse(period, parsedFlowType, accountId, historical, projected);
  }

  @Transactional(readOnly = true)
  public ReportPdf expensesPdf(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      ExpenseStatus status,
      UUID categoryId,
      UUID accountId,
      UUID creditCardId,
      ResponsibleType responsibleType,
      String responsibleName,
      PaymentMethod paymentMethod,
      String sort,
      String direction) {
    ExpenseReportResponse report =
        listExpenses(
            authenticatedUser,
            startDate,
            endDate,
            status,
            categoryId,
            accountId,
            creditCardId,
            responsibleType,
            responsibleName,
            paymentMethod,
            sort,
            direction,
            0,
            PDF_UNIVERSE,
            false);
    return new ReportPdf(
        reportsPdfRenderer.expenses(report, Instant.now(clock)),
        periodFilename("despesas", report.period()));
  }

  @Transactional(readOnly = true)
  public ReportPdf incomesPdf(
      AuthenticatedUser authenticatedUser,
      String dateType,
      LocalDate startDate,
      LocalDate endDate,
      String status,
      UUID categoryId,
      UUID accountId,
      ResponsibleType responsibleType,
      String responsibleName,
      String sort,
      String direction) {
    IncomeReportResponse report =
        listIncomes(
            authenticatedUser,
            dateType,
            startDate,
            endDate,
            status,
            categoryId,
            accountId,
            responsibleType,
            responsibleName,
            sort,
            direction,
            0,
            PDF_UNIVERSE);
    return new ReportPdf(
        reportsPdfRenderer.incomes(report, Instant.now(clock)),
        periodFilename("receitas", report.period()));
  }

  @Transactional(readOnly = true)
  public ReportPdf categoriesPdf(
      AuthenticatedUser authenticatedUser,
      String dateType,
      LocalDate startDate,
      LocalDate endDate,
      String sort,
      String direction) {
    CategoryReportResponse report =
        listCategories(
            authenticatedUser, dateType, startDate, endDate, sort, direction, 0, PDF_UNIVERSE);
    return new ReportPdf(
        reportsPdfRenderer.categories(report, Instant.now(clock)),
        periodFilename("categorias", report.period()));
  }

  @Transactional(readOnly = true)
  public ReportPdf responsiblesPdf(
      AuthenticatedUser authenticatedUser,
      String nature,
      String dateType,
      LocalDate startDate,
      LocalDate endDate,
      String sort,
      String direction) {
    ResponsibleReportResponse report =
        listResponsibles(
            authenticatedUser,
            nature,
            dateType,
            startDate,
            endDate,
            sort,
            direction,
            0,
            PDF_UNIVERSE);
    return new ReportPdf(
        reportsPdfRenderer.responsibles(report, Instant.now(clock)),
        periodFilename("responsaveis", report.period()));
  }

  @Transactional(readOnly = true)
  public ReportPdf cardsPdf(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      UUID creditCardId,
      String sort,
      String direction) {
    CardReportResponse report =
        listCards(
            authenticatedUser, startDate, endDate, creditCardId, sort, direction, 0, PDF_UNIVERSE);
    return new ReportPdf(
        reportsPdfRenderer.cards(report, Instant.now(clock)),
        periodFilename("cartoes", report.period()));
  }

  @Transactional(readOnly = true)
  public ReportPdf cashFlowPdf(
      AuthenticatedUser authenticatedUser,
      String flowType,
      LocalDate startDate,
      LocalDate endDate,
      UUID accountId,
      String sort,
      String direction) {
    CashFlowResponse report =
        listCashFlow(
            authenticatedUser,
            flowType,
            startDate,
            endDate,
            accountId,
            sort,
            direction,
            0,
            PDF_UNIVERSE);
    return new ReportPdf(
        reportsPdfRenderer.cashFlow(report, Instant.now(clock)),
        periodFilename("fluxo-caixa", report.period()));
  }

  @Transactional(readOnly = true)
  public ReportPdf invoicePdf(
      AuthenticatedUser authenticatedUser,
      UUID invoiceId,
      ResponsibleType responsibleType,
      String responsibleName) {
    InvoiceReportResponse report =
        getInvoice(authenticatedUser, invoiceId, responsibleType, responsibleName);
    return new ReportPdf(
        reportsPdfRenderer.invoice(report, Instant.now(clock)),
        "relatorio-fatura-"
            + report.invoice().referenceYear()
            + "-"
            + report.invoice().referenceMonth()
            + ".pdf");
  }

  private static String periodFilename(String resource, ReportPeriodResponse period) {
    return "relatorio-" + resource + "-" + period.startDate() + "_" + period.endDate() + ".pdf";
  }

  private CashFlowHistoricalResponse buildHistoricalCashFlow(
      UUID userId,
      List<Account> accounts,
      UUID accountId,
      boolean unknownAccount,
      boolean historicalIntervalEmpty,
      LocalDate historicalStart,
      LocalDate historicalEnd,
      CashFlowSortField sortField,
      boolean descending,
      int page,
      int size,
      boolean paginate) {
    if (unknownAccount || historicalIntervalEmpty) {
      return new CashFlowHistoricalResponse(
          null, null, List.of(), paginate ? page : 0, paginate ? size : 0, 0, 0, null);
    }

    List<CashFlowItemResponse> lines =
        loadHistoricalCashFlowLines(userId, accountId, historicalStart, historicalEnd);
    lines.sort(cashFlowComparator(sortField, descending));

    BigDecimal totalIn = money(BigDecimal.ZERO);
    BigDecimal totalOut = money(BigDecimal.ZERO);
    BigDecimal net = money(BigDecimal.ZERO);
    for (CashFlowItemResponse line : lines) {
      BigDecimal amount = line.amount();
      net = money(net.add(amount));
      if (amount.signum() > 0) {
        totalIn = money(totalIn.add(amount));
      } else if (amount.signum() < 0) {
        totalOut = money(totalOut.add(amount.abs()));
      }
    }

    BigDecimal opening = money(BigDecimal.ZERO);
    BigDecimal closing = money(BigDecimal.ZERO);
    LocalDate openingAsOf = historicalStart.minusDays(1);
    for (Account account : accounts) {
      opening = money(opening.add(accountService.calculateBalanceAsOf(account, openingAsOf)));
      closing = money(closing.add(accountService.calculateBalanceAsOf(account, historicalEnd)));
    }

    int totalItems = lines.size();
    return new CashFlowHistoricalResponse(
        opening,
        closing,
        pageItems(lines, page, size, paginate),
        paginate ? page : 0,
        paginate ? size : totalItems,
        totalItems,
        totalPages(totalItems, size, paginate),
        new CashFlowSummaryResponse(totalIn, totalOut, net));
  }

  private List<CashFlowItemResponse> loadHistoricalCashFlowLines(
      UUID userId, UUID accountId, LocalDate historicalStart, LocalDate historicalEnd) {
    List<CashFlowItemResponse> lines = new ArrayList<>();

    for (IncomeMovement receipt :
        incomeMovementRepository.findActiveReceiptsByUserIdAndMovementDateBetween(
            userId, historicalStart, historicalEnd)) {
      if (receipt.getAccount() == null
          || !matchesCashFlowAccount(accountId, receipt.getAccount().getId())) {
        continue;
      }
      lines.add(
          new CashFlowItemResponse(
              receipt.getId(),
              CashFlowType.INCOME_RECEIPT,
              receipt.getMovementDate(),
              money(receipt.getAmount()),
              receipt.getAccount().getId(),
              receipt.getIncome().getDescription()));
    }

    for (Payment payment :
        paymentRepository.findActiveValidByUserIdAndPaymentDateBetween(
            userId, historicalStart, historicalEnd)) {
      if (!matchesCashFlowAccount(accountId, payment.getAccount().getId())) {
        continue;
      }
      lines.add(
          new CashFlowItemResponse(
              payment.getId(),
              CashFlowType.EXPENSE_PAYMENT,
              payment.getPaymentDate(),
              money(payment.getAmount().negate()),
              payment.getAccount().getId(),
              payment.getExpense().getDescription()));
    }

    for (CreditCardInvoicePayment payment :
        invoicePaymentRepository.findAllByUserIdAndPaymentDateBetween(
            userId, historicalStart, historicalEnd, null)) {
      if (payment.getStatus() != InvoicePaymentStatus.ACTIVE
          || !matchesCashFlowAccount(accountId, payment.getAccount().getId())) {
        continue;
      }
      String description = payment.getNotes();
      if (description == null || description.isBlank()) {
        description = payment.getInvoice().getCreditCard().getName();
      }
      lines.add(
          new CashFlowItemResponse(
              payment.getId(),
              CashFlowType.INVOICE_PAYMENT,
              payment.getPaymentDate(),
              money(payment.getAmount().negate()),
              payment.getAccount().getId(),
              description));
    }

    for (CardPurchaseAccountRefund refund :
        cardPurchaseAccountRefundRepository.findAllByUserIdAndCreatedAtBetween(
            userId, startOfFinancialDay(historicalStart), endOfFinancialDay(historicalEnd))) {
      LocalDate date = refund.getCreatedAt().atZone(FINANCIAL_ZONE).toLocalDate();
      if (date.isBefore(historicalStart)
          || date.isAfter(historicalEnd)
          || !matchesCashFlowAccount(accountId, refund.getAccount().getId())) {
        continue;
      }
      lines.add(
          new CashFlowItemResponse(
              refund.getId(),
              CashFlowType.CARD_PURCHASE_REFUND,
              date,
              money(refund.getAmount()),
              refund.getAccount().getId(),
              refund.getExpense().getDescription()));
    }

    for (Transfer transfer :
        transferRepository.searchByUser(userId, historicalStart, historicalEnd, accountId)) {
      if (transfer.getStatus() != TransferStatus.ACTIVE) {
        continue;
      }
      UUID sourceId = transfer.getSourceAccount().getId();
      UUID destinationId = transfer.getDestinationAccount().getId();
      if (matchesCashFlowAccount(accountId, sourceId)) {
        lines.add(
            new CashFlowItemResponse(
                transfer.getId(),
                CashFlowType.TRANSFER_OUT,
                transfer.getTransferDate(),
                money(transfer.getAmount().negate()),
                sourceId,
                transfer.getDescription()));
      }
      if (matchesCashFlowAccount(accountId, destinationId)) {
        lines.add(
            new CashFlowItemResponse(
                transfer.getId(),
                CashFlowType.TRANSFER_IN,
                transfer.getTransferDate(),
                money(transfer.getAmount()),
                destinationId,
                transfer.getDescription()));
      }
    }

    for (AccountBalanceAdjustment adjustment :
        balanceAdjustmentRepository.findActiveByUserIdAndAdjustmentDateBetween(
            userId, historicalStart, historicalEnd)) {
      if (!matchesCashFlowAccount(accountId, adjustment.getAccount().getId())) {
        continue;
      }
      lines.add(
          new CashFlowItemResponse(
              adjustment.getId(),
              CashFlowType.BALANCE_ADJUSTMENT,
              adjustment.getAdjustmentDate(),
              money(adjustment.getAdjustmentAmount()),
              adjustment.getAccount().getId(),
              null));
    }
    return lines;
  }

  private static boolean matchesCashFlowAccount(UUID filterAccountId, UUID lineAccountId) {
    return filterAccountId == null || filterAccountId.equals(lineAccountId);
  }

  private Instant startOfFinancialDay(LocalDate date) {
    return date.atStartOfDay(FINANCIAL_ZONE).toInstant();
  }

  private Instant endOfFinancialDay(LocalDate date) {
    return date.atTime(LocalTime.MAX).atZone(FINANCIAL_ZONE).toInstant();
  }

  private static Comparator<CashFlowItemResponse> cashFlowComparator(
      CashFlowSortField sortField, boolean descending) {
    Comparator<CashFlowItemResponse> primary =
        switch (sortField) {
          case DATE -> Comparator.comparing(CashFlowItemResponse::date);
          case AMOUNT -> Comparator.comparing(CashFlowItemResponse::amount);
          case TYPE -> Comparator.comparing(item -> item.type().name());
        };
    if (descending) {
      primary = primary.reversed();
    }
    return primary
        .thenComparing(CashFlowItemResponse::id)
        .thenComparing(item -> item.type().name())
        .thenComparing(CashFlowItemResponse::accountId);
  }

  private LoadedIncomes loadIncomes(
      UUID userId,
      ReportPeriodResponse period,
      ReportDateType parsedDateType,
      IncomeStatus parsedStatus,
      UUID categoryId,
      UUID accountId,
      ResponsibleType responsibleType,
      String responsibleName) {
    List<Income> incomes;
    Map<UUID, BigDecimal> periodReceivedByIncome = new LinkedHashMap<>();
    if (parsedDateType == ReportDateType.EXPECTED) {
      incomes =
          incomeRepository.findAllByUserIdAndExpectedDateBetween(
              userId, period.startDate(), period.endDate());
    } else {
      List<IncomeMovement> receipts =
          incomeMovementRepository.findActiveReceiptsByUserIdAndMovementDateBetween(
              userId, period.startDate(), period.endDate());
      Map<UUID, Income> byId = new LinkedHashMap<>();
      for (IncomeMovement receipt : receipts) {
        Income income = receipt.getIncome();
        byId.putIfAbsent(income.getId(), income);
        periodReceivedByIncome.merge(income.getId(), money(receipt.getAmount()), BigDecimal::add);
      }
      incomes = new ArrayList<>(byId.values());
    }
    incomes =
        incomes.stream()
            .filter(
                income ->
                    matchesIncomeFilters(
                        income, parsedStatus, categoryId, responsibleType, responsibleName))
            .toList();
    incomes = applyAccountFilter(userId, accountId, incomes);
    return new LoadedIncomes(incomes, periodReceivedByIncome, loadIncomeTotals(userId, incomes));
  }

  private Set<UUID> agreementExpenseIds(UUID userId, Set<UUID> expenseIds) {
    if (expenseIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        agreementRepository.findExpenseIdsByUserIdAndExpenseIdIn(userId, expenseIds));
  }

  private InstallmentAmounts amountsOf(ExpenseInstallment installment) {
    UUID installmentId = installment.getId();
    UUID userId = installment.getUserId();
    BigDecimal original = money(installment.getAmount());
    BigDecimal discount =
        money(
            adjustmentRepository.sumActiveDiscountAmountByInstallmentIdAndUserId(
                installmentId, userId));
    BigDecimal surcharge =
        money(
            adjustmentRepository.sumActiveSurchargeAmountByInstallmentIdAndUserId(
                installmentId, userId));
    BigDecimal obligation = money(installmentBalanceService.obligation(installment));
    BigDecimal remaining = money(installmentBalanceService.remaining(installment));
    BigDecimal paid;
    if (installment.getExpense().getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
      paid = money(obligation.subtract(remaining));
    } else {
      paid =
          money(paymentRepository.sumActiveAmountByInstallmentIdAndUserId(installmentId, userId));
    }
    return new InstallmentAmounts(original, discount, surcharge, obligation, paid, remaining);
  }

  private static boolean countsTowardSummary(Expense expense, ExpenseInstallment installment) {
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      return false;
    }
    CreditCardInvoice invoice = installment.getInvoice();
    return invoice == null || invoice.getStatus() != CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT;
  }

  private static boolean matchesExpenseFilters(
      Expense expense,
      ExpenseStatus status,
      UUID categoryId,
      UUID accountId,
      UUID creditCardId,
      ResponsibleType responsibleType,
      String responsibleName,
      PaymentMethod paymentMethod) {
    if (status != null && expense.getStatus() != status) {
      return false;
    }
    if (categoryId != null && !categoryId.equals(expense.getCategory().getId())) {
      return false;
    }
    if (accountId != null
        && (expense.getAccount() == null || !accountId.equals(expense.getAccount().getId()))) {
      return false;
    }
    if (creditCardId != null
        && (expense.getCreditCard() == null
            || !creditCardId.equals(expense.getCreditCard().getId()))) {
      return false;
    }
    if (responsibleType != null && expense.getResponsibleType() != responsibleType) {
      return false;
    }
    if (responsibleName != null && !Objects.equals(responsibleName, expense.getResponsibleName())) {
      return false;
    }
    return paymentMethod == null || expense.getPaymentMethod() == paymentMethod;
  }

  private static boolean matchesResponsible(
      Expense expense, ResponsibleType responsibleType, String responsibleName) {
    if (responsibleType != null && expense.getResponsibleType() != responsibleType) {
      return false;
    }
    return responsibleName == null || Objects.equals(responsibleName, expense.getResponsibleName());
  }

  private List<InvoiceReportPurchaseResponse> toInvoicePurchases(
      List<ExpenseInstallment> installments) {
    Map<UUID, List<ExpenseInstallment>> grouped = new LinkedHashMap<>();
    for (ExpenseInstallment installment : installments) {
      grouped
          .computeIfAbsent(installment.getExpense().getId(), unused -> new ArrayList<>())
          .add(installment);
    }
    List<InvoiceReportPurchaseResponse> purchases = new ArrayList<>();
    for (List<ExpenseInstallment> recorte : grouped.values()) {
      recorte.sort(
          Comparator.comparingInt(ExpenseInstallment::getInstallmentNumber)
              .thenComparing(ExpenseInstallment::getId));
      Expense expense = recorte.getFirst().getExpense();
      BigDecimal discount = money(BigDecimal.ZERO);
      BigDecimal surcharge = money(BigDecimal.ZERO);
      for (ExpenseInstallment installment : recorte) {
        InstallmentAmounts amounts = amountsOf(installment);
        discount = money(discount.add(amounts.discount()));
        surcharge = money(surcharge.add(amounts.surcharge()));
      }
      ExpenseInstallment first = recorte.getFirst();
      purchases.add(
          new InvoiceReportPurchaseResponse(
              expense.getId(),
              expense.getDescription(),
              expense.getExpenseDate(),
              money(expense.getTotalAmount()),
              expense.getCategory().getName(),
              expense.getResponsibleType(),
              expense.getResponsibleName(),
              first.getInstallmentNumber(),
              first.getTotalInstallments(),
              discount,
              surcharge));
    }
    purchases.sort(
        Comparator.comparing(InvoiceReportPurchaseResponse::expenseDate)
            .thenComparing(InvoiceReportPurchaseResponse::expenseId));
    return purchases;
  }

  private List<InvoiceReportCategoryGroupResponse> toInvoiceCategories(
      List<InvoiceReportPurchaseResponse> purchases) {
    Map<String, BigDecimal> totals = new LinkedHashMap<>();
    for (InvoiceReportPurchaseResponse purchase : purchases) {
      totals.merge(purchase.categoryName(), purchase.original(), BigDecimal::add);
    }
    return totals.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(
            entry ->
                new InvoiceReportCategoryGroupResponse(entry.getKey(), money(entry.getValue())))
        .toList();
  }

  private List<InvoiceReportResponsibleGroupResponse> toInvoiceResponsibles(
      List<InvoiceReportPurchaseResponse> purchases) {
    Map<String, InvoiceReportResponsibleGroupResponse> grouped = new LinkedHashMap<>();
    for (InvoiceReportPurchaseResponse purchase : purchases) {
      ResponsibleGroup group = groupOf(purchase.responsibleType(), purchase.responsibleName());
      grouped.merge(
          group.key(),
          new InvoiceReportResponsibleGroupResponse(
              group.responsibleType(), group.responsibleName(), purchase.original()),
          (left, right) ->
              new InvoiceReportResponsibleGroupResponse(
                  left.responsibleType(),
                  left.responsibleName(),
                  money(left.original().add(right.original()))));
    }
    return grouped.values().stream()
        .sorted(
            Comparator.comparing(
                    InvoiceReportResponsibleGroupResponse::responsibleType,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                    InvoiceReportResponsibleGroupResponse::responsibleName,
                    Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  private List<InvoiceReportAllocationResponse> loadInvoiceAllocations(
      UUID invoiceId, UUID userId, List<CreditCardCreditApplication> applications) {
    List<InvoiceReportAllocationResponse> allocations = new ArrayList<>();
    for (CreditCardInvoicePaymentAllocation allocation :
        paymentAllocationRepository.findAllByInvoice_IdAndUserId(invoiceId, userId)) {
      allocations.add(
          new InvoiceReportAllocationResponse(
              allocation.getId(),
              InvoiceReportAllocationType.PAYMENT,
              allocation.getInvoicePayment().getId(),
              allocation.getInstallment().getId(),
              money(allocation.getAmount()),
              allocation.getCreatedAt()));
    }
    for (CreditCardInvoiceAdjustmentAllocation allocation :
        invoiceAdjustmentAllocationRepository.findAllByInvoice_IdAndUserId(invoiceId, userId)) {
      allocations.add(
          new InvoiceReportAllocationResponse(
              allocation.getId(),
              InvoiceReportAllocationType.INVOICE_ADJUSTMENT,
              allocation.getInvoiceAdjustment().getId(),
              allocation.getInstallment().getId(),
              money(allocation.getAmount()),
              allocation.getCreatedAt()));
    }
    for (CreditCardCreditApplication application : applications) {
      allocations.add(
          new InvoiceReportAllocationResponse(
              application.getId(),
              InvoiceReportAllocationType.CREDIT,
              application.getCredit().getId(),
              application.getInstallment().getId(),
              money(application.getAmount()),
              application.getCreatedAt()));
    }
    for (CreditCardInvoiceAgreementSettlementAllocation allocation :
        settlementAllocationRepository.findAllByInvoice_IdAndUserId(invoiceId, userId)) {
      allocations.add(
          new InvoiceReportAllocationResponse(
              allocation.getId(),
              InvoiceReportAllocationType.SETTLEMENT,
              allocation.getSettlement().getId(),
              allocation.getInstallment().getId(),
              money(allocation.getAmount()),
              allocation.getCreatedAt()));
    }
    allocations.sort(
        Comparator.comparing(InvoiceReportAllocationResponse::createdAt)
            .thenComparing(InvoiceReportAllocationResponse::id));
    return allocations;
  }

  private List<Income> applyAccountFilter(UUID userId, UUID accountId, List<Income> incomes) {
    if (accountId == null || incomes.isEmpty()) {
      return incomes;
    }
    Set<UUID> matching =
        new HashSet<>(
            incomeMovementRepository.findIncomeIdsWithActiveReceiptOnAccount(
                userId, accountId, incomes.stream().map(Income::getId).toList()));
    return incomes.stream().filter(income -> matching.contains(income.getId())).toList();
  }

  private static boolean matchesIncomeFilters(
      Income income,
      IncomeStatus status,
      UUID categoryId,
      ResponsibleType responsibleType,
      String responsibleName) {
    if (status != null && income.getStatus() != status) {
      return false;
    }
    if (categoryId != null && !categoryId.equals(income.getCategory().getId())) {
      return false;
    }
    if (responsibleType != null && income.getResponsibleType() != responsibleType) {
      return false;
    }
    return responsibleName == null || Objects.equals(responsibleName, income.getResponsibleName());
  }

  private Map<UUID, IncomeTotals> loadIncomeTotals(UUID userId, List<Income> incomes) {
    if (incomes.isEmpty()) {
      return Map.of();
    }
    Map<UUID, IncomeTotals> totals = new HashMap<>();
    for (Object[] row :
        incomeMovementRepository.sumActiveAmountsByIncomeIdsAndUserId(
            userId, incomes.stream().map(Income::getId).toList())) {
      UUID incomeId = (UUID) row[0];
      IncomeMovementType type = (IncomeMovementType) row[1];
      BigDecimal amount = money(row[2]);
      IncomeTotals current = totals.getOrDefault(incomeId, IncomeTotals.ZERO);
      if (type == IncomeMovementType.ACCRUAL) {
        totals.put(incomeId, new IncomeTotals(current.amount, amount, current.received));
      } else if (type == IncomeMovementType.RECEIPT) {
        totals.put(incomeId, new IncomeTotals(current.amount, current.accrued, amount));
      }
    }
    return totals;
  }

  private static IncomeReportItemResponse toIncomeItem(
      Income income, IncomeTotals totals, BigDecimal periodReceivedAmount) {
    BigDecimal amount = money(income.getAmount());
    return new IncomeReportItemResponse(
        income.getId(),
        income.getDescription(),
        income.getStatus(),
        income.getCategory().getId(),
        income.getResponsibleType(),
        income.getResponsibleName(),
        income.getExpectedDate(),
        amount,
        totals.accrued,
        totals.received,
        money(amount.add(totals.accrued).subtract(totals.received)),
        periodReceivedAmount);
  }

  private static ExpenseReportItemResponse toItemResponse(
      Expense expense,
      ExpenseReportOrigin origin,
      PeriodTotals totals,
      List<ExpenseReportInstallmentResponse> installments) {
    return new ExpenseReportItemResponse(
        expense.getId(),
        expense.getDescription(),
        expense.getExpenseDate(),
        expense.getPaymentMethod(),
        expense.getStatus(),
        expense.getCategory().getId(),
        expense.getAccount() == null ? null : expense.getAccount().getId(),
        expense.getCreditCard() == null ? null : expense.getCreditCard().getId(),
        expense.getResponsibleType(),
        expense.getResponsibleName(),
        origin,
        totals.original,
        totals.discount,
        totals.surcharge,
        totals.obligation,
        totals.paid,
        totals.remaining,
        List.copyOf(installments));
  }

  private static ExpenseReportInstallmentResponse toInstallmentResponse(
      ExpenseInstallment installment, InstallmentAmounts amounts) {
    return new ExpenseReportInstallmentResponse(
        installment.getId(),
        installment.getInstallmentNumber(),
        installment.getTotalInstallments(),
        installment.getDueDate(),
        amounts.original,
        amounts.discount,
        amounts.surcharge,
        amounts.obligation,
        amounts.paid,
        amounts.remaining,
        installment.getStatus());
  }

  private static void validatePagination(int page, int size) {
    if (!paginated(size)) {
      return;
    }
    if (page < 0) {
      throw new BusinessRuleException(INVALID_PAGE);
    }
    if (size < 1) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE);
    }
    if (size > MAX_PAGE_SIZE) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE_MAX);
    }
  }

  private static boolean paginated(int size) {
    return size != Integer.MAX_VALUE;
  }

  private static int totalPages(int totalItems, int size, boolean paginate) {
    if (!paginate) {
      return totalItems == 0 ? 0 : 1;
    }
    return totalItems == 0 ? 0 : (int) Math.ceil(totalItems / (double) size);
  }

  private static <T> List<T> pageItems(List<T> allItems, int page, int size, boolean paginate) {
    if (!paginate) {
      return allItems;
    }
    int totalItems = allItems.size();
    int from = page * size;
    if (from >= totalItems) {
      return List.of();
    }
    return List.copyOf(allItems.subList(from, Math.min(from + size, totalItems)));
  }

  private static ReportPeriodResponse resolvePeriod(
      LocalDate startDate, LocalDate endDate, LocalDate today) {
    if (startDate == null && endDate == null) {
      YearMonth current = YearMonth.from(today);
      return new ReportPeriodResponse(current.atDay(1), current.atEndOfMonth());
    }
    if (startDate == null || endDate == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    if (startDate.isAfter(endDate)) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    YearMonth startMonth = YearMonth.from(startDate);
    YearMonth endMonth = YearMonth.from(endDate);
    if (ChronoUnit.MONTHS.between(startMonth, endMonth) + 1 > MAX_MONTHS) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    return new ReportPeriodResponse(startDate, endDate);
  }

  private static ExpenseReportSortField parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return ExpenseReportSortField.DUE_DATE;
    }
    ExpenseReportSortField field = ExpenseReportSortField.fromQuery(sort);
    if (field == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    return field;
  }

  private static CardReportSortField parseCardSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return CardReportSortField.NAME;
    }
    CardReportSortField field = CardReportSortField.fromQuery(sort);
    if (field == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    return field;
  }

  private static boolean parseDescending(String direction) {
    if (direction == null || direction.isBlank()) {
      return false;
    }
    if ("asc".equalsIgnoreCase(direction)) {
      return false;
    }
    if ("desc".equalsIgnoreCase(direction)) {
      return true;
    }
    throw new InvalidRequestException(INVALID_DATA);
  }

  private static CashFlowFlowType parseCashFlowFlowType(String flowType) {
    if (flowType == null || flowType.isBlank()) {
      return CashFlowFlowType.BOTH;
    }
    try {
      return CashFlowFlowType.valueOf(flowType.trim());
    } catch (IllegalArgumentException exception) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static CashFlowSortField parseCashFlowSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return CashFlowSortField.DATE;
    }
    CashFlowSortField field = CashFlowSortField.fromQuery(sort);
    if (field == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    return field;
  }

  private static ReportDateType parseDateType(String dateType) {
    if (dateType == null || dateType.isBlank()) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    try {
      return ReportDateType.valueOf(dateType.trim());
    } catch (IllegalArgumentException exception) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static IncomeStatus parseIncomeStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    try {
      return IncomeStatus.valueOf(status.trim());
    } catch (IllegalArgumentException exception) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static IncomeReportSortField parseIncomeSort(String sort, ReportDateType dateType) {
    if (sort == null || sort.isBlank()) {
      return dateType == ReportDateType.RECEIVED
          ? IncomeReportSortField.CREATED_AT
          : IncomeReportSortField.EXPECTED_DATE;
    }
    IncomeReportSortField field = IncomeReportSortField.fromQuery(sort);
    if (field == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    return field;
  }

  private static CategoryReportSortField parseCategorySort(String sort) {
    if (sort == null || sort.isBlank()) {
      return CategoryReportSortField.NAME;
    }
    CategoryReportSortField field = CategoryReportSortField.fromQuery(sort);
    if (field == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    return field;
  }

  private static ReportNature parseNature(String nature) {
    if (nature == null || nature.isBlank()) {
      return ReportNature.BOTH;
    }
    try {
      return ReportNature.valueOf(nature.trim());
    } catch (IllegalArgumentException exception) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static ReportDateType parseResponsibleDateType(ReportNature nature, String dateType) {
    if (nature == ReportNature.EXPENSE) {
      if (dateType != null) {
        throw new InvalidRequestException(INVALID_DATA);
      }
      return null;
    }
    return parseDateType(dateType);
  }

  private static ResponsibleReportSortField parseResponsibleSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return ResponsibleReportSortField.RESPONSIBLE_TYPE;
    }
    ResponsibleReportSortField field = ResponsibleReportSortField.fromQuery(sort);
    if (field == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    return field;
  }

  private static ResponsibleGroup groupOf(ResponsibleType type, String name) {
    boolean unnamedOther = type == ResponsibleType.OTHER && (name == null || name.isBlank());
    if (type == null || unnamedOther) {
      return new ResponsibleGroup("UNASSIGNED", null, null);
    }
    String persistedName = name == null || name.isBlank() ? null : name;
    String key = persistedName == null ? type.name() : type.name() + "/" + persistedName;
    return new ResponsibleGroup(key, type, persistedName);
  }

  private static ResponsibleBucket bucketOf(
      Map<String, ResponsibleBucket> buckets, ResponsibleGroup group) {
    return buckets.computeIfAbsent(
        group.key(),
        unused ->
            new ResponsibleBucket(group.key(), group.responsibleType(), group.responsibleName()));
  }

  private static ResponsibleReportItemResponse toResponsibleItem(
      ResponsibleBucket bucket,
      ReportDateType dateType,
      boolean includeExpense,
      boolean includeIncome) {
    ExpenseReportSummaryResponse expense =
        includeExpense && bucket.hasExpense ? bucket.expenseTotals.toSummary() : null;
    IncomeReportSummaryResponse income = null;
    if (includeIncome && bucket.hasIncome) {
      income =
          dateType == ReportDateType.RECEIVED
              ? IncomeReportSummaryResponse.received(money(bucket.periodReceived))
              : IncomeReportSummaryResponse.expected(
                  bucket.incomeTotals.amount,
                  bucket.incomeTotals.accrued,
                  bucket.incomeTotals.received,
                  bucket.incomeTotals.remaining());
    }
    return new ResponsibleReportItemResponse(
        bucket.key, bucket.responsibleType, bucket.responsibleName, expense, income);
  }

  private static Comparator<ResponsibleReportItemResponse> responsibleComparator(
      ResponsibleReportSortField sortField, boolean descending) {
    Comparator<String> names = Comparator.nullsLast(Comparator.naturalOrder());
    Comparator<ResponsibleReportItemResponse> byType =
        Comparator.comparing(
            item -> item.responsibleType() == null ? null : item.responsibleType().name(), names);
    Comparator<ResponsibleReportItemResponse> byName =
        Comparator.comparing(ResponsibleReportItemResponse::responsibleName, names);
    Comparator<ResponsibleReportItemResponse> byKey =
        Comparator.comparing(ResponsibleReportItemResponse::key);
    Comparator<ResponsibleReportItemResponse> primary =
        sortField == ResponsibleReportSortField.RESPONSIBLE_NAME ? byName : byType;
    if (descending) {
      primary = primary.reversed();
    }
    if (sortField == ResponsibleReportSortField.RESPONSIBLE_TYPE) {
      return primary.thenComparing(byName).thenComparing(byKey);
    }
    return primary.thenComparing(byKey);
  }

  private static CategoryReportItemResponse toExpenseCategoryItem(CategoryExpenseBucket bucket) {
    PeriodTotals totals = bucket.itemTotals;
    return new CategoryReportItemResponse(
        bucket.categoryId,
        bucket.name,
        CategoryType.EXPENSE,
        bucket.active,
        totals.original,
        totals.discount,
        totals.surcharge,
        totals.obligation,
        totals.paid,
        totals.remaining,
        null,
        null,
        null,
        null,
        null);
  }

  private static CategoryReportItemResponse toIncomeCategoryItem(
      CategoryIncomeBucket bucket, ReportDateType dateType) {
    IncomeTotals totals = bucket.itemTotals;
    return new CategoryReportItemResponse(
        bucket.categoryId,
        bucket.name,
        CategoryType.INCOME,
        bucket.active,
        null,
        null,
        null,
        null,
        null,
        null,
        totals.amount,
        totals.accrued,
        totals.received,
        totals.remaining(),
        dateType == ReportDateType.RECEIVED ? money(bucket.periodReceived) : null);
  }

  private static Comparator<CategoryReportItemResponse> categoryComparator(
      CategoryReportSortField sortField, boolean descending) {
    Comparator<CategoryReportItemResponse> primary =
        switch (sortField) {
          case NAME -> Comparator.comparing(CategoryReportItemResponse::name);
          case TYPE -> Comparator.comparing(item -> item.type().name());
        };
    if (descending) {
      primary = primary.reversed();
    }
    return primary.thenComparing(CategoryReportItemResponse::categoryId);
  }

  private static Comparator<RankedIncomeItem> incomeComparator(
      IncomeReportSortField sortField, boolean descending) {
    Comparator<RankedIncomeItem> primary =
        switch (sortField) {
          case EXPECTED_DATE -> Comparator.comparing(item -> item.item().expectedDate());
          case DESCRIPTION -> Comparator.comparing(item -> item.item().description());
          case AMOUNT -> Comparator.comparing(item -> item.item().amount());
          case STATUS -> Comparator.comparing(item -> item.item().status().name());
          case CREATED_AT -> Comparator.comparing(RankedIncomeItem::createdAt);
          case RECEIVED_AMOUNT -> Comparator.comparing(item -> item.item().receivedAmount());
        };
    if (descending) {
      primary = primary.reversed();
    }
    return primary.thenComparing(item -> item.item().id());
  }

  private static Comparator<RankedItem> comparator(
      ExpenseReportSortField sortField, boolean descending) {
    Comparator<RankedItem> primary =
        switch (sortField) {
          case DUE_DATE -> Comparator.comparing(RankedItem::minDueDate);
          case EXPENSE_DATE -> Comparator.comparing(item -> item.item().expenseDate());
          case DESCRIPTION -> Comparator.comparing(item -> item.item().description());
          case STATUS -> Comparator.comparing(item -> item.item().status().name());
          case CREATED_AT -> Comparator.comparing(RankedItem::createdAt);
          case PERIOD_OBLIGATION -> Comparator.comparing(item -> item.item().periodObligation());
          case PERIOD_REMAINING -> Comparator.comparing(item -> item.item().periodRemaining());
        };
    if (descending) {
      primary = primary.reversed();
    }
    return primary.thenComparing(item -> item.item().id());
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal money(Object value) {
    if (value instanceof BigDecimal amount) {
      return money(amount);
    }
    return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  }

  private record InstallmentAmounts(
      BigDecimal original,
      BigDecimal discount,
      BigDecimal surcharge,
      BigDecimal obligation,
      BigDecimal paid,
      BigDecimal remaining) {

    PeriodTotals toPeriodTotals() {
      return new PeriodTotals(original, discount, surcharge, obligation, paid, remaining);
    }
  }

  private record RankedItem(
      ExpenseReportItemResponse item, LocalDate minDueDate, Instant createdAt) {}

  private record RankedIncomeItem(IncomeReportItemResponse item, Instant createdAt) {}

  private record LoadedIncomes(
      List<Income> incomes,
      Map<UUID, BigDecimal> periodReceivedByIncome,
      Map<UUID, IncomeTotals> totalsByIncome) {}

  private static final class CategoryExpenseBucket {
    private final UUID categoryId;
    private final String name;
    private final boolean active;
    private PeriodTotals itemTotals;

    private CategoryExpenseBucket(
        UUID categoryId, String name, boolean active, PeriodTotals itemTotals) {
      this.categoryId = categoryId;
      this.name = name;
      this.active = active;
      this.itemTotals = itemTotals;
    }
  }

  private static final class CategoryIncomeBucket {
    private final UUID categoryId;
    private final String name;
    private final boolean active;
    private IncomeTotals itemTotals;
    private BigDecimal periodReceived;

    private CategoryIncomeBucket(
        UUID categoryId,
        String name,
        boolean active,
        IncomeTotals itemTotals,
        BigDecimal periodReceived) {
      this.categoryId = categoryId;
      this.name = name;
      this.active = active;
      this.itemTotals = itemTotals;
      this.periodReceived = periodReceived;
    }
  }

  private record ResponsibleGroup(
      String key, ResponsibleType responsibleType, String responsibleName) {}

  private static CardBucket bucketOf(Map<UUID, CardBucket> buckets, CreditCard card) {
    return buckets.computeIfAbsent(card.getId(), unused -> new CardBucket(card));
  }

  private CardReportItemResponse toCardItem(
      CardBucket bucket, Map<UUID, List<ExpenseInstallment>> installmentsByExpense) {
    List<CardReportPurchaseResponse> purchases = new ArrayList<>();
    BigDecimal purchaseAmount = money(BigDecimal.ZERO);
    for (Expense expense : bucket.purchases) {
      List<ExpenseInstallment> nested =
          installmentsByExpense.getOrDefault(expense.getId(), List.of());
      List<CardReportPurchaseInstallmentResponse> installmentItems =
          nested.stream()
              .map(
                  installment ->
                      new CardReportPurchaseInstallmentResponse(
                          installment.getInstallmentNumber(),
                          installment.getDueDate(),
                          money(installment.getAmount())))
              .toList();
      BigDecimal original = money(expense.getTotalAmount());
      purchaseAmount = money(purchaseAmount.add(original));
      int totalInstallments = nested.isEmpty() ? 0 : nested.getFirst().getTotalInstallments();
      purchases.add(
          new CardReportPurchaseResponse(
              expense.getId(),
              expense.getDescription(),
              expense.getExpenseDate(),
              original,
              expense.getResponsibleType(),
              expense.getResponsibleName(),
              expense.getStatus(),
              totalInstallments,
              installmentItems));
    }

    List<CreditCardInvoiceResponse> invoices = new ArrayList<>();
    BigDecimal invoiceAmount = money(BigDecimal.ZERO);
    for (CreditCardInvoice invoice : bucket.invoices) {
      CreditCardInvoiceResponse invoiceResponse = creditCardInvoiceService.toResponse(invoice);
      invoices.add(invoiceResponse);
      invoiceAmount = money(invoiceAmount.add(invoiceResponse.totalAmount()));
    }

    List<InvoicePaymentResponse> payments =
        bucket.payments.stream().map(InvoicePaymentResponse::from).toList();
    BigDecimal paidAmount = money(BigDecimal.ZERO);
    for (CreditCardInvoicePayment payment : bucket.payments) {
      if (payment.getStatus() == InvoicePaymentStatus.ACTIVE) {
        paidAmount = money(paidAmount.add(payment.getAmount()));
      }
    }

    List<CardReportCreditApplicationResponse> credits = new ArrayList<>();
    BigDecimal creditAmount = money(BigDecimal.ZERO);
    for (CreditCardCreditApplication application : bucket.credits) {
      creditAmount = money(creditAmount.add(application.getAmount()));
      credits.add(
          new CardReportCreditApplicationResponse(
              application.getId(),
              application.getCredit().getId(),
              application.getInvoice().getId(),
              application.getInstallment().getId(),
              money(application.getAmount()),
              application.getCreatedAt()));
    }

    List<AdjustmentResponse> installmentAdjustments =
        bucket.installmentAdjustments.stream().map(AdjustmentResponse::from).toList();
    List<InvoiceAdjustmentResponse> invoiceAdjustments =
        bucket.invoiceAdjustments.stream().map(InvoiceAdjustmentResponse::from).toList();

    CardReportSummaryResponse summary =
        new CardReportSummaryResponse(purchaseAmount, invoiceAmount, paidAmount, creditAmount);
    return new CardReportItemResponse(
        bucket.card.getId(),
        bucket.card.getName(),
        bucket.card.getHolderName(),
        bucket.card.getLastFourDigits(),
        bucket.card.isActive(),
        summary,
        purchases,
        invoices,
        payments,
        credits,
        installmentAdjustments,
        invoiceAdjustments);
  }

  private static CardReportSummaryResponse emptyCardSummary() {
    BigDecimal zero = money(BigDecimal.ZERO);
    return new CardReportSummaryResponse(zero, zero, zero, zero);
  }

  private static CardReportSummaryResponse addCardSummary(
      CardReportSummaryResponse left, CardReportSummaryResponse right) {
    return new CardReportSummaryResponse(
        money(left.purchaseAmount().add(right.purchaseAmount())),
        money(left.invoiceAmount().add(right.invoiceAmount())),
        money(left.paidAmount().add(right.paidAmount())),
        money(left.creditAmount().add(right.creditAmount())));
  }

  private static Comparator<CardReportItemResponse> cardComparator(
      CardReportSortField sortField, boolean descending) {
    Comparator<CardReportItemResponse> primary =
        switch (sortField) {
          case NAME -> Comparator.comparing(CardReportItemResponse::name);
          case HOLDER_NAME -> Comparator.comparing(CardReportItemResponse::holderName);
        };
    if (descending) {
      primary = primary.reversed();
    }
    return primary.thenComparing(CardReportItemResponse::creditCardId);
  }

  private static final class CardBucket {
    private final CreditCard card;
    private final List<Expense> purchases = new ArrayList<>();
    private final List<CreditCardInvoice> invoices = new ArrayList<>();
    private final List<CreditCardInvoicePayment> payments = new ArrayList<>();
    private final List<CreditCardCreditApplication> credits = new ArrayList<>();
    private final List<ExpenseInstallmentAdjustment> installmentAdjustments = new ArrayList<>();
    private final List<CreditCardInvoiceAdjustment> invoiceAdjustments = new ArrayList<>();

    private CardBucket(CreditCard card) {
      this.card = card;
    }
  }

  private static final class ResponsibleBucket {
    private final String key;
    private final ResponsibleType responsibleType;
    private final String responsibleName;
    private PeriodTotals expenseTotals = PeriodTotals.zero();
    private IncomeTotals incomeTotals = IncomeTotals.ZERO;
    private BigDecimal periodReceived;
    private boolean hasExpense;
    private boolean hasIncome;

    private ResponsibleBucket(String key, ResponsibleType responsibleType, String responsibleName) {
      this.key = key;
      this.responsibleType = responsibleType;
      this.responsibleName = responsibleName;
    }
  }

  private record IncomeTotals(BigDecimal amount, BigDecimal accrued, BigDecimal received) {

    static final IncomeTotals ZERO =
        new IncomeTotals(
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

    IncomeTotals add(IncomeTotals movementTotals, BigDecimal originalAmount) {
      return new IncomeTotals(
          money(amount.add(originalAmount)),
          money(accrued.add(movementTotals.accrued)),
          money(received.add(movementTotals.received)));
    }

    BigDecimal remaining() {
      return money(amount.add(accrued).subtract(received));
    }
  }

  private record PeriodTotals(
      BigDecimal original,
      BigDecimal discount,
      BigDecimal surcharge,
      BigDecimal obligation,
      BigDecimal paid,
      BigDecimal remaining) {

    static PeriodTotals zero() {
      BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      return new PeriodTotals(zero, zero, zero, zero, zero, zero);
    }

    PeriodTotals add(PeriodTotals other) {
      return new PeriodTotals(
          money(original.add(other.original)),
          money(discount.add(other.discount)),
          money(surcharge.add(other.surcharge)),
          money(obligation.add(other.obligation)),
          money(paid.add(other.paid)),
          money(remaining.add(other.remaining)));
    }

    PeriodTotals withPaidFor(PaymentMethod paymentMethod) {
      if (paymentMethod == PaymentMethod.CREDIT_CARD) {
        return new PeriodTotals(
            original,
            discount,
            surcharge,
            obligation,
            money(obligation.subtract(remaining)),
            remaining);
      }
      return this;
    }

    ExpenseReportSummaryResponse toSummary() {
      return new ExpenseReportSummaryResponse(
          original, discount, surcharge, obligation, paid, remaining);
    }
  }
}
