package br.com.financialcontrol.payables;

import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.InstallmentBalanceService;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.payables.dto.PayableItemResponse;
import br.com.financialcontrol.payables.dto.PayablePageResponse;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayablesService {

  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";
  static final String INVALID_PAGE_SIZE_MAX = "O tamanho da página não pode ser maior que 100.";
  static final String INVALID_DATA = "Dados inválidos.";
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final int MAX_PAGE_SIZE = 100;
  private static final Set<String> ACCEPTED_STATUSES =
      Set.of(
          "OPEN",
          "PARTIALLY_PAID",
          "PAID",
          "CANCELLED",
          "REFUNDED",
          "SCHEDULED",
          "CLOSED",
          "SETTLED_BY_AGREEMENT");
  private static final List<CreditCardInvoiceStatus> INVOICE_CANDIDATE_STATUSES =
      List.of(
          CreditCardInvoiceStatus.SCHEDULED,
          CreditCardInvoiceStatus.OPEN,
          CreditCardInvoiceStatus.CLOSED);
  private static final Set<ExpenseStatus> EXCLUDED_EXPENSE_STATUSES =
      EnumSet.of(ExpenseStatus.CANCELLED, ExpenseStatus.REFUNDED);

  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final CreditCardInvoiceRepository invoiceRepository;
  private final CreditCardInvoiceService creditCardInvoiceService;
  private final InstallmentBalanceService installmentBalanceService;
  private final PaymentRepository paymentRepository;
  private final Clock clock;

  public PayablesService(
      ExpenseInstallmentRepository expenseInstallmentRepository,
      CreditCardInvoiceRepository invoiceRepository,
      CreditCardInvoiceService creditCardInvoiceService,
      InstallmentBalanceService installmentBalanceService,
      PaymentRepository paymentRepository,
      Clock clock) {
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.invoiceRepository = invoiceRepository;
    this.creditCardInvoiceService = creditCardInvoiceService;
    this.installmentBalanceService = installmentBalanceService;
    this.paymentRepository = paymentRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PayablePageResponse list(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      boolean includeWithoutDueDate,
      String status,
      Boolean overdue,
      UUID creditCardId,
      boolean withoutCreditCard,
      UUID categoryId,
      ResponsibleType responsibleType,
      String search,
      String sort,
      String direction,
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
    validatePeriod(startDate, endDate, year, month);
    Set<String> statuses = parseStatuses(status);
    PayableSortField sortField = parseSort(sort);
    boolean descending = parseDescending(direction);
    String searchTerm = normalizeSearch(search);
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    UUID userId = authenticatedUser.userId();

    List<PayableItemResponse> lines = new ArrayList<>();
    if (!withoutCreditCard) {
      lines.addAll(
          invoiceLines(
              userId,
              startDate,
              endDate,
              year,
              month,
              includeWithoutDueDate,
              statuses,
              overdue,
              creditCardId,
              searchTerm,
              today));
    }
    if (creditCardId == null) {
      lines.addAll(
          installmentLines(
              userId,
              startDate,
              endDate,
              year,
              month,
              includeWithoutDueDate,
              statuses,
              overdue,
              categoryId,
              responsibleType,
              searchTerm,
              today));
    }

    lines.sort(comparator(sortField, descending));

    BigDecimal totalRemaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalOriginal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (PayableItemResponse line : lines) {
      totalRemaining = totalRemaining.add(line.remainingAmount());
      totalOriginal = totalOriginal.add(line.originalAmount());
      totalPaid = totalPaid.add(line.paidAmount());
    }
    totalRemaining = totalRemaining.setScale(2, RoundingMode.HALF_UP);
    totalOriginal = totalOriginal.setScale(2, RoundingMode.HALF_UP);
    totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP);

    int totalItems = lines.size();
    int totalPages = totalItems == 0 ? 0 : (int) Math.ceil(totalItems / (double) size);
    int from = page * size;
    List<PayableItemResponse> items =
        from >= totalItems
            ? List.of()
            : List.copyOf(lines.subList(from, Math.min(from + size, totalItems)));
    return new PayablePageResponse(
        items, page, size, totalItems, totalPages, totalRemaining, totalOriginal, totalPaid);
  }

  @Transactional(readOnly = true)
  public PayablesSummary summarize(AuthenticatedUser authenticatedUser) {
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    UUID userId = authenticatedUser.userId();
    List<PayableItemResponse> lines = new ArrayList<>();
    lines.addAll(
        invoiceLines(userId, null, null, null, null, false, Set.of(), null, null, null, today));
    lines.addAll(
        installmentLines(
            userId, null, null, null, null, false, Set.of(), null, null, null, null, today));
    return toSummary(lines);
  }

  private static PayablesSummary toSummary(List<PayableItemResponse> lines) {
    BigDecimal installmentRemaining = money(BigDecimal.ZERO);
    BigDecimal invoiceRemaining = money(BigDecimal.ZERO);
    BigDecimal overdueInstallmentRemaining = money(BigDecimal.ZERO);
    BigDecimal overdueInvoiceRemaining = money(BigDecimal.ZERO);
    long overdueCount = 0;
    Map<UUID, BigDecimal> invoiceRemainingByCardId = new HashMap<>();
    Map<UUID, BigDecimal> overdueInvoiceRemainingByCardId = new HashMap<>();
    for (PayableItemResponse line : lines) {
      BigDecimal remaining = money(line.remainingAmount());
      if (line.type() == PayableItemType.INSTALLMENT) {
        installmentRemaining = money(installmentRemaining.add(remaining));
        if (line.overdue()) {
          overdueInstallmentRemaining = money(overdueInstallmentRemaining.add(remaining));
          overdueCount++;
        }
        continue;
      }
      invoiceRemaining = money(invoiceRemaining.add(remaining));
      invoiceRemainingByCardId.merge(line.creditCardId(), remaining, PayablesService::addMoney);
      if (line.overdue()) {
        overdueInvoiceRemaining = money(overdueInvoiceRemaining.add(remaining));
        overdueInvoiceRemainingByCardId.merge(
            line.creditCardId(), remaining, PayablesService::addMoney);
        overdueCount++;
      }
    }
    return new PayablesSummary(
        money(installmentRemaining.add(invoiceRemaining)),
        installmentRemaining,
        invoiceRemaining,
        money(overdueInstallmentRemaining.add(overdueInvoiceRemaining)),
        overdueInstallmentRemaining,
        overdueInvoiceRemaining,
        lines.size(),
        overdueCount,
        Map.copyOf(invoiceRemainingByCardId),
        Map.copyOf(overdueInvoiceRemainingByCardId));
  }

  private static BigDecimal addMoney(BigDecimal left, BigDecimal right) {
    return money(left.add(right));
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private List<PayableItemResponse> installmentLines(
      UUID userId,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      boolean includeWithoutDueDate,
      Set<String> statuses,
      Boolean overdue,
      UUID categoryId,
      ResponsibleType responsibleType,
      String searchTerm,
      LocalDate today) {
    List<ExpenseInstallment> candidates =
        expenseInstallmentRepository.findAllByUserIdAndPaymentMethodsExcludingStatuses(
            userId, List.of(PaymentMethod.ACCOUNT, PaymentMethod.NONE), EXCLUDED_EXPENSE_STATUSES);
    List<PayableItemResponse> lines = new ArrayList<>();
    for (ExpenseInstallment installment : candidates) {
      Expense expense = installment.getExpense();
      BigDecimal remaining = installmentBalanceService.remaining(installment);
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      BigDecimal paid =
          zeroIfNull(
              paymentRepository.sumActiveAmountByInstallmentIdAndUserId(
                  installment.getId(), userId));
      boolean lineOverdue = isInstallmentOverdue(installment.getDueDate(), remaining, today);
      PayableItemResponse item =
          new PayableItemResponse(
              installment.getId(),
              PayableItemType.INSTALLMENT,
              expense.getId(),
              null,
              expense.getCategory().getId(),
              expense.getAccount() == null ? null : expense.getAccount().getId(),
              expense.getPaymentMethod(),
              expense.getDescription(),
              expense.getExpenseDate(),
              installment.getDueDate(),
              installment.getAmount().setScale(2, RoundingMode.HALF_UP),
              paid.setScale(2, RoundingMode.HALF_UP),
              remaining.setScale(2, RoundingMode.HALF_UP),
              installment.getStatus().name(),
              lineOverdue,
              expense.getResponsibleType(),
              expense.getResponsibleName());
      if (matchesFilters(
          item,
          startDate,
          endDate,
          year,
          month,
          includeWithoutDueDate,
          statuses,
          overdue,
          categoryId,
          responsibleType,
          searchTerm,
          expense.getNotes(),
          expense.getBarcode(),
          null)) {
        lines.add(item);
      }
    }
    return lines;
  }

  private List<PayableItemResponse> invoiceLines(
      UUID userId,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      boolean includeWithoutDueDate,
      Set<String> statuses,
      Boolean overdue,
      UUID creditCardId,
      String searchTerm,
      LocalDate today) {
    List<CreditCardInvoice> candidates =
        invoiceRepository.findAllByUserIdAndStatusInWithCard(userId, INVOICE_CANDIDATE_STATUSES);
    List<PayableItemResponse> lines = new ArrayList<>();
    for (CreditCardInvoice invoice : candidates) {
      if (creditCardId != null && !invoice.getCreditCard().getId().equals(creditCardId)) {
        continue;
      }
      CreditCardInvoiceResponse derived = creditCardInvoiceService.toResponse(invoice);
      if (derived.remainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      boolean lineOverdue = isInvoiceOverdue(invoice, derived.remainingAmount(), today);
      PayableItemResponse item =
          new PayableItemResponse(
              invoice.getId(),
              PayableItemType.INVOICE,
              null,
              invoice.getCreditCard().getId(),
              null,
              null,
              null,
              invoice.getCreditCard().getName(),
              invoice.getClosingDate(),
              invoice.getDueDate(),
              derived.totalAmount().setScale(2, RoundingMode.HALF_UP),
              derived.paidAmount().setScale(2, RoundingMode.HALF_UP),
              derived.remainingAmount().setScale(2, RoundingMode.HALF_UP),
              invoice.getStatus().name(),
              lineOverdue,
              null,
              null);
      if (matchesFilters(
          item,
          startDate,
          endDate,
          year,
          month,
          includeWithoutDueDate,
          statuses,
          overdue,
          null,
          null,
          searchTerm,
          null,
          null,
          invoice.getCreditCard().getName())) {
        lines.add(item);
      }
    }
    return lines;
  }

  private static boolean matchesFilters(
      PayableItemResponse item,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      boolean includeWithoutDueDate,
      Set<String> statuses,
      Boolean overdue,
      UUID categoryId,
      ResponsibleType responsibleType,
      String searchTerm,
      String notes,
      String barcode,
      String invoiceSearchName) {
    if (!statuses.isEmpty() && !statuses.contains(item.status())) {
      return false;
    }
    if (overdue != null && item.overdue() != overdue) {
      return false;
    }
    if (item.type() == PayableItemType.INSTALLMENT) {
      if (categoryId != null && !categoryId.equals(item.categoryId())) {
        return false;
      }
      if (responsibleType != null && responsibleType != item.responsibleType()) {
        return false;
      }
    }
    if (!matchesPeriod(item.dueDate(), startDate, endDate, year, month, includeWithoutDueDate)) {
      return false;
    }
    return matchesSearch(item, searchTerm, notes, barcode, invoiceSearchName);
  }

  private static boolean matchesPeriod(
      LocalDate dueDate,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      boolean includeWithoutDueDate) {
    boolean temporal = startDate != null || endDate != null || (year != null && month != null);
    if (!temporal) {
      return true;
    }
    if (dueDate == null) {
      return includeWithoutDueDate;
    }
    if (startDate != null && dueDate.isBefore(startDate)) {
      return false;
    }
    if (endDate != null && dueDate.isAfter(endDate)) {
      return false;
    }
    if (year != null && month != null) {
      YearMonth selected = YearMonth.of(year, month);
      if (!YearMonth.from(dueDate).equals(selected)) {
        return false;
      }
    }
    return true;
  }

  private static boolean matchesSearch(
      PayableItemResponse item,
      String searchTerm,
      String notes,
      String barcode,
      String invoiceSearchName) {
    if (searchTerm == null) {
      return true;
    }
    if (item.type() == PayableItemType.INSTALLMENT) {
      return containsIgnoreCase(item.name(), searchTerm)
          || containsIgnoreCase(notes, searchTerm)
          || containsIgnoreCase(barcode, searchTerm);
    }
    return containsIgnoreCase(invoiceSearchName, searchTerm);
  }

  private static boolean containsIgnoreCase(String value, String searchTerm) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(searchTerm);
  }

  private static boolean isInstallmentOverdue(
      LocalDate dueDate, BigDecimal remaining, LocalDate today) {
    return remaining.compareTo(BigDecimal.ZERO) > 0 && dueDate != null && dueDate.isBefore(today);
  }

  private static boolean isInvoiceOverdue(
      CreditCardInvoice invoice, BigDecimal remaining, LocalDate today) {
    if (invoice.getStatus() == CreditCardInvoiceStatus.SCHEDULED) {
      return false;
    }
    if (invoice.getStatus() != CreditCardInvoiceStatus.OPEN
        && invoice.getStatus() != CreditCardInvoiceStatus.CLOSED) {
      return false;
    }
    return remaining.compareTo(BigDecimal.ZERO) > 0
        && invoice.getDueDate() != null
        && invoice.getDueDate().isBefore(today);
  }

  private static void validatePeriod(
      LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
    if ((year == null) != (month == null)) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    if (month != null && (month < 1 || month > 12)) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static Set<String> parseStatuses(String status) {
    if (status == null || status.isBlank()) {
      return Set.of();
    }
    Set<String> values = new LinkedHashSet<>();
    for (String token : status.split(",")) {
      String value = token.trim();
      if (value.isEmpty() || !ACCEPTED_STATUSES.contains(value)) {
        throw new InvalidRequestException(INVALID_DATA);
      }
      values.add(value);
    }
    return values;
  }

  private static PayableSortField parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return PayableSortField.DUE_DATE;
    }
    PayableSortField field = PayableSortField.fromQuery(sort);
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

  private static String normalizeSearch(String search) {
    if (search == null || search.isBlank()) {
      return null;
    }
    return search.toLowerCase(Locale.ROOT);
  }

  private static Comparator<PayableItemResponse> comparator(
      PayableSortField sortField, boolean descending) {
    Comparator<PayableItemResponse> primary =
        switch (sortField) {
          case NAME -> compareNullable(PayableItemResponse::name, Comparator.naturalOrder());
          case PURCHASE_DATE ->
              compareNullable(PayableItemResponse::purchaseDate, Comparator.naturalOrder());
          case DUE_DATE -> dueDateComparator();
          case ORIGINAL_AMOUNT -> Comparator.comparing(PayableItemResponse::originalAmount);
          case REMAINING_AMOUNT -> Comparator.comparing(PayableItemResponse::remainingAmount);
          case STATUS -> Comparator.comparing(PayableItemResponse::status);
          case PAID_AMOUNT -> Comparator.comparing(PayableItemResponse::paidAmount);
        };
    if (descending) {
      primary = primary.reversed();
    }
    return primary.thenComparing(PayableItemResponse::id);
  }

  private static Comparator<PayableItemResponse> dueDateComparator() {
    return (left, right) -> {
      LocalDate leftDue = left.dueDate();
      LocalDate rightDue = right.dueDate();
      if (leftDue == null && rightDue == null) {
        return 0;
      }
      if (leftDue == null) {
        return 1;
      }
      if (rightDue == null) {
        return -1;
      }
      return leftDue.compareTo(rightDue);
    };
  }

  private static <T> Comparator<PayableItemResponse> compareNullable(
      java.util.function.Function<PayableItemResponse, T> getter, Comparator<T> valueOrder) {
    return (left, right) -> {
      T leftValue = getter.apply(left);
      T rightValue = getter.apply(right);
      if (leftValue == null && rightValue == null) {
        return 0;
      }
      if (leftValue == null) {
        return 1;
      }
      if (rightValue == null) {
        return -1;
      }
      return valueOrder.compare(leftValue, rightValue);
    };
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
  }
}
