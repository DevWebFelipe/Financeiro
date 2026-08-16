package br.com.financialcontrol.credit_card_invoices;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_card_invoices.dto.CreateInvoiceAdjustmentRequest;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoiceAdjustmentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoicePaymentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.PayInvoiceRequest;
import br.com.financialcontrol.credit_cards.CreditCard;
import br.com.financialcontrol.credit_cards.CreditCardCredit;
import br.com.financialcontrol.credit_cards.CreditCardCreditApplication;
import br.com.financialcontrol.credit_cards.CreditCardCreditApplicationRepository;
import br.com.financialcontrol.credit_cards.CreditCardCreditOrigin;
import br.com.financialcontrol.credit_cards.CreditCardCreditRepository;
import br.com.financialcontrol.credit_cards.CreditCardCycleCalculator;
import br.com.financialcontrol.credit_cards.CreditCardService;
import br.com.financialcontrol.credit_cards.dto.CreateCreditCardCreditRequest;
import br.com.financialcontrol.credit_cards.dto.CreditCardCreditResponse;
import br.com.financialcontrol.expenses.AdjustmentStatus;
import br.com.financialcontrol.expenses.AdjustmentType;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseRepository;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.InstallmentBalanceService;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditCardInvoiceService {

  static final String INVOICE_NOT_FOUND = "Fatura não encontrada.";
  static final String INVOICE_PAYMENT_NOT_FOUND = "Pagamento da fatura não encontrado.";
  static final String INVOICE_ADJUSTMENT_NOT_FOUND = "Ajuste da fatura não encontrado.";
  static final String INVOICE_NOT_PAYABLE = "A fatura não aceita pagamento.";
  static final String INVOICE_PAID_IMMUTABLE = "Fatura paga não pode ser alterada.";
  static final String INVOICE_CLOSED_NO_PURCHASE = "Fatura fechada não recebe novas compras.";
  static final String PAYMENT_EXCEEDS_REMAINING =
      "O pagamento não pode ultrapassar o saldo da fatura.";
  static final String INSUFFICIENT_BALANCE =
      "O pagamento não pode exceder o saldo disponível da conta.";
  static final String PAYMENT_ALREADY_REVERSED = "O pagamento da fatura já está estornado.";
  static final String ADJUSTMENT_ALREADY_REVERSED = "O ajuste da fatura já está estornado.";
  static final String DISCOUNT_EXCEEDS_REMAINING =
      "O desconto não pode ultrapassar o saldo da fatura.";
  static final String SURCHARGE_REQUIRES_REMAINING =
      "O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto.";
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  private final CreditCardInvoiceRepository invoiceRepository;
  private final CreditCardInvoicePaymentRepository invoicePaymentRepository;
  private final CreditCardInvoicePaymentAllocationRepository paymentAllocationRepository;
  private final CreditCardInvoiceAdjustmentRepository invoiceAdjustmentRepository;
  private final CreditCardInvoiceAdjustmentAllocationRepository adjustmentAllocationRepository;
  private final CreditCardCreditRepository creditRepository;
  private final CreditCardCreditApplicationRepository creditApplicationRepository;
  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final ExpenseRepository expenseRepository;
  private final InstallmentBalanceService installmentBalanceService;
  private final CreditCardService creditCardService;
  private final AccountService accountService;
  private final Clock clock;

  public CreditCardInvoiceService(
      CreditCardInvoiceRepository invoiceRepository,
      CreditCardInvoicePaymentRepository invoicePaymentRepository,
      CreditCardInvoicePaymentAllocationRepository paymentAllocationRepository,
      CreditCardInvoiceAdjustmentRepository invoiceAdjustmentRepository,
      CreditCardInvoiceAdjustmentAllocationRepository adjustmentAllocationRepository,
      CreditCardCreditRepository creditRepository,
      CreditCardCreditApplicationRepository creditApplicationRepository,
      ExpenseInstallmentRepository expenseInstallmentRepository,
      ExpenseRepository expenseRepository,
      InstallmentBalanceService installmentBalanceService,
      CreditCardService creditCardService,
      AccountService accountService,
      Clock clock) {
    this.invoiceRepository = invoiceRepository;
    this.invoicePaymentRepository = invoicePaymentRepository;
    this.paymentAllocationRepository = paymentAllocationRepository;
    this.invoiceAdjustmentRepository = invoiceAdjustmentRepository;
    this.adjustmentAllocationRepository = adjustmentAllocationRepository;
    this.creditRepository = creditRepository;
    this.creditApplicationRepository = creditApplicationRepository;
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.expenseRepository = expenseRepository;
    this.installmentBalanceService = installmentBalanceService;
    this.creditCardService = creditCardService;
    this.accountService = accountService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<CreditCardInvoiceResponse> listByCard(
      AuthenticatedUser authenticatedUser,
      UUID cardId,
      Integer year,
      Integer month,
      CreditCardInvoiceStatus status) {
    CreditCard card = creditCardService.requireOwned(authenticatedUser.userId(), cardId);
    return invoiceRepository
        .searchByCard(card.getId(), card.getUserId(), year, month, status)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public CreditCardInvoiceResponse get(AuthenticatedUser authenticatedUser, UUID invoiceId) {
    return toResponse(requireOwned(authenticatedUser.userId(), invoiceId));
  }

  @Transactional(readOnly = true)
  public CreditCardInvoiceResponse current(AuthenticatedUser authenticatedUser, UUID cardId) {
    CreditCard card = creditCardService.requireOwned(authenticatedUser.userId(), cardId);
    CreditCardInvoice invoice =
        invoiceRepository
            .findFirstByCreditCard_IdAndUserIdAndStatus(
                card.getId(), card.getUserId(), CreditCardInvoiceStatus.OPEN)
            .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
    return toResponse(invoice);
  }

  @Transactional(readOnly = true)
  public List<InvoicePaymentResponse> listPayments(
      AuthenticatedUser authenticatedUser, UUID invoiceId) {
    CreditCardInvoice invoice = requireOwned(authenticatedUser.userId(), invoiceId);
    return invoicePaymentRepository
        .findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(invoice.getId(), invoice.getUserId())
        .stream()
        .map(InvoicePaymentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InvoiceAdjustmentResponse> listAdjustments(
      AuthenticatedUser authenticatedUser, UUID invoiceId) {
    CreditCardInvoice invoice = requireOwned(authenticatedUser.userId(), invoiceId);
    return invoiceAdjustmentRepository
        .findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(invoice.getId(), invoice.getUserId())
        .stream()
        .map(InvoiceAdjustmentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ExpenseInstallmentResponse> listItems(
      AuthenticatedUser authenticatedUser, UUID invoiceId) {
    CreditCardInvoice invoice = requireOwned(authenticatedUser.userId(), invoiceId);
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    return expenseInstallmentRepository
        .findAllByInvoice_IdAndUserIdOrderByDueDateAscIdAsc(invoice.getId(), invoice.getUserId())
        .stream()
        .map(
            installment -> {
              BigDecimal remaining = installmentBalanceService.remaining(installment);
              boolean overdue =
                  installment.getExpense().getStatus() != ExpenseStatus.CANCELLED
                      && installment.getExpense().getStatus() != ExpenseStatus.REFUNDED
                      && (installment.getStatus() == ExpenseStatus.OPEN
                          || installment.getStatus() == ExpenseStatus.PARTIALLY_PAID)
                      && remaining.compareTo(BigDecimal.ZERO) > 0
                      && installment.getDueDate().isBefore(today);
              return ExpenseInstallmentResponse.from(installment, remaining, overdue);
            })
        .toList();
  }

  public void refreshOperationalState(CreditCardInvoice invoice) {
    CreditCardInvoice locked =
        invoiceRepository
            .findByIdAndUserIdForUpdate(invoice.getId(), invoice.getUserId())
            .orElse(null);
    if (locked == null) {
      return;
    }
    markPaidIfClosedAndZero(locked, Instant.now(clock));
  }

  public CreditCardInvoice requireInvoiceForPurchase(
      CreditCard card, LocalDate purchaseDate, int installmentNumber) {
    LocalDate closingDate =
        CreditCardCycleCalculator.closingDateForInstallment(
            purchaseDate, card.getClosingDay(), installmentNumber);
    LocalDate dueDate =
        CreditCardCycleCalculator.dueDate(closingDate, card.getClosingDay(), card.getDueDay());
    YearMonth reference = YearMonth.from(closingDate);
    CreditCardInvoice existing =
        invoiceRepository
            .findByCardAndCycleForUpdate(
                card.getId(), card.getUserId(), reference.getYear(), reference.getMonthValue())
            .orElse(null);
    if (existing != null) {
      if (existing.getStatus() == CreditCardInvoiceStatus.PAID
          || existing.getStatus() == CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT) {
        throw new BusinessRuleException(INVOICE_PAID_IMMUTABLE);
      }
      if (existing.getStatus() == CreditCardInvoiceStatus.CLOSED) {
        throw new BusinessRuleException(INVOICE_CLOSED_NO_PURCHASE);
      }
      return existing;
    }
    Instant now = Instant.now(clock);
    CreditCardInvoice invoice = new CreditCardInvoice();
    invoice.setId(UuidV7.create());
    invoice.setUserId(card.getUserId());
    invoice.setCreditCard(card);
    invoice.setReferenceYear(reference.getYear());
    invoice.setReferenceMonth(reference.getMonthValue());
    invoice.setClosingDate(closingDate);
    invoice.setDueDate(dueDate);
    invoice.setStatus(resolveNewInvoiceStatus(card, closingDate));
    invoice.setCreatedAt(now);
    invoice.setUpdatedAt(now);
    return invoiceRepository.save(invoice);
  }

  @Transactional
  public InvoicePaymentResponse pay(
      AuthenticatedUser authenticatedUser, UUID invoiceId, PayInvoiceRequest request) {
    UUID userId = authenticatedUser.userId();
    CreditCardInvoice invoice = requireOwnedForUpdate(userId, invoiceId);
    assertInvoicePayable(invoice);
    CreditCard card =
        creditCardService.requireOwnedForUpdate(userId, invoice.getCreditCard().getId());
    Account account =
        accountService.requireActiveOwnedAccountForUpdate(userId, request.accountId());
    BigDecimal amount = normalize(request.amount());
    BigDecimal remaining = remainingAmount(invoice);
    if (amount.compareTo(remaining) > 0) {
      throw new BusinessRuleException(PAYMENT_EXCEEDS_REMAINING);
    }
    if (amount.compareTo(accountService.calculateCurrentBalance(account)) > 0) {
      throw new BusinessRuleException(INSUFFICIENT_BALANCE);
    }
    Instant now = Instant.now(clock);
    CreditCardInvoicePayment payment = new CreditCardInvoicePayment();
    payment.setId(UuidV7.create());
    payment.setUserId(userId);
    payment.setInvoice(invoice);
    payment.setAccount(account);
    payment.setAmount(amount);
    payment.setPaymentDate(request.paymentDate());
    payment.setNotes(request.notes());
    payment.setStatus(InvoicePaymentStatus.ACTIVE);
    payment.setCreatedAt(now);
    invoicePaymentRepository.save(payment);
    persistPaymentAllocations(payment, allocateOnInvoice(invoice, amount), now);
    refreshInstallmentsOfInvoice(invoice, now);
    markPaidIfClosedAndZero(invoice, now);
    applyAvailableCredits(card);
    return InvoicePaymentResponse.from(payment);
  }

  @Transactional
  public InvoicePaymentResponse reversePayment(
      AuthenticatedUser authenticatedUser, UUID invoiceId, UUID paymentId) {
    UUID userId = authenticatedUser.userId();
    CreditCardInvoice invoice = requireOwnedForUpdate(userId, invoiceId);
    if (isTerminalImmutable(invoice.getStatus())) {
      throw new BusinessRuleException(INVOICE_PAID_IMMUTABLE);
    }
    CreditCard card =
        creditCardService.requireOwnedForUpdate(userId, invoice.getCreditCard().getId());
    CreditCardInvoicePayment payment =
        invoicePaymentRepository
            .findByIdAndInvoice_IdAndUserIdForUpdate(paymentId, invoiceId, userId)
            .orElseThrow(() -> new NotFoundException(INVOICE_PAYMENT_NOT_FOUND));
    if (payment.getStatus() != InvoicePaymentStatus.ACTIVE) {
      throw new BusinessRuleException(PAYMENT_ALREADY_REVERSED);
    }
    Instant now = Instant.now(clock);
    payment.setStatus(InvoicePaymentStatus.REVERSED);
    invoicePaymentRepository.save(payment);
    refreshInstallmentsOfInvoice(invoice, now);
    // RN246: reverse restores liquidatable remaining; reapply idle credits in the same transaction.
    applyAvailableCredits(card);
    return InvoicePaymentResponse.from(payment);
  }

  @Transactional
  public InvoiceAdjustmentResponse createAdjustment(
      AuthenticatedUser authenticatedUser, UUID invoiceId, CreateInvoiceAdjustmentRequest request) {
    UUID userId = authenticatedUser.userId();
    CreditCardInvoice invoice = requireOwnedForUpdate(userId, invoiceId);
    if (isTerminalImmutable(invoice.getStatus())) {
      throw new BusinessRuleException(INVOICE_PAID_IMMUTABLE);
    }
    CreditCard card =
        creditCardService.requireOwnedForUpdate(userId, invoice.getCreditCard().getId());
    BigDecimal amount = normalize(request.amount());
    BigDecimal remaining = remainingAmount(invoice);
    if (request.type() == AdjustmentType.DISCOUNT && amount.compareTo(remaining) > 0) {
      throw new BusinessRuleException(DISCOUNT_EXCEEDS_REMAINING);
    }
    if (request.type() == AdjustmentType.SURCHARGE && remaining.compareTo(BigDecimal.ZERO) == 0) {
      throw new BusinessRuleException(SURCHARGE_REQUIRES_REMAINING);
    }
    Instant now = Instant.now(clock);
    CreditCardInvoiceAdjustment adjustment = new CreditCardInvoiceAdjustment();
    adjustment.setId(UuidV7.create());
    adjustment.setUserId(userId);
    adjustment.setInvoice(invoice);
    adjustment.setType(request.type());
    adjustment.setAmount(amount);
    adjustment.setReason(request.reason());
    adjustment.setStatus(AdjustmentStatus.ACTIVE);
    adjustment.setCreatedAt(now);
    invoiceAdjustmentRepository.save(adjustment);
    persistAdjustmentAllocations(adjustment, allocateOnInvoice(invoice, amount), now);
    refreshInstallmentsOfInvoice(invoice, now);
    markPaidIfClosedAndZero(invoice, now);
    applyAvailableCredits(card);
    return InvoiceAdjustmentResponse.from(adjustment);
  }

  @Transactional
  public InvoiceAdjustmentResponse reverseAdjustment(
      AuthenticatedUser authenticatedUser, UUID invoiceId, UUID adjustmentId) {
    UUID userId = authenticatedUser.userId();
    CreditCardInvoice invoice = requireOwnedForUpdate(userId, invoiceId);
    if (isTerminalImmutable(invoice.getStatus())) {
      throw new BusinessRuleException(INVOICE_PAID_IMMUTABLE);
    }
    CreditCard card =
        creditCardService.requireOwnedForUpdate(userId, invoice.getCreditCard().getId());
    CreditCardInvoiceAdjustment adjustment =
        invoiceAdjustmentRepository
            .findOwned(adjustmentId, invoiceId, userId)
            .orElseThrow(() -> new NotFoundException(INVOICE_ADJUSTMENT_NOT_FOUND));
    if (adjustment.getStatus() != AdjustmentStatus.ACTIVE) {
      throw new BusinessRuleException(ADJUSTMENT_ALREADY_REVERSED);
    }
    Instant now = Instant.now(clock);
    adjustment.setStatus(AdjustmentStatus.REVERSED);
    invoiceAdjustmentRepository.save(adjustment);
    refreshInstallmentsOfInvoice(invoice, now);
    // RN246: reverse restores liquidatable remaining; reapply idle credits in the same transaction.
    applyAvailableCredits(card);
    return InvoiceAdjustmentResponse.from(adjustment);
  }

  @Transactional
  public CreditCardCreditResponse createManualCredit(
      AuthenticatedUser authenticatedUser, UUID cardId, CreateCreditCardCreditRequest request) {
    CreditCard card = creditCardService.requireOwnedForUpdate(authenticatedUser.userId(), cardId);
    CreditCardCredit credit =
        persistCredit(
            card,
            normalize(request.amount()),
            request.reason(),
            CreditCardCreditOrigin.MANUAL,
            null);
    applyAvailableCredits(card);
    BigDecimal unused =
        normalize(
            credit
                .getAmount()
                .subtract(
                    zeroIfNull(
                        creditApplicationRepository.sumAmountByCreditIdAndUserId(
                            credit.getId(), credit.getUserId()))));
    return CreditCardCreditResponse.from(credit, unused);
  }

  public CreditCardCredit persistCredit(
      CreditCard card,
      BigDecimal amount,
      String reason,
      CreditCardCreditOrigin origin,
      Expense expense) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    CreditCardCredit credit = new CreditCardCredit();
    credit.setId(UuidV7.create());
    credit.setUserId(card.getUserId());
    credit.setCreditCard(card);
    credit.setAmount(normalize(amount));
    credit.setReason(reason);
    credit.setOrigin(origin);
    credit.setExpense(expense);
    credit.setCreatedAt(Instant.now(clock));
    return creditRepository.save(credit);
  }

  public void applyAvailableCredits(CreditCard card) {
    List<CreditCardCredit> credits =
        creditRepository.findAllByCardForUpdate(card.getId(), card.getUserId());
    List<CreditCardInvoice> invoices =
        invoiceRepository.findAllByCreditCard_IdAndUserIdAndStatusInOrderByDueDateAscIdAsc(
            card.getId(),
            card.getUserId(),
            List.of(CreditCardInvoiceStatus.OPEN, CreditCardInvoiceStatus.CLOSED));
    Instant now = Instant.now(clock);
    for (CreditCardCredit credit : credits) {
      BigDecimal unused =
          normalize(
              credit
                  .getAmount()
                  .subtract(
                      zeroIfNull(
                          creditApplicationRepository.sumAmountByCreditIdAndUserId(
                              credit.getId(), credit.getUserId()))));
      if (unused.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      for (CreditCardInvoice invoice : invoices) {
        if (isTerminalImmutable(invoice.getStatus())) {
          continue;
        }
        BigDecimal invoiceRemaining = remainingAmount(invoice);
        if (invoiceRemaining.compareTo(BigDecimal.ZERO) <= 0) {
          markPaidIfClosedAndZero(invoice, now);
          continue;
        }
        BigDecimal apply = unused.min(invoiceRemaining);
        persistCreditAllocations(credit, invoice, allocateOnInvoice(invoice, apply), now);
        unused = unused.subtract(apply);
        refreshInstallmentsOfInvoice(invoice, now);
        markPaidIfClosedAndZero(invoice, now);
        if (unused.compareTo(BigDecimal.ZERO) <= 0) {
          break;
        }
      }
    }
  }

  @Transactional
  public void closeDueInvoices() {
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    Instant now = Instant.now(clock);
    Set<CardRef> touchedCards = new LinkedHashSet<>();
    List<CreditCardInvoice> open = invoiceRepository.findAllByStatus(CreditCardInvoiceStatus.OPEN);
    for (CreditCardInvoice invoice : open) {
      CreditCardInvoice locked =
          invoiceRepository
              .findByIdAndUserIdForUpdate(invoice.getId(), invoice.getUserId())
              .orElse(null);
      if (locked == null || locked.getStatus() != CreditCardInvoiceStatus.OPEN) {
        continue;
      }
      if (!today.isBefore(locked.getClosingDate())) {
        BigDecimal remaining = remainingAmount(locked);
        locked.setStatus(
            remaining.compareTo(BigDecimal.ZERO) == 0
                ? CreditCardInvoiceStatus.PAID
                : CreditCardInvoiceStatus.CLOSED);
        locked.setUpdatedAt(now);
        invoiceRepository.save(locked);
        touchedCards.add(new CardRef(locked.getCreditCard().getId(), locked.getUserId()));
      }
    }
    List<CreditCardInvoice> closed =
        invoiceRepository.findAllByStatus(CreditCardInvoiceStatus.CLOSED);
    for (CreditCardInvoice invoice : closed) {
      CreditCardInvoice locked =
          invoiceRepository
              .findByIdAndUserIdForUpdate(invoice.getId(), invoice.getUserId())
              .orElse(null);
      if (locked == null || locked.getStatus() != CreditCardInvoiceStatus.CLOSED) {
        continue;
      }
      if (remainingAmount(locked).compareTo(BigDecimal.ZERO) == 0) {
        locked.setStatus(CreditCardInvoiceStatus.PAID);
        locked.setUpdatedAt(now);
        invoiceRepository.save(locked);
        touchedCards.add(new CardRef(locked.getCreditCard().getId(), locked.getUserId()));
      }
    }
    List<CreditCardInvoice> scheduled =
        invoiceRepository.findAllByStatus(CreditCardInvoiceStatus.SCHEDULED);
    for (CreditCardInvoice invoice : scheduled) {
      CreditCardInvoice locked =
          invoiceRepository
              .findByIdAndUserIdForUpdate(invoice.getId(), invoice.getUserId())
              .orElse(null);
      if (locked == null || locked.getStatus() != CreditCardInvoiceStatus.SCHEDULED) {
        continue;
      }
      if (cycleHasStarted(locked, today)
          && !invoiceRepository.existsByCreditCard_IdAndUserIdAndStatus(
              locked.getCreditCard().getId(), locked.getUserId(), CreditCardInvoiceStatus.OPEN)) {
        locked.setStatus(CreditCardInvoiceStatus.OPEN);
        locked.setUpdatedAt(now);
        invoiceRepository.save(locked);
        touchedCards.add(new CardRef(locked.getCreditCard().getId(), locked.getUserId()));
      }
    }
    // RN246: after close/open, reapply idle credits to newly eligible OPEN/CLOSED invoices.
    for (CardRef cardRef : touchedCards) {
      CreditCard card = creditCardService.requireOwnedForUpdate(cardRef.userId(), cardRef.cardId());
      applyAvailableCredits(card);
    }
  }

  private record CardRef(UUID cardId, UUID userId) {}

  public CreditCardInvoiceResponse toResponse(CreditCardInvoice invoice) {
    return CreditCardInvoiceResponse.from(
        invoice, totalAmount(invoice), paidAmount(invoice), remainingAmount(invoice));
  }

  public BigDecimal remainingAmount(CreditCardInvoice invoice) {
    return expenseInstallmentRepository
        .findAllByInvoice_IdAndUserIdOrderByDueDateAscIdAsc(invoice.getId(), invoice.getUserId())
        .stream()
        .filter(this::countsTowardInvoice)
        .map(installmentBalanceService::remaining)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  public void refreshInstallmentsOfInvoice(CreditCardInvoice invoice, Instant now) {
    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByInvoiceIdAndUserIdForUpdate(
            invoice.getId(), invoice.getUserId());
    for (ExpenseInstallment installment : installments) {
      refreshExpenseOf(installment.getExpense(), now);
    }
  }

  public void refreshExpenseOf(Expense expense, Instant now) {
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      return;
    }
    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.getId(), expense.getUserId());
    for (ExpenseInstallment installment : installments) {
      if (installment.getStatus() == ExpenseStatus.CANCELLED
          || installment.getStatus() == ExpenseStatus.REFUNDED) {
        continue;
      }
      installment.setStatus(resolveInstallmentStatus(installment));
      installment.setUpdatedAt(now);
    }
    expense.setStatus(aggregateExpenseStatus(installments));
    expense.setUpdatedAt(now);
    expenseRepository.save(expense);
    expenseInstallmentRepository.saveAll(installments);
  }

  private ExpenseStatus resolveInstallmentStatus(ExpenseInstallment installment) {
    BigDecimal remaining = installmentBalanceService.remaining(installment);
    if (remaining.compareTo(BigDecimal.ZERO) == 0) {
      return ExpenseStatus.PAID;
    }
    BigDecimal obligation = installmentBalanceService.obligation(installment);
    if (remaining.compareTo(obligation) < 0) {
      return ExpenseStatus.PARTIALLY_PAID;
    }
    return ExpenseStatus.OPEN;
  }

  private static ExpenseStatus aggregateExpenseStatus(List<ExpenseInstallment> installments) {
    boolean allPaid = installments.stream().allMatch(i -> i.getStatus() == ExpenseStatus.PAID);
    if (allPaid) {
      return ExpenseStatus.PAID;
    }
    boolean allOpen = installments.stream().allMatch(i -> i.getStatus() == ExpenseStatus.OPEN);
    if (allOpen) {
      return ExpenseStatus.OPEN;
    }
    boolean anySettled =
        installments.stream()
            .anyMatch(
                i ->
                    i.getStatus() == ExpenseStatus.PAID
                        || i.getStatus() == ExpenseStatus.PARTIALLY_PAID
                        || i.getStatus() == ExpenseStatus.REFUNDED);
    if (anySettled) {
      return ExpenseStatus.PARTIALLY_PAID;
    }
    return ExpenseStatus.OPEN;
  }

  private List<InvoiceAllocationCalculator.Share> allocateOnInvoice(
      CreditCardInvoice invoice, BigDecimal amount) {
    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByInvoiceIdAndUserIdForUpdate(
            invoice.getId(), invoice.getUserId());
    List<InvoiceAllocationCalculator.Line> lines = new ArrayList<>();
    for (ExpenseInstallment installment : installments) {
      if (!countsTowardInvoice(installment)) {
        continue;
      }
      lines.add(
          new InvoiceAllocationCalculator.Line(
              installment.getId(),
              installment.getDueDate(),
              installmentBalanceService.remaining(installment)));
    }
    return InvoiceAllocationCalculator.allocate(amount, lines);
  }

  private void persistPaymentAllocations(
      CreditCardInvoicePayment payment,
      List<InvoiceAllocationCalculator.Share> shares,
      Instant now) {
    for (InvoiceAllocationCalculator.Share share : shares) {
      ExpenseInstallment installment =
          expenseInstallmentRepository.findById(share.installmentId()).orElseThrow();
      CreditCardInvoicePaymentAllocation allocation = new CreditCardInvoicePaymentAllocation();
      allocation.setId(UuidV7.create());
      allocation.setUserId(payment.getUserId());
      allocation.setInvoicePayment(payment);
      allocation.setInstallment(installment);
      allocation.setAmount(share.amount());
      allocation.setCreatedAt(now);
      paymentAllocationRepository.save(allocation);
    }
  }

  private void persistAdjustmentAllocations(
      CreditCardInvoiceAdjustment adjustment,
      List<InvoiceAllocationCalculator.Share> shares,
      Instant now) {
    for (InvoiceAllocationCalculator.Share share : shares) {
      ExpenseInstallment installment =
          expenseInstallmentRepository.findById(share.installmentId()).orElseThrow();
      CreditCardInvoiceAdjustmentAllocation allocation =
          new CreditCardInvoiceAdjustmentAllocation();
      allocation.setId(UuidV7.create());
      allocation.setUserId(adjustment.getUserId());
      allocation.setInvoiceAdjustment(adjustment);
      allocation.setInstallment(installment);
      allocation.setAmount(share.amount());
      allocation.setCreatedAt(now);
      adjustmentAllocationRepository.save(allocation);
    }
  }

  private void persistCreditAllocations(
      CreditCardCredit credit,
      CreditCardInvoice invoice,
      List<InvoiceAllocationCalculator.Share> shares,
      Instant now) {
    for (InvoiceAllocationCalculator.Share share : shares) {
      ExpenseInstallment installment =
          expenseInstallmentRepository.findById(share.installmentId()).orElseThrow();
      CreditCardCreditApplication application = new CreditCardCreditApplication();
      application.setId(UuidV7.create());
      application.setUserId(credit.getUserId());
      application.setCredit(credit);
      application.setInvoice(invoice);
      application.setInstallment(installment);
      application.setAmount(share.amount());
      application.setCreatedAt(now);
      creditApplicationRepository.save(application);
    }
  }

  private BigDecimal totalAmount(CreditCardInvoice invoice) {
    return expenseInstallmentRepository
        .findAllByInvoice_IdAndUserIdOrderByDueDateAscIdAsc(invoice.getId(), invoice.getUserId())
        .stream()
        .filter(this::countsTowardInvoice)
        .map(ExpenseInstallment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal paidAmount(CreditCardInvoice invoice) {
    return invoicePaymentRepository
        .findAllByInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(invoice.getId(), invoice.getUserId())
        .stream()
        .filter(payment -> payment.getStatus() == InvoicePaymentStatus.ACTIVE)
        .map(CreditCardInvoicePayment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private boolean countsTowardInvoice(ExpenseInstallment installment) {
    return installment.getStatus() != ExpenseStatus.CANCELLED
        && installment.getStatus() != ExpenseStatus.REFUNDED;
  }

  private CreditCardInvoiceStatus resolveNewInvoiceStatus(CreditCard card, LocalDate closingDate) {
    boolean first = !invoiceRepository.existsByCreditCard(card);
    if (first) {
      return CreditCardInvoiceStatus.OPEN;
    }
    if (invoiceRepository.existsByCreditCard_IdAndUserIdAndStatus(
        card.getId(), card.getUserId(), CreditCardInvoiceStatus.OPEN)) {
      return CreditCardInvoiceStatus.SCHEDULED;
    }
    LocalDate today = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    if (today.isBefore(closingDate)) {
      return CreditCardInvoiceStatus.OPEN;
    }
    return CreditCardInvoiceStatus.SCHEDULED;
  }

  private boolean cycleHasStarted(CreditCardInvoice scheduled, LocalDate today) {
    List<CreditCardInvoice> invoices =
        invoiceRepository.findAllByCreditCard_IdAndUserIdOrderByClosingDateAscIdAsc(
            scheduled.getCreditCard().getId(), scheduled.getUserId());
    CreditCardInvoice previous = null;
    for (CreditCardInvoice invoice : invoices) {
      if (invoice.getId().equals(scheduled.getId())) {
        break;
      }
      previous = invoice;
    }
    if (previous == null) {
      return !today.isBefore(scheduled.getClosingDate());
    }
    return !today.isBefore(previous.getClosingDate());
  }

  private void markPaidIfClosedAndZero(CreditCardInvoice invoice, Instant now) {
    if (invoice.getStatus() != CreditCardInvoiceStatus.CLOSED) {
      return;
    }
    if (remainingAmount(invoice).compareTo(BigDecimal.ZERO) == 0) {
      invoice.setStatus(CreditCardInvoiceStatus.PAID);
      invoice.setUpdatedAt(now);
      invoiceRepository.save(invoice);
    }
  }

  private void assertInvoicePayable(CreditCardInvoice invoice) {
    if (isTerminalImmutable(invoice.getStatus())) {
      throw new BusinessRuleException(INVOICE_PAID_IMMUTABLE);
    }
    if (invoice.getStatus() == CreditCardInvoiceStatus.SCHEDULED) {
      throw new BusinessRuleException(INVOICE_NOT_PAYABLE);
    }
  }

  private static boolean isTerminalImmutable(CreditCardInvoiceStatus status) {
    return status == CreditCardInvoiceStatus.PAID
        || status == CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT;
  }

  private CreditCardInvoice requireOwned(UUID userId, UUID invoiceId) {
    return invoiceRepository
        .findByIdAndUserId(invoiceId, userId)
        .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
  }

  private CreditCardInvoice requireOwnedForUpdate(UUID userId, UUID invoiceId) {
    return invoiceRepository
        .findByIdAndUserIdForUpdate(invoiceId, userId)
        .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
