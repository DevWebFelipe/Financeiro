package br.com.financialcontrol.receivables;

import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.incomes.Income;
import br.com.financialcontrol.incomes.IncomeMovementRepository;
import br.com.financialcontrol.incomes.IncomeMovementType;
import br.com.financialcontrol.incomes.IncomeRepository;
import br.com.financialcontrol.incomes.IncomeStatus;
import br.com.financialcontrol.receivables.dto.ReceivableItemResponse;
import br.com.financialcontrol.receivables.dto.ReceivablePageResponse;
import br.com.financialcontrol.receivables.dto.ReceivableSummaryResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivablesService {

  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";
  static final String INVALID_PAGE_SIZE_MAX = "O tamanho da página não pode ser maior que 100.";
  static final String INVALID_DATA = "Dados inválidos.";
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final int MAX_PAGE_SIZE = 100;

  private final IncomeRepository incomeRepository;
  private final IncomeMovementRepository incomeMovementRepository;
  private final Clock clock;

  public ReceivablesService(
      IncomeRepository incomeRepository,
      IncomeMovementRepository incomeMovementRepository,
      Clock clock) {
    this.incomeRepository = incomeRepository;
    this.incomeMovementRepository = incomeMovementRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ReceivablePageResponse list(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      String dateType,
      String status,
      Boolean overdue,
      UUID categoryId,
      UUID accountId,
      ResponsibleType responsibleType,
      String responsibleName,
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

    IncomeStatus parsedStatus = parseStatus(status);
    ReceivableDateType parsedDateType = parseDateType(dateType);
    ReceivableSortField sortField = parseSort(sort);
    boolean descending = parseDescending(direction);
    String nameFilter = normalizeResponsibleName(responsibleName);
    validateCombinations(parsedStatus, parsedDateType, overdue, startDate, endDate);

    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    DateBounds bounds = dateBounds(parsedDateType, startDate, endDate, overdue, today);
    Sort order =
        Sort.by(descending ? Sort.Direction.DESC : Sort.Direction.ASC, sortField.property())
            .and(Sort.by(Sort.Direction.ASC, "id"));
    PageRequest pageable = PageRequest.of(page, size, order);

    Page<Income> result =
        incomeRepository.searchReceivables(
            authenticatedUser.userId(),
            parsedStatus,
            categoryId,
            accountId,
            responsibleType,
            nameFilter,
            bounds.expectedMin(),
            bounds.expectedMax(),
            bounds.receivedMin(),
            bounds.receivedMax(),
            pageable);
    Object[] totals =
        incomeRepository
            .sumReceivableAmounts(
                authenticatedUser.userId(),
                parsedStatus,
                categoryId,
                accountId,
                responsibleType,
                nameFilter,
                bounds.expectedMin(),
                bounds.expectedMax(),
                bounds.receivedMin(),
                bounds.receivedMax(),
                today)
            .getFirst();

    BigDecimal futureAmount = money(totals[0]);
    BigDecimal overdueAmount = money(totals[1]);
    BigDecimal receivedAmount = money(totals[2]);
    ReceivableSummaryResponse summary =
        new ReceivableSummaryResponse(
            futureAmount,
            overdueAmount,
            futureAmount.add(overdueAmount).setScale(2, RoundingMode.HALF_UP),
            receivedAmount);

    List<ReceivableItemResponse> items =
        toItems(result.getContent(), today, authenticatedUser.userId());
    return new ReceivablePageResponse(
        items,
        summary,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  private List<ReceivableItemResponse> toItems(List<Income> incomes, LocalDate today, UUID userId) {
    Map<UUID, Totals> totalsByIncome = loadTotals(userId, incomes);
    return incomes.stream()
        .map(
            income ->
                toItem(income, today, totalsByIncome.getOrDefault(income.getId(), Totals.ZERO)))
        .toList();
  }

  private Map<UUID, Totals> loadTotals(UUID userId, List<Income> incomes) {
    if (incomes.isEmpty()) {
      return Map.of();
    }
    List<UUID> ids = incomes.stream().map(Income::getId).toList();
    Map<UUID, Totals> totals = new HashMap<>();
    for (Object[] row :
        incomeMovementRepository.sumActiveAmountsByIncomeIdsAndUserId(userId, ids)) {
      UUID incomeId = (UUID) row[0];
      IncomeMovementType type = (IncomeMovementType) row[1];
      BigDecimal amount = money(row[2]);
      Totals current = totals.getOrDefault(incomeId, Totals.ZERO);
      if (type == IncomeMovementType.ACCRUAL) {
        totals.put(incomeId, new Totals(amount, current.received()));
      } else if (type == IncomeMovementType.RECEIPT) {
        totals.put(incomeId, new Totals(current.accrued(), amount));
      }
    }
    return totals;
  }

  private static ReceivableItemResponse toItem(Income income, LocalDate today, Totals totals) {
    boolean overdue =
        income.getStatus() == IncomeStatus.EXPECTED && income.getExpectedDate().isBefore(today);
    BigDecimal amount = income.getAmount().setScale(2, RoundingMode.HALF_UP);
    BigDecimal remaining =
        amount.add(totals.accrued()).subtract(totals.received()).setScale(2, RoundingMode.HALF_UP);
    return new ReceivableItemResponse(
        income.getId(),
        income.getCategory().getId(),
        income.getAccount() == null ? null : income.getAccount().getId(),
        income.getResponsibleType(),
        income.getResponsibleName(),
        income.getDescription(),
        amount,
        totals.accrued(),
        totals.received(),
        remaining,
        income.getExpectedDate(),
        income.getReceivedDate(),
        income.getStatus(),
        overdue);
  }

  private static void validateCombinations(
      IncomeStatus status,
      ReceivableDateType dateType,
      Boolean overdue,
      LocalDate startDate,
      LocalDate endDate) {
    if ((startDate != null || endDate != null) && dateType == null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    if (status == IncomeStatus.RECEIVED && dateType == ReceivableDateType.EXPECTED) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    if (status == IncomeStatus.RECEIVED && overdue != null) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static DateBounds dateBounds(
      ReceivableDateType dateType,
      LocalDate startDate,
      LocalDate endDate,
      Boolean overdue,
      LocalDate today) {
    LocalDate expectedMin = null;
    LocalDate expectedMax = null;
    LocalDate receivedMin = null;
    LocalDate receivedMax = null;
    if (dateType == ReceivableDateType.EXPECTED) {
      expectedMin = startDate;
      expectedMax = endDate;
    } else if (dateType == ReceivableDateType.RECEIVED) {
      receivedMin = startDate;
      receivedMax = endDate;
    }
    if (Boolean.TRUE.equals(overdue)) {
      expectedMax = minDate(expectedMax, today.minusDays(1));
    } else if (Boolean.FALSE.equals(overdue)) {
      expectedMin = maxDate(expectedMin, today);
    }
    return new DateBounds(expectedMin, expectedMax, receivedMin, receivedMax);
  }

  private static IncomeStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return IncomeStatus.EXPECTED;
    }
    if (status.indexOf(',') >= 0) {
      throw new InvalidRequestException(INVALID_DATA);
    }
    try {
      IncomeStatus parsed = IncomeStatus.valueOf(status.trim());
      if (parsed == IncomeStatus.CANCELLED) {
        throw new InvalidRequestException(INVALID_DATA);
      }
      return parsed;
    } catch (IllegalArgumentException exception) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static ReceivableDateType parseDateType(String dateType) {
    if (dateType == null || dateType.isBlank()) {
      return null;
    }
    try {
      return ReceivableDateType.valueOf(dateType.trim());
    } catch (IllegalArgumentException exception) {
      throw new InvalidRequestException(INVALID_DATA);
    }
  }

  private static ReceivableSortField parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return ReceivableSortField.EXPECTED_DATE;
    }
    ReceivableSortField field = ReceivableSortField.fromQuery(sort);
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

  private static String normalizeResponsibleName(String responsibleName) {
    if (responsibleName == null || responsibleName.isBlank()) {
      return null;
    }
    return responsibleName;
  }

  private static LocalDate minDate(LocalDate left, LocalDate right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return left.isBefore(right) ? left : right;
  }

  private static LocalDate maxDate(LocalDate left, LocalDate right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return left.isAfter(right) ? left : right;
  }

  private static BigDecimal money(Object value) {
    BigDecimal amount =
        value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  private record DateBounds(
      LocalDate expectedMin, LocalDate expectedMax, LocalDate receivedMin, LocalDate receivedMax) {}

  private record Totals(BigDecimal accrued, BigDecimal received) {
    private static final Totals ZERO =
        new Totals(
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
  }
}
