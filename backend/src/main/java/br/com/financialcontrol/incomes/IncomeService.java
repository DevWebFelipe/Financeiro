package br.com.financialcontrol.incomes;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.incomes.dto.CreateIncomeRequest;
import br.com.financialcontrol.incomes.dto.IncomePageResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.incomes.dto.ReceiveIncomeRequest;
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
  static final String ONLY_EXPECTED_CAN_BE_RECEIVED =
      "Somente receitas esperadas podem ser recebidas.";
  static final String ONLY_RECEIVED_CAN_BE_REVERSED =
      "Somente receitas recebidas podem ser estornadas.";
  static final String ONLY_EXPECTED_CAN_BE_CANCELLED =
      "Somente receitas esperadas podem ser canceladas.";
  static final String RECEIVED_CANNOT_BE_EDITED = "Receita recebida não pode ser editada.";
  static final String CANCELLED_CANNOT_BE_EDITED = "Receita cancelada não pode ser editada.";
  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";

  private final IncomeRepository incomeRepository;
  private final AccountService accountService;
  private final CategoryService categoryService;
  private final Clock clock;

  public IncomeService(
      IncomeRepository incomeRepository,
      AccountService accountService,
      CategoryService categoryService,
      Clock clock) {
    this.incomeRepository = incomeRepository;
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
    if (page < 0) {
      throw new BusinessRuleException(INVALID_PAGE);
    }
    if (size < 1) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE);
    }
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
    income.setResponsibleType(null);
    income.setResponsibleName(null);
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
    Category category =
        categoryService.requireActiveOwnedIncomeCategory(
            authenticatedUser.userId(), request.categoryId());
    income.setCategory(category);
    income.setDescription(request.description());
    income.setAmount(normalizeMoney(request.amount()));
    income.setExpectedDate(request.expectedDate());
    income.setNotes(request.notes());
    income.setUpdatedAt(Instant.now(clock));
    return IncomeResponse.from(incomeRepository.save(income));
  }

  @Transactional
  public IncomeResponse receive(
      AuthenticatedUser authenticatedUser, UUID incomeId, ReceiveIncomeRequest request) {
    Income income = requireOwnedIncomeForUpdate(authenticatedUser.userId(), incomeId);
    if (income.getStatus() != IncomeStatus.EXPECTED) {
      throw new BusinessRuleException(ONLY_EXPECTED_CAN_BE_RECEIVED);
    }
    Account account =
        accountService.requireActiveOwnedAccountForUpdate(
            authenticatedUser.userId(), request.accountId());
    income.receive(account, request.receivedDate());
    income.setUpdatedAt(Instant.now(clock));
    accountService.markInitialBalanceLocked(account);
    return IncomeResponse.from(incomeRepository.save(income));
  }

  @Transactional
  public IncomeResponse reverse(AuthenticatedUser authenticatedUser, UUID incomeId) {
    Income income = requireOwnedIncomeForUpdate(authenticatedUser.userId(), incomeId);
    if (income.getStatus() != IncomeStatus.RECEIVED) {
      throw new BusinessRuleException(ONLY_RECEIVED_CAN_BE_REVERSED);
    }
    // Capture account before reverse clears account_id (Phase 6); RN010A must remain locked.
    Account receivedAccount = income.getAccount();
    if (receivedAccount != null) {
      Account locked =
          accountService.requireOwnedAccountForUpdate(
              authenticatedUser.userId(), receivedAccount.getId());
      accountService.markInitialBalanceLocked(locked);
    }
    income.reverse();
    income.setUpdatedAt(Instant.now(clock));
    return IncomeResponse.from(incomeRepository.save(income));
  }

  @Transactional
  public IncomeResponse cancel(AuthenticatedUser authenticatedUser, UUID incomeId) {
    Income income = requireOwnedIncomeForUpdate(authenticatedUser.userId(), incomeId);
    if (income.getStatus() != IncomeStatus.EXPECTED) {
      throw new BusinessRuleException(ONLY_EXPECTED_CAN_BE_CANCELLED);
    }
    income.cancel();
    income.setUpdatedAt(Instant.now(clock));
    return IncomeResponse.from(incomeRepository.save(income));
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

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
