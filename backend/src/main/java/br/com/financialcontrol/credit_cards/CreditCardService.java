package br.com.financialcontrol.credit_cards;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_cards.dto.CreateCreditCardRequest;
import br.com.financialcontrol.credit_cards.dto.CreditCardCreditResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardLimitResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.credit_cards.dto.UpdateCreditCardRequest;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.InstallmentBalanceService;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditCardService {

  static final String CARD_NOT_FOUND = "Cartão não encontrado.";
  static final String CARD_INACTIVE = "Somente cartões ativos podem receber novas compras.";

  private final CreditCardRepository creditCardRepository;
  private final CreditCardCreditRepository creditRepository;
  private final CreditCardCreditApplicationRepository creditApplicationRepository;
  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final InstallmentBalanceService installmentBalanceService;
  private final Clock clock;

  public CreditCardService(
      CreditCardRepository creditCardRepository,
      CreditCardCreditRepository creditRepository,
      CreditCardCreditApplicationRepository creditApplicationRepository,
      ExpenseInstallmentRepository expenseInstallmentRepository,
      InstallmentBalanceService installmentBalanceService,
      Clock clock) {
    this.creditCardRepository = creditCardRepository;
    this.creditRepository = creditRepository;
    this.creditApplicationRepository = creditApplicationRepository;
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.installmentBalanceService = installmentBalanceService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<CreditCardResponse> list(AuthenticatedUser authenticatedUser, String holderName) {
    UUID userId = authenticatedUser.userId();
    List<CreditCard> cards =
        holderName == null || holderName.isBlank()
            ? creditCardRepository.findAllByUserIdOrderByCreatedAtAsc(userId)
            : creditCardRepository.findAllByUserIdAndHolderNameIgnoreCaseOrderByCreatedAtAsc(
                userId, holderName.trim());
    return cards.stream().map(CreditCardResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public CreditCardResponse get(AuthenticatedUser authenticatedUser, UUID cardId) {
    return CreditCardResponse.from(requireOwned(authenticatedUser.userId(), cardId));
  }

  @Transactional
  public CreditCardResponse create(
      AuthenticatedUser authenticatedUser, CreateCreditCardRequest request) {
    Instant now = Instant.now(clock);
    CreditCard card = new CreditCard();
    card.setId(UuidV7.create());
    card.setUserId(authenticatedUser.userId());
    card.setName(request.name());
    card.setHolderName(request.holderName());
    card.setLastFourDigits(request.lastFourDigits());
    card.setCreditLimit(normalize(request.creditLimit()));
    card.setClosingDay(request.closingDay());
    card.setDueDay(request.dueDay());
    card.setActive(true);
    card.setCreatedAt(now);
    card.setUpdatedAt(now);
    return CreditCardResponse.from(creditCardRepository.save(card));
  }

  @Transactional
  public CreditCardResponse update(
      AuthenticatedUser authenticatedUser, UUID cardId, UpdateCreditCardRequest request) {
    CreditCard card = requireOwnedForUpdate(authenticatedUser.userId(), cardId);
    card.setName(request.name());
    card.setHolderName(request.holderName());
    card.setLastFourDigits(request.lastFourDigits());
    card.setCreditLimit(normalize(request.creditLimit()));
    card.setClosingDay(request.closingDay());
    card.setDueDay(request.dueDay());
    card.setUpdatedAt(Instant.now(clock));
    return CreditCardResponse.from(creditCardRepository.save(card));
  }

  @Transactional
  public CreditCardResponse deactivate(AuthenticatedUser authenticatedUser, UUID cardId) {
    CreditCard card = requireOwnedForUpdate(authenticatedUser.userId(), cardId);
    card.setActive(false);
    card.setUpdatedAt(Instant.now(clock));
    return CreditCardResponse.from(creditCardRepository.save(card));
  }

  @Transactional
  public CreditCardResponse activate(AuthenticatedUser authenticatedUser, UUID cardId) {
    CreditCard card = requireOwnedForUpdate(authenticatedUser.userId(), cardId);
    card.setActive(true);
    card.setUpdatedAt(Instant.now(clock));
    return CreditCardResponse.from(creditCardRepository.save(card));
  }

  @Transactional(readOnly = true)
  public CreditCardLimitResponse getLimit(AuthenticatedUser authenticatedUser, UUID cardId) {
    CreditCard card = requireOwned(authenticatedUser.userId(), cardId);
    BigDecimal used = usedLimit(card);
    BigDecimal available = normalize(card.getCreditLimit().subtract(used));
    return new CreditCardLimitResponse(card.getCreditLimit(), used, available);
  }

  @Transactional(readOnly = true)
  public List<CreditCardCreditResponse> listCredits(
      AuthenticatedUser authenticatedUser, UUID cardId) {
    CreditCard card = requireOwned(authenticatedUser.userId(), cardId);
    return creditRepository
        .findAllByCreditCard_IdAndUserIdOrderByCreatedAtAscIdAsc(card.getId(), card.getUserId())
        .stream()
        .map(credit -> CreditCardCreditResponse.from(credit, unusedAmount(credit)))
        .toList();
  }

  public CreditCard requireOwned(UUID userId, UUID cardId) {
    return creditCardRepository
        .findByIdAndUserId(cardId, userId)
        .orElseThrow(() -> new NotFoundException(CARD_NOT_FOUND));
  }

  public CreditCard requireOwnedForUpdate(UUID userId, UUID cardId) {
    return creditCardRepository
        .findByIdAndUserIdForUpdate(cardId, userId)
        .orElseThrow(() -> new NotFoundException(CARD_NOT_FOUND));
  }

  public CreditCard requireActiveOwned(UUID userId, UUID cardId) {
    CreditCard card = requireOwnedForUpdate(userId, cardId);
    if (!card.isActive()) {
      throw new BusinessRuleException(CARD_INACTIVE);
    }
    return card;
  }

  BigDecimal usedLimit(CreditCard card) {
    return expenseInstallmentRepository
        .findAllByExpense_CreditCard_IdAndUserId(card.getId(), card.getUserId())
        .stream()
        .filter(this::countsTowardUsedLimit)
        .map(installmentBalanceService::remaining)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private boolean countsTowardUsedLimit(ExpenseInstallment installment) {
    ExpenseStatus expenseStatus = installment.getExpense().getStatus();
    if (expenseStatus == ExpenseStatus.CANCELLED || expenseStatus == ExpenseStatus.REFUNDED) {
      return false;
    }
    ExpenseStatus installmentStatus = installment.getStatus();
    return installmentStatus != ExpenseStatus.CANCELLED
        && installmentStatus != ExpenseStatus.REFUNDED;
  }

  private BigDecimal unusedAmount(CreditCardCredit credit) {
    BigDecimal applied =
        zeroIfNull(
            creditApplicationRepository.sumAmountByCreditIdAndUserId(
                credit.getId(), credit.getUserId()));
    return normalize(credit.getAmount().subtract(applied));
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
