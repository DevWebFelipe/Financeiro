package br.com.financialcontrol.incomes;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.incomes.dto.CreateIncomeAccrualRequest;
import br.com.financialcontrol.incomes.dto.CreateIncomeReceiptRequest;
import br.com.financialcontrol.incomes.dto.CreateIncomeRequest;
import br.com.financialcontrol.incomes.dto.IncomeMovementPageResponse;
import br.com.financialcontrol.incomes.dto.IncomeMovementResponse;
import br.com.financialcontrol.incomes.dto.IncomePageResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.incomes.dto.UpdateIncomeRequest;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomeService {

  static final String INCOME_NOT_FOUND = "Receita não encontrada.";
  static final String MOVEMENT_NOT_FOUND = "Movimentação não encontrada.";
  static final String ONLY_EXPECTED_CAN_BE_CANCELLED =
      "Somente receitas esperadas podem ser canceladas.";
  static final String CANCEL_REQUIRES_NO_ACTIVE_RECEIPT =
      "Não é permitido cancelar receita com recebimento ativo.";
  static final String RECEIVED_CANNOT_BE_EDITED = "Receita recebida não pode ser editada.";
  static final String CANCELLED_CANNOT_BE_EDITED = "Receita cancelada não pode ser editada.";
  static final String AMOUNT_LOCKED_AFTER_MOVEMENTS =
      "O valor original não pode ser alterado após movimentações.";
  static final String CANCELLED_CANNOT_HAVE_MOVEMENTS =
      "Receita cancelada não aceita movimentações.";
  static final String FUTURE_DATE = "A data da movimentação não pode ser futura.";
  static final String OVER_RECEIPT =
      "O valor do recebimento não pode ser maior que o saldo a receber.";
  static final String MOVEMENT_ALREADY_REVERSED = "A movimentação já está estornada.";
  static final String ACCRUAL_REVERSE_NEGATIVE =
      "O estorno do acréscimo deixaria o saldo a receber negativo.";
  static final String OTHER_REQUIRES_NAME =
      "O nome do responsável é obrigatório quando o tipo for OTHER.";
  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";

  private final IncomeRepository incomeRepository;
  private final IncomeMovementRepository incomeMovementRepository;
  private final AccountService accountService;
  private final CategoryService categoryService;
  private final Clock clock;

  public IncomeService(
      IncomeRepository incomeRepository,
      IncomeMovementRepository incomeMovementRepository,
      AccountService accountService,
      CategoryService categoryService,
      Clock clock) {
    this.incomeRepository = incomeRepository;
    this.incomeMovementRepository = incomeMovementRepository;
    this.accountService = accountService;
    this.categoryService = categoryService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public IncomePageResponse list(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      IncomeStatus status,
      UUID categoryId,
      UUID accountId,
      int page,
      int size) {
    assertPage(page, size);
    Page<Income> result =
        incomeRepository.searchByUser(
            authenticatedUser.userId(),
            status,
            categoryId,
            accountId,
            startDate,
            endDate,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
    return new IncomePageResponse(
        result.getContent().stream().map(IncomeResponse::from).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public IncomeResponse get(AuthenticatedUser authenticatedUser, UUID incomeId) {
    return IncomeResponse.from(requireOwnedIncome(authenticatedUser.userId(), incomeId));
  }

  @Transactional
  public IncomeResponse create(AuthenticatedUser authenticatedUser, CreateIncomeRequest request) {
    UUID userId = authenticatedUser.userId();
    Category category =
        categoryService.requireActiveOwnedIncomeCategory(userId, request.categoryId());
    Instant now = Instant.now(clock);
    Income income = new Income();
    income.setId(UuidV7.create());
    income.setUserId(userId);
    income.setCategory(category);
    income.setDescription(request.description());
    income.setAmount(normalizeMoney(request.amount()));
    income.setExpectedDate(request.expectedDate());
    income.setAccount(null);
    income.setReceivedDate(null);
    income.setStatus(IncomeStatus.EXPECTED);
    income.setResponsibleType(request.responsibleType());
    income.setResponsibleName(
        resolveResponsibleName(request.responsibleType(), request.responsibleName()));
    income.setNotes(request.notes());
    income.setCreatedAt(now);
    income.setUpdatedAt(now);
    return IncomeResponse.from(incomeRepository.save(income));
  }

  @Transactional
  public IncomeResponse update(
      AuthenticatedUser authenticatedUser, UUID incomeId, UpdateIncomeRequest request) {
    Income income = requireOwnedIncomeForUpdate(authenticatedUser.userId(), incomeId);
    if (income.getStatus() == IncomeStatus.RECEIVED) {
      throw new BusinessRuleException(RECEIVED_CANNOT_BE_EDITED);
    }
    if (income.getStatus() == IncomeStatus.CANCELLED) {
      throw new BusinessRuleException(CANCELLED_CANNOT_BE_EDITED);
    }
    BigDecimal newAmount = normalizeMoney(request.amount());
    if (newAmount.compareTo(income.getAmount()) != 0
        && incomeMovementRepository.existsByIncome_IdAndUserId(
            income.getId(), income.getUserId())) {
      throw new BusinessRuleException(AMOUNT_LOCKED_AFTER_MOVEMENTS);
    }
    Category category =
        categoryService.requireActiveOwnedIncomeCategory(
            authenticatedUser.userId(), request.categoryId());
    income.setCategory(category);
    income.setDescription(request.description());
    income.setAmount(newAmount);
    income.setExpectedDate(request.expectedDate());
    income.setNotes(request.notes());
    income.setResponsibleType(request.responsibleType());
    income.setResponsibleName(
        resolveResponsibleName(request.responsibleType(), request.responsibleName()));
    income.setUpdatedAt(Instant.now(clock));
    return IncomeResponse.from(incomeRepository.save(income));
  }

  @Transactional
  public IncomeMovementResponse createAccrual(
      AuthenticatedUser authenticatedUser, UUID incomeId, CreateIncomeAccrualRequest request) {
    UUID userId = authenticatedUser.userId();
    Income income = requireOwnedIncomeForUpdate(userId, incomeId);
    assertNotCancelled(income);
    LocalDate date = request.date();
    assertNotFuture(date);
    BigDecimal amount = normalizeMoney(request.amount());
    Instant now = Instant.now(clock);
    IncomeMovement movement =
        persistMovement(income, IncomeMovementType.ACCRUAL, amount, date, null, now);
    applyStatus(income, remainingOf(income), now);
    incomeRepository.save(income);
    return IncomeMovementResponse.from(movement);
  }

  @Transactional
  public IncomeMovementResponse createReceipt(
      AuthenticatedUser authenticatedUser, UUID incomeId, CreateIncomeReceiptRequest request) {
    UUID userId = authenticatedUser.userId();
    Income income = requireOwnedIncomeForUpdate(userId, incomeId);
    assertNotCancelled(income);
    LocalDate date = request.date();
    assertNotFuture(date);
    BigDecimal amount = normalizeMoney(request.amount());
    BigDecimal remaining = remainingOf(income);
    if (amount.compareTo(remaining) > 0) {
      throw new BusinessRuleException(OVER_RECEIPT);
    }
    Account account =
        accountService.requireActiveOwnedAccountForUpdate(userId, request.accountId());
    Instant now = Instant.now(clock);
    IncomeMovement movement =
        persistMovement(income, IncomeMovementType.RECEIPT, amount, date, account, now);
    applyStatus(income, remaining.subtract(amount), now);
    accountService.markInitialBalanceLocked(account);
    incomeRepository.save(income);
    return IncomeMovementResponse.from(movement);
  }

  @Transactional(readOnly = true)
  public IncomeMovementPageResponse listMovements(
      AuthenticatedUser authenticatedUser, UUID incomeId, int page, int size) {
    assertPage(page, size);
    requireOwnedIncome(authenticatedUser.userId(), incomeId);
    Page<IncomeMovement> result =
        incomeMovementRepository.searchByIncomeIdAndUserId(
            incomeId,
            authenticatedUser.userId(),
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "movementDate")
                    .and(Sort.by(Sort.Direction.ASC, "id"))));
    return new IncomeMovementPageResponse(
        result.getContent().stream().map(IncomeMovementResponse::from).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional
  public IncomeMovementResponse reverseMovement(
      AuthenticatedUser authenticatedUser, UUID incomeId, UUID movementId) {
    UUID userId = authenticatedUser.userId();
    Income income = requireOwnedIncomeForUpdate(userId, incomeId);
    if (income.getStatus() == IncomeStatus.CANCELLED) {
      throw new BusinessRuleException(CANCELLED_CANNOT_HAVE_MOVEMENTS);
    }
    IncomeMovement movement =
        incomeMovementRepository
            .findByIdAndIncome_IdAndUserIdForUpdate(movementId, incomeId, userId)
            .orElseThrow(() -> new NotFoundException(MOVEMENT_NOT_FOUND));
    if (movement.getStatus() != IncomeMovementStatus.ACTIVE) {
      throw new BusinessRuleException(MOVEMENT_ALREADY_REVERSED);
    }
    BigDecimal remaining = remainingOf(income);
    Instant now = Instant.now(clock);
    if (movement.getType() == IncomeMovementType.RECEIPT) {
      Account account =
          accountService.requireOwnedAccountForUpdate(userId, movement.getAccount().getId());
      movement.setStatus(IncomeMovementStatus.REVERSED);
      movement.setReversedAt(now);
      movement.setUpdatedAt(now);
      incomeMovementRepository.save(movement);
      applyStatus(income, remaining.add(movement.getAmount()), now);
      accountService.markInitialBalanceLocked(account);
    } else {
      BigDecimal nextRemaining = remaining.subtract(movement.getAmount());
      if (nextRemaining.compareTo(BigDecimal.ZERO) < 0) {
        throw new BusinessRuleException(ACCRUAL_REVERSE_NEGATIVE);
      }
      movement.setStatus(IncomeMovementStatus.REVERSED);
      movement.setReversedAt(now);
      movement.setUpdatedAt(now);
      incomeMovementRepository.save(movement);
      applyStatus(income, nextRemaining, now);
    }
    incomeRepository.save(income);
    return IncomeMovementResponse.from(movement);
  }

  @Transactional
  public IncomeResponse cancel(AuthenticatedUser authenticatedUser, UUID incomeId) {
    Income income = requireOwnedIncomeForUpdate(authenticatedUser.userId(), incomeId);
    if (income.getStatus() != IncomeStatus.EXPECTED) {
      throw new BusinessRuleException(ONLY_EXPECTED_CAN_BE_CANCELLED);
    }
    if (incomeMovementRepository.existsByIncome_IdAndUserIdAndTypeAndStatus(
        income.getId(),
        income.getUserId(),
        IncomeMovementType.RECEIPT,
        IncomeMovementStatus.ACTIVE)) {
      throw new BusinessRuleException(CANCEL_REQUIRES_NO_ACTIVE_RECEIPT);
    }
    income.cancel();
    income.setUpdatedAt(Instant.now(clock));
    return IncomeResponse.from(incomeRepository.save(income));
  }

  private IncomeMovement persistMovement(
      Income income,
      IncomeMovementType type,
      BigDecimal amount,
      LocalDate date,
      Account account,
      Instant now) {
    IncomeMovement movement = new IncomeMovement();
    movement.setId(UuidV7.create());
    movement.setUserId(income.getUserId());
    movement.setIncome(income);
    movement.setType(type);
    movement.setStatus(IncomeMovementStatus.ACTIVE);
    movement.setAmount(amount);
    movement.setMovementDate(date);
    movement.setAccount(account);
    movement.setCreatedAt(now);
    movement.setUpdatedAt(now);
    movement.setReversedAt(null);
    return incomeMovementRepository.save(movement);
  }

  private BigDecimal remainingOf(Income income) {
    BigDecimal accrued =
        zeroIfNull(
            incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
                income.getId(), income.getUserId(), IncomeMovementType.ACCRUAL));
    BigDecimal received =
        zeroIfNull(
            incomeMovementRepository.sumActiveAmountByIncomeIdAndUserIdAndType(
                income.getId(), income.getUserId(), IncomeMovementType.RECEIPT));
    return normalizeMoney(income.getAmount().add(accrued).subtract(received));
  }

  private void applyStatus(Income income, BigDecimal remaining, Instant now) {
    if (remaining.compareTo(BigDecimal.ZERO) == 0) {
      income.setStatus(IncomeStatus.RECEIVED);
    } else {
      income.setStatus(IncomeStatus.EXPECTED);
    }
    income.setUpdatedAt(now);
  }

  private void assertNotCancelled(Income income) {
    if (income.getStatus() == IncomeStatus.CANCELLED) {
      throw new BusinessRuleException(CANCELLED_CANNOT_HAVE_MOVEMENTS);
    }
  }

  private void assertNotFuture(LocalDate date) {
    if (date.isAfter(accountService.today())) {
      throw new BusinessRuleException(FUTURE_DATE);
    }
  }

  private void assertPage(int page, int size) {
    if (page < 0) {
      throw new BusinessRuleException(INVALID_PAGE);
    }
    if (size < 1) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE);
    }
  }

  private Income requireOwnedIncome(UUID userId, UUID incomeId) {
    return incomeRepository
        .findByIdAndUserId(incomeId, userId)
        .orElseThrow(() -> new NotFoundException(INCOME_NOT_FOUND));
  }

  private Income requireOwnedIncomeForUpdate(UUID userId, UUID incomeId) {
    return incomeRepository
        .findByIdAndUserIdForUpdate(incomeId, userId)
        .orElseThrow(() -> new NotFoundException(INCOME_NOT_FOUND));
  }

  private static String resolveResponsibleName(ResponsibleType type, String responsibleName) {
    if (type == ResponsibleType.OTHER) {
      if (responsibleName == null) {
        throw new BusinessRuleException(OTHER_REQUIRES_NAME);
      }
      return responsibleName;
    }
    return null;
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
