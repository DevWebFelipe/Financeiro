package br.com.financialcontrol.balance_adjustments;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.balance_adjustments.dto.BalanceAdjustmentResponse;
import br.com.financialcontrol.balance_adjustments.dto.CreateBalanceAdjustmentRequest;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceAdjustmentService {

  static final String ADJUSTMENT_NOT_FOUND = "Acerto de saldo não encontrado.";
  static final String FUTURE_DATE = "A data do acerto não pode ser futura.";
  static final String NEGATIVE_RESULT = "O acerto não pode resultar em saldo negativo da conta.";
  static final String INSUFFICIENT_BALANCE = "Saldo insuficiente para realizar a operação.";
  static final String ALREADY_REVERSED = "O acerto de saldo já está estornado.";
  static final String ONLY_ACTIVE_CAN_BE_REVERSED = "Somente acertos ativos podem ser estornados.";

  private final AccountBalanceAdjustmentRepository adjustmentRepository;
  private final AccountService accountService;
  private final Clock clock;

  public BalanceAdjustmentService(
      AccountBalanceAdjustmentRepository adjustmentRepository,
      AccountService accountService,
      Clock clock) {
    this.adjustmentRepository = adjustmentRepository;
    this.accountService = accountService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<BalanceAdjustmentResponse> list(AuthenticatedUser authenticatedUser, UUID accountId) {
    accountService.requireOwnedAccount(authenticatedUser.userId(), accountId);
    return adjustmentRepository
        .findAllByAccount_IdAndUserIdOrderByAdjustmentDateAscCreatedAtAscIdAsc(
            accountId, authenticatedUser.userId())
        .stream()
        .map(BalanceAdjustmentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public BalanceAdjustmentResponse get(
      AuthenticatedUser authenticatedUser, UUID accountId, UUID adjustmentId) {
    return BalanceAdjustmentResponse.from(
        requireOwned(authenticatedUser.userId(), accountId, adjustmentId));
  }

  @Transactional
  public BalanceAdjustmentResponse create(
      AuthenticatedUser authenticatedUser, UUID accountId, CreateBalanceAdjustmentRequest request) {
    UUID userId = authenticatedUser.userId();
    Account account = accountService.requireActiveOwnedAccountForUpdate(userId, accountId);

    LocalDate today = accountService.today();
    LocalDate adjustmentDate = request.adjustmentDate() == null ? today : request.adjustmentDate();
    if (adjustmentDate.isAfter(today)) {
      throw new BusinessRuleException(FUTURE_DATE);
    }

    BigDecimal reported = normalize(request.reportedBalance());
    BigDecimal calculated = accountService.calculateBalanceAsOf(account, adjustmentDate);
    BigDecimal adjustmentAmount = normalize(reported.subtract(calculated));

    // Resulting balance on that date after adjustment must be >= 0 (reported is the result)
    if (reported.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessRuleException(NEGATIVE_RESULT);
    }

    // Current balance after applying this adjustment (as-of today includes this fact if date <=
    // today)
    // Also ensure current balance won't go negative when adjustment is negative and date is
    // today-ish
    BigDecimal currentBefore = accountService.calculateCurrentBalance(account);
    BigDecimal currentAfter = normalize(currentBefore.add(adjustmentAmount));
    if (currentAfter.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessRuleException(NEGATIVE_RESULT);
    }

    Instant now = Instant.now(clock);
    AccountBalanceAdjustment adjustment = new AccountBalanceAdjustment();
    adjustment.setId(UuidV7.create());
    adjustment.setUserId(userId);
    adjustment.setAccount(account);
    adjustment.setAdjustmentDate(adjustmentDate);
    adjustment.setCalculatedBalance(calculated);
    adjustment.setReportedBalance(reported);
    adjustment.setAdjustmentAmount(adjustmentAmount);
    adjustment.setStatus(BalanceAdjustmentStatus.ACTIVE);
    adjustment.setCreatedAt(now);
    adjustment.setUpdatedAt(now);
    adjustmentRepository.save(adjustment);

    accountService.markInitialBalanceLocked(account);
    return BalanceAdjustmentResponse.from(adjustment);
  }

  @Transactional
  public BalanceAdjustmentResponse reverse(
      AuthenticatedUser authenticatedUser, UUID accountId, UUID adjustmentId) {
    UUID userId = authenticatedUser.userId();
    Account account = accountService.requireOwnedAccountForUpdate(userId, accountId);
    AccountBalanceAdjustment adjustment =
        adjustmentRepository
            .findByIdAndAccount_IdAndUserIdForUpdate(adjustmentId, accountId, userId)
            .orElseThrow(() -> new NotFoundException(ADJUSTMENT_NOT_FOUND));

    if (adjustment.getStatus() == BalanceAdjustmentStatus.REVERSED) {
      throw new BusinessRuleException(ALREADY_REVERSED);
    }
    if (adjustment.getStatus() != BalanceAdjustmentStatus.ACTIVE) {
      throw new BusinessRuleException(ONLY_ACTIVE_CAN_BE_REVERSED);
    }

    // Inverse effect: if original was +, reverse is - (debit)
    BigDecimal inverse = adjustment.getAdjustmentAmount().negate();
    if (inverse.compareTo(BigDecimal.ZERO) < 0) {
      BigDecimal debit = inverse.abs();
      if (accountService.calculateCurrentBalance(account).compareTo(debit) < 0) {
        throw new BusinessRuleException(INSUFFICIENT_BALANCE);
      }
    }

    adjustment.setStatus(BalanceAdjustmentStatus.REVERSED);
    adjustment.setUpdatedAt(Instant.now(clock));
    return BalanceAdjustmentResponse.from(adjustmentRepository.save(adjustment));
  }

  private AccountBalanceAdjustment requireOwned(UUID userId, UUID accountId, UUID adjustmentId) {
    return adjustmentRepository
        .findByIdAndAccount_IdAndUserId(adjustmentId, accountId, userId)
        .orElseThrow(() -> new NotFoundException(ADJUSTMENT_NOT_FOUND));
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
