package br.com.financialcontrol.financial_goals;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.financial_goals.dto.CreateFinancialGoalRequest;
import br.com.financialcontrol.financial_goals.dto.CreateGoalContributionRequest;
import br.com.financialcontrol.financial_goals.dto.CreateGoalContributionResponse;
import br.com.financialcontrol.financial_goals.dto.CreateGoalRedemptionRequest;
import br.com.financialcontrol.financial_goals.dto.CreateGoalRedemptionResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalPageResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalResponse;
import br.com.financialcontrol.financial_goals.dto.GoalContributionResponse;
import br.com.financialcontrol.financial_goals.dto.GoalRedemptionResponse;
import br.com.financialcontrol.financial_goals.dto.UpdateFinancialGoalRequest;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialGoalService {

  static final String GOAL_NOT_FOUND = "Meta não encontrada.";
  static final String ONLY_ACTIVE_CAN_BE_EDITED = "Somente metas ativas podem ser editadas.";
  static final String ONLY_ACTIVE_CAN_RECEIVE_CONTRIBUTION =
      "Somente metas ativas podem receber contribuições.";
  static final String ONLY_ACTIVE_OR_COMPLETED_CAN_BE_REDEEMED =
      "Somente metas ativas ou concluídas podem ser resgatadas.";
  static final String ONLY_ACTIVE_CAN_BE_COMPLETED = "Somente metas ativas podem ser concluídas.";
  static final String ONLY_ACTIVE_CAN_BE_CANCELLED = "Somente metas ativas podem ser canceladas.";
  static final String CANCEL_REQUIRES_ZERO_CURRENT =
      "A meta só pode ser cancelada quando não possui valor reservado.";
  static final String REDEMPTION_EXCEEDS_CURRENT =
      "O valor do resgate não pode exceder o valor acumulado da meta.";
  static final String FUTURE_CONTRIBUTION_DATE = "A data da contribuição não pode ser futura.";
  static final String FUTURE_REDEMPTION_DATE = "A data do resgate não pode ser futura.";
  static final String INSUFFICIENT_BALANCE = "Saldo insuficiente para realizar a operação.";
  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";

  private final FinancialGoalRepository financialGoalRepository;
  private final GoalContributionRepository goalContributionRepository;
  private final GoalRedemptionRepository goalRedemptionRepository;
  private final AccountService accountService;
  private final Clock clock;

  public FinancialGoalService(
      FinancialGoalRepository financialGoalRepository,
      GoalContributionRepository goalContributionRepository,
      GoalRedemptionRepository goalRedemptionRepository,
      AccountService accountService,
      Clock clock) {
    this.financialGoalRepository = financialGoalRepository;
    this.goalContributionRepository = goalContributionRepository;
    this.goalRedemptionRepository = goalRedemptionRepository;
    this.accountService = accountService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public FinancialGoalPageResponse list(
      AuthenticatedUser authenticatedUser, FinancialGoalStatus status, int page, int size) {
    if (page < 0) {
      throw new BusinessRuleException(INVALID_PAGE);
    }
    if (size < 1) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE);
    }
    UUID userId = authenticatedUser.userId();
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
    Page<FinancialGoal> result =
        status == null
            ? financialGoalRepository.findAllByUserId(userId, pageable)
            : financialGoalRepository.findAllByUserIdAndStatus(userId, status, pageable);
    return new FinancialGoalPageResponse(
        result.getContent().stream().map(goal -> toResponse(goal, userId)).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public FinancialGoalResponse get(AuthenticatedUser authenticatedUser, UUID goalId) {
    FinancialGoal goal = requireOwnedGoal(authenticatedUser.userId(), goalId);
    return toResponse(goal, authenticatedUser.userId());
  }

  @Transactional
  public FinancialGoalResponse create(
      AuthenticatedUser authenticatedUser, CreateFinancialGoalRequest request) {
    UUID userId = authenticatedUser.userId();
    Account account = accountService.requireActiveOwnedAccount(userId, request.accountId());
    Instant now = Instant.now(clock);
    FinancialGoal goal = new FinancialGoal();
    goal.setId(UuidV7.create());
    goal.setUserId(userId);
    goal.setAccount(account);
    goal.setName(request.name());
    goal.setDescription(request.description());
    goal.setTargetAmount(normalizeMoney(request.targetAmount()));
    goal.setTargetDate(request.targetDate());
    goal.setStatus(FinancialGoalStatus.ACTIVE);
    goal.setCreatedAt(now);
    goal.setUpdatedAt(now);
    return toResponse(financialGoalRepository.save(goal), userId);
  }

  @Transactional
  public FinancialGoalResponse update(
      AuthenticatedUser authenticatedUser, UUID goalId, UpdateFinancialGoalRequest request) {
    UUID userId = authenticatedUser.userId();
    FinancialGoal goal = requireOwnedGoalForUpdate(userId, goalId);
    assertActive(goal, ONLY_ACTIVE_CAN_BE_EDITED);
    goal.setName(request.name());
    goal.setDescription(request.description());
    goal.setTargetAmount(normalizeMoney(request.targetAmount()));
    goal.setTargetDate(request.targetDate());
    goal.setUpdatedAt(Instant.now(clock));
    return toResponse(financialGoalRepository.save(goal), userId);
  }

  @Transactional
  public CreateGoalContributionResponse contribute(
      AuthenticatedUser authenticatedUser, UUID goalId, CreateGoalContributionRequest request) {
    UUID userId = authenticatedUser.userId();
    FinancialGoal goal = requireOwnedGoal(userId, goalId);
    Account account =
        accountService.requireActiveOwnedAccountForUpdate(userId, goal.getAccount().getId());
    goal = requireOwnedGoalForUpdate(userId, goalId);
    assertActive(goal, ONLY_ACTIVE_CAN_RECEIVE_CONTRIBUTION);

    LocalDate today = accountService.today();
    if (request.contributionDate().isAfter(today)) {
      throw new BusinessRuleException(FUTURE_CONTRIBUTION_DATE);
    }

    BigDecimal amount = normalizeMoney(request.amount());
    if (accountService.calculateAvailableBalance(account).compareTo(amount) < 0) {
      throw new BusinessRuleException(INSUFFICIENT_BALANCE);
    }

    Instant now = Instant.now(clock);
    GoalContribution contribution = new GoalContribution();
    contribution.setId(UuidV7.create());
    contribution.setUserId(userId);
    contribution.setGoal(goal);
    contribution.setAmount(amount);
    contribution.setContributionDate(request.contributionDate());
    contribution.setNotes(request.notes());
    contribution.setCreatedAt(now);
    goalContributionRepository.save(contribution);
    accountService.markInitialBalanceLocked(account);

    return new CreateGoalContributionResponse(
        GoalContributionResponse.from(contribution), toResponse(goal, userId));
  }

  @Transactional(readOnly = true)
  public List<GoalContributionResponse> listContributions(
      AuthenticatedUser authenticatedUser, UUID goalId) {
    UUID userId = authenticatedUser.userId();
    requireOwnedGoal(userId, goalId);
    return goalContributionRepository
        .findAllByGoal_IdAndUserIdOrderByContributionDateAscCreatedAtAscIdAsc(goalId, userId)
        .stream()
        .map(GoalContributionResponse::from)
        .toList();
  }

  @Transactional
  public CreateGoalRedemptionResponse redeem(
      AuthenticatedUser authenticatedUser, UUID goalId, CreateGoalRedemptionRequest request) {
    UUID userId = authenticatedUser.userId();
    FinancialGoal goal = requireOwnedGoal(userId, goalId);
    Account account =
        accountService.requireActiveOwnedAccountForUpdate(userId, goal.getAccount().getId());
    goal = requireOwnedGoalForUpdate(userId, goalId);
    if (goal.getStatus() == FinancialGoalStatus.CANCELLED) {
      throw new BusinessRuleException(ONLY_ACTIVE_OR_COMPLETED_CAN_BE_REDEEMED);
    }
    if (goal.getStatus() != FinancialGoalStatus.ACTIVE
        && goal.getStatus() != FinancialGoalStatus.COMPLETED) {
      throw new BusinessRuleException(ONLY_ACTIVE_OR_COMPLETED_CAN_BE_REDEEMED);
    }

    LocalDate today = accountService.today();
    if (request.redemptionDate().isAfter(today)) {
      throw new BusinessRuleException(FUTURE_REDEMPTION_DATE);
    }

    BigDecimal amount = normalizeMoney(request.amount());
    BigDecimal currentAmount = currentAmount(goal.getId(), userId);
    if (amount.compareTo(currentAmount) > 0) {
      throw new BusinessRuleException(REDEMPTION_EXCEEDS_CURRENT);
    }

    Instant now = Instant.now(clock);
    GoalRedemption redemption = new GoalRedemption();
    redemption.setId(UuidV7.create());
    redemption.setUserId(userId);
    redemption.setGoal(goal);
    redemption.setAmount(amount);
    redemption.setRedemptionDate(request.redemptionDate());
    redemption.setNotes(request.notes());
    redemption.setCreatedAt(now);
    goalRedemptionRepository.save(redemption);
    accountService.markInitialBalanceLocked(account);

    return new CreateGoalRedemptionResponse(
        GoalRedemptionResponse.from(redemption), toResponse(goal, userId));
  }

  @Transactional(readOnly = true)
  public List<GoalRedemptionResponse> listRedemptions(
      AuthenticatedUser authenticatedUser, UUID goalId) {
    UUID userId = authenticatedUser.userId();
    requireOwnedGoal(userId, goalId);
    return goalRedemptionRepository
        .findAllByGoal_IdAndUserIdOrderByRedemptionDateAscCreatedAtAscIdAsc(goalId, userId)
        .stream()
        .map(GoalRedemptionResponse::from)
        .toList();
  }

  @Transactional
  public FinancialGoalResponse complete(AuthenticatedUser authenticatedUser, UUID goalId) {
    UUID userId = authenticatedUser.userId();
    FinancialGoal goal = requireOwnedGoalForUpdate(userId, goalId);
    assertActive(goal, ONLY_ACTIVE_CAN_BE_COMPLETED);
    goal.setStatus(FinancialGoalStatus.COMPLETED);
    goal.setUpdatedAt(Instant.now(clock));
    return toResponse(financialGoalRepository.save(goal), userId);
  }

  @Transactional
  public FinancialGoalResponse cancel(AuthenticatedUser authenticatedUser, UUID goalId) {
    UUID userId = authenticatedUser.userId();
    FinancialGoal goal = requireOwnedGoalForUpdate(userId, goalId);
    assertActive(goal, ONLY_ACTIVE_CAN_BE_CANCELLED);
    BigDecimal current = currentAmount(goal.getId(), userId);
    if (current.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) != 0) {
      throw new BusinessRuleException(CANCEL_REQUIRES_ZERO_CURRENT);
    }
    goal.setStatus(FinancialGoalStatus.CANCELLED);
    goal.setUpdatedAt(Instant.now(clock));
    return toResponse(financialGoalRepository.save(goal), userId);
  }

  private FinancialGoalResponse toResponse(FinancialGoal goal, UUID userId) {
    return FinancialGoalResponse.from(goal, currentAmount(goal.getId(), userId));
  }

  private BigDecimal currentAmount(UUID goalId, UUID userId) {
    return accountService.calculateGoalCurrentAmount(goalId, userId, null);
  }

  private FinancialGoal requireOwnedGoal(UUID userId, UUID goalId) {
    return financialGoalRepository
        .findByIdAndUserId(goalId, userId)
        .orElseThrow(() -> new NotFoundException(GOAL_NOT_FOUND));
  }

  private FinancialGoal requireOwnedGoalForUpdate(UUID userId, UUID goalId) {
    return financialGoalRepository
        .findByIdAndUserIdForUpdate(goalId, userId)
        .orElseThrow(() -> new NotFoundException(GOAL_NOT_FOUND));
  }

  private static void assertActive(FinancialGoal goal, String message) {
    if (goal.getStatus() != FinancialGoalStatus.ACTIVE) {
      throw new BusinessRuleException(message);
    }
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
