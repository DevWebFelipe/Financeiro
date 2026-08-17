package br.com.financialcontrol.transfers;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.accounts.AccountType;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.transfers.dto.CreateTransferRequest;
import br.com.financialcontrol.transfers.dto.TransferResponse;
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
public class TransferService {

  static final String TRANSFER_NOT_FOUND = "Transferência não encontrada.";
  static final String SAME_ACCOUNT = "A conta de origem e a conta de destino devem ser diferentes.";
  static final String ONLY_BANK_ACCOUNT =
      "Somente contas bancárias (BANK_ACCOUNT) podem participar de transferências.";
  static final String INSUFFICIENT_BALANCE = "Saldo insuficiente para realizar a operação.";
  static final String FUTURE_DATE = "A data da transferência não pode ser futura.";
  static final String ALREADY_REVERSED = "A transferência já está estornada.";
  static final String ONLY_ACTIVE_CAN_BE_REVERSED =
      "Somente transferências ativas podem ser estornadas.";

  private final TransferRepository transferRepository;
  private final AccountService accountService;
  private final Clock clock;

  public TransferService(
      TransferRepository transferRepository, AccountService accountService, Clock clock) {
    this.transferRepository = transferRepository;
    this.accountService = accountService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<TransferResponse> list(
      AuthenticatedUser authenticatedUser, LocalDate startDate, LocalDate endDate, UUID accountId) {
    return transferRepository
        .searchByUser(authenticatedUser.userId(), startDate, endDate, accountId)
        .stream()
        .map(TransferResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public TransferResponse get(AuthenticatedUser authenticatedUser, UUID transferId) {
    return TransferResponse.from(requireOwned(authenticatedUser.userId(), transferId));
  }

  @Transactional
  public TransferResponse create(
      AuthenticatedUser authenticatedUser, CreateTransferRequest request) {
    UUID userId = authenticatedUser.userId();
    if (request.sourceAccountId().equals(request.destinationAccountId())) {
      throw new BusinessRuleException(SAME_ACCOUNT);
    }
    LocalDate today = accountService.today();
    if (request.transferDate().isAfter(today)) {
      throw new BusinessRuleException(FUTURE_DATE);
    }

    Account first;
    Account second;
    // Deterministic lock order by UUID to avoid deadlocks
    if (request.sourceAccountId().compareTo(request.destinationAccountId()) < 0) {
      first = accountService.requireActiveOwnedAccountForUpdate(userId, request.sourceAccountId());
      second =
          accountService.requireActiveOwnedAccountForUpdate(userId, request.destinationAccountId());
    } else {
      first =
          accountService.requireActiveOwnedAccountForUpdate(userId, request.destinationAccountId());
      second = accountService.requireActiveOwnedAccountForUpdate(userId, request.sourceAccountId());
    }
    Account source = first.getId().equals(request.sourceAccountId()) ? first : second;
    Account destination = first.getId().equals(request.destinationAccountId()) ? first : second;

    assertBankAccount(source);
    assertBankAccount(destination);

    BigDecimal amount = normalize(request.amount());
    if (accountService.calculateCurrentBalance(source).compareTo(amount) < 0) {
      throw new BusinessRuleException(INSUFFICIENT_BALANCE);
    }

    Transfer transfer = new Transfer();
    transfer.setId(UuidV7.create());
    transfer.setUserId(userId);
    transfer.setSourceAccount(source);
    transfer.setDestinationAccount(destination);
    transfer.setAmount(amount);
    transfer.setTransferDate(request.transferDate());
    transfer.setDescription(request.description());
    transfer.setStatus(TransferStatus.ACTIVE);
    transfer.setCreatedAt(Instant.now(clock));
    transferRepository.save(transfer);

    accountService.markInitialBalanceLocked(source);
    accountService.markInitialBalanceLocked(destination);

    return TransferResponse.from(transfer);
  }

  @Transactional
  public TransferResponse reverse(AuthenticatedUser authenticatedUser, UUID transferId) {
    UUID userId = authenticatedUser.userId();
    Transfer transfer =
        transferRepository
            .findByIdAndUserIdForUpdate(transferId, userId)
            .orElseThrow(() -> new NotFoundException(TRANSFER_NOT_FOUND));
    if (transfer.getStatus() == TransferStatus.REVERSED) {
      throw new BusinessRuleException(ALREADY_REVERSED);
    }
    if (transfer.getStatus() != TransferStatus.ACTIVE) {
      throw new BusinessRuleException(ONLY_ACTIVE_CAN_BE_REVERSED);
    }

    UUID sourceId = transfer.getSourceAccount().getId();
    UUID destinationId = transfer.getDestinationAccount().getId();
    Account first;
    Account second;
    if (sourceId.compareTo(destinationId) < 0) {
      first = accountService.requireOwnedAccountForUpdate(userId, sourceId);
      second = accountService.requireOwnedAccountForUpdate(userId, destinationId);
    } else {
      first = accountService.requireOwnedAccountForUpdate(userId, destinationId);
      second = accountService.requireOwnedAccountForUpdate(userId, sourceId);
    }
    Account destination = first.getId().equals(destinationId) ? first : second;

    // Reverse debits destination (undo credit)
    if (accountService.calculateCurrentBalance(destination).compareTo(transfer.getAmount()) < 0) {
      throw new BusinessRuleException(INSUFFICIENT_BALANCE);
    }

    transfer.setStatus(TransferStatus.REVERSED);
    return TransferResponse.from(transferRepository.save(transfer));
  }

  private Transfer requireOwned(UUID userId, UUID transferId) {
    return transferRepository
        .findByIdAndUserId(transferId, userId)
        .orElseThrow(() -> new NotFoundException(TRANSFER_NOT_FOUND));
  }

  private static void assertBankAccount(Account account) {
    if (account.getType() != AccountType.BANK_ACCOUNT) {
      throw new BusinessRuleException(ONLY_BANK_ACCOUNT);
    }
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
