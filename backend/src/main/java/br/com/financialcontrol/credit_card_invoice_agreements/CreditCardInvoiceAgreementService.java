package br.com.financialcontrol.credit_card_invoice_agreements;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementInstallmentResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AnticipateAgreementInstallmentRequest;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.CreateAgreementRequest;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.RenegotiateAgreementRequest;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.InvoiceAllocationCalculator;
import br.com.financialcontrol.credit_card_invoices.dto.PayInvoiceRequest;
import br.com.financialcontrol.credit_cards.CreditCard;
import br.com.financialcontrol.credit_cards.CreditCardService;
import br.com.financialcontrol.expenses.AdjustmentType;
import br.com.financialcontrol.expenses.Expense;
import br.com.financialcontrol.expenses.ExpenseInstallment;
import br.com.financialcontrol.expenses.ExpenseInstallmentRepository;
import br.com.financialcontrol.expenses.ExpenseService;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.InstallmentBalanceService;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.PaymentStatus;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditCardInvoiceAgreementService {

  static final String INVOICE_NOT_FOUND = "Fatura não encontrada.";
  static final String INVOICE_NOT_CLOSED_FOR_AGREEMENT =
      "Somente fatura fechada com saldo em aberto pode ser negociada.";
  static final String ENTRY_EQUALS_REMAINING =
      "Entrada igual ao saldo restante: use o pagamento da fatura.";
  static final String ENTRY_EXCEEDS_REMAINING =
      "A entrada não pode ultrapassar o saldo restante da fatura.";
  static final String AGREEMENT_NOT_FOUND = "Negociação não encontrada.";
  static final String INSTALLMENT_NOT_ON_AGREEMENT = "A parcela não pertence a esta negociação.";
  static final String INSTALLMENT_ALREADY_PAID = "A parcela já está paga.";
  static final String PAYMENT_EXCEEDS_REMAINING =
      "O pagamento não pode ultrapassar o saldo restante da parcela.";
  static final String INSUFFICIENT_BALANCE =
      "O pagamento não pode exceder o saldo disponível da conta.";
  static final String INVOICE_ALREADY_HAS_AGREEMENT = "A fatura já possui negociação.";
  static final String RENEGOTIATION_FUTURES_FINANCIAL_DISCOUNT_REASON =
      "Desconto financeiro por renegociação";
  static final String RENEGOTIATION_FUTURES_INCORPORATION_REASON =
      "Incorporação de saldo à renegociação";
  static final String SETTLED_DISCOUNT_REASON = "Quitação com desconto";
  static final String AGREEMENT_DESCRIPTION = "Negociação de fatura";
  static final String RENEGOTIATION_DESCRIPTION = "Renegociação de fatura";
  static final String AGREEMENT_CONTRACTED_TOTAL_BELOW_FINANCED_AMOUNT =
      "O total contratado não pode ser menor que o valor financiado.";
  static final String ANTICIPATED_FUTURES_NET_EXCEEDS_ORIGINAL =
      "O valor líquido dos futuros não pode ultrapassar o saldo original dos futuros.";
  static final String ANTICIPATED_FUTURES_NET_NEGATIVE =
      "O valor líquido dos futuros não pode ser negativo.";

  private static final EnumSet<CreditCardInvoiceAgreementStatus> BLOCKING_SOURCE_STATUSES =
      EnumSet.of(
          CreditCardInvoiceAgreementStatus.ACTIVE,
          CreditCardInvoiceAgreementStatus.COMPLETED,
          CreditCardInvoiceAgreementStatus.RENEGOTIATED);

  private final CreditCardInvoiceAgreementRepository agreementRepository;
  private final CreditCardInvoiceAgreementSettlementRepository settlementRepository;
  private final CreditCardInvoiceAgreementSettlementAllocationRepository
      settlementAllocationRepository;
  private final CreditCardInvoiceRepository invoiceRepository;
  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final PaymentRepository paymentRepository;
  private final CreditCardInvoiceService creditCardInvoiceService;
  private final CreditCardService creditCardService;
  private final ExpenseService expenseService;
  private final AccountService accountService;
  private final InstallmentBalanceService installmentBalanceService;
  private final Clock clock;

  public CreditCardInvoiceAgreementService(
      CreditCardInvoiceAgreementRepository agreementRepository,
      CreditCardInvoiceAgreementSettlementRepository settlementRepository,
      CreditCardInvoiceAgreementSettlementAllocationRepository settlementAllocationRepository,
      CreditCardInvoiceRepository invoiceRepository,
      ExpenseInstallmentRepository expenseInstallmentRepository,
      PaymentRepository paymentRepository,
      CreditCardInvoiceService creditCardInvoiceService,
      CreditCardService creditCardService,
      ExpenseService expenseService,
      AccountService accountService,
      InstallmentBalanceService installmentBalanceService,
      Clock clock) {
    this.agreementRepository = agreementRepository;
    this.settlementRepository = settlementRepository;
    this.settlementAllocationRepository = settlementAllocationRepository;
    this.invoiceRepository = invoiceRepository;
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.paymentRepository = paymentRepository;
    this.creditCardInvoiceService = creditCardInvoiceService;
    this.creditCardService = creditCardService;
    this.expenseService = expenseService;
    this.accountService = accountService;
    this.installmentBalanceService = installmentBalanceService;
    this.clock = clock;
  }

  @Transactional
  public AgreementResponse createAgreement(
      AuthenticatedUser authenticatedUser, UUID invoiceId, CreateAgreementRequest request) {
    return negotiate(
        authenticatedUser,
        invoiceId,
        request.entryAmount(),
        request.accountId(),
        request.entryPaymentDate(),
        request.installmentCount(),
        request.installmentAmount(),
        null,
        false);
  }

  @Transactional
  public AgreementResponse renegotiate(
      AuthenticatedUser authenticatedUser, UUID invoiceId, RenegotiateAgreementRequest request) {
    return negotiate(
        authenticatedUser,
        invoiceId,
        request.entryAmount(),
        request.accountId(),
        request.entryPaymentDate(),
        request.installmentCount(),
        request.installmentAmount(),
        request.anticipatedFuturesNetAmount(),
        true);
  }

  @Transactional(readOnly = true)
  public List<AgreementResponse> listByInvoice(
      AuthenticatedUser authenticatedUser, UUID invoiceId) {
    CreditCardInvoice invoice = requireOwnedInvoice(authenticatedUser.userId(), invoiceId);
    return agreementRepository
        .findAllBySourceInvoice_IdAndUserIdOrderByCreatedAtAscIdAsc(
            invoice.getId(), invoice.getUserId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public AgreementResponse get(AuthenticatedUser authenticatedUser, UUID agreementId) {
    return toResponse(requireOwned(authenticatedUser.userId(), agreementId));
  }

  @Transactional
  public AgreementResponse anticipate(
      AuthenticatedUser authenticatedUser,
      UUID agreementId,
      UUID installmentId,
      AnticipateAgreementInstallmentRequest request) {
    UUID userId = authenticatedUser.userId();
    CreditCardInvoiceAgreement agreement = requireOwnedForUpdate(userId, agreementId);
    Expense expense = agreement.getExpense();
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserIdForUpdate(installmentId, expense.getId(), userId)
            .orElseThrow(() -> new BusinessRuleException(INSTALLMENT_NOT_ON_AGREEMENT));
    if (installment.getStatus() == ExpenseStatus.PAID) {
      throw new BusinessRuleException(INSTALLMENT_ALREADY_PAID);
    }
    BigDecimal remainingBefore = installmentBalanceService.remaining(installment);
    if (remainingBefore.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException(INSTALLMENT_ALREADY_PAID);
    }
    BigDecimal amount = normalize(request.amount());
    if (amount.compareTo(remainingBefore) > 0) {
      throw new BusinessRuleException(PAYMENT_EXCEEDS_REMAINING);
    }
    Account account =
        accountService.requireActiveOwnedAccountForUpdate(userId, request.accountId());
    if (amount.compareTo(accountService.calculateCurrentBalance(account)) > 0) {
      throw new BusinessRuleException(INSUFFICIENT_BALANCE);
    }

    Instant now = Instant.now(clock);
    Payment payment = new Payment();
    payment.setId(UuidV7.create());
    payment.setUserId(userId);
    payment.setExpense(expense);
    payment.setInstallment(installment);
    payment.setAccount(account);
    payment.setAmount(amount);
    payment.setPaymentDate(request.paymentDate());
    payment.setStatus(PaymentStatus.ACTIVE);
    payment.setType(null);
    payment.setNotes(request.notes());
    payment.setCreatedAt(now);
    paymentRepository.save(payment);

    if (request.isSettled() && amount.compareTo(remainingBefore) < 0) {
      BigDecimal discount = normalize(remainingBefore.subtract(amount));
      expenseService.createAdjustment(
          authenticatedUser,
          expense.getId(),
          installment.getId(),
          AdjustmentType.DISCOUNT,
          discount,
          SETTLED_DISCOUNT_REASON);
    }

    creditCardInvoiceService.refreshExpenseOf(expense, now);
    completeAgreementIfFullyPaid(agreement, now);
    return toResponse(agreement);
  }

  private AgreementResponse negotiate(
      AuthenticatedUser authenticatedUser,
      UUID invoiceId,
      BigDecimal entryAmountRaw,
      UUID accountId,
      java.time.LocalDate entryPaymentDate,
      int installmentCount,
      BigDecimal installmentAmountRaw,
      BigDecimal anticipatedFuturesNetAmountRaw,
      boolean renegotiation) {
    UUID userId = authenticatedUser.userId();
    CreditCardInvoice invoice = requireOwnedInvoiceForUpdate(userId, invoiceId);
    if (invoice.getStatus() != CreditCardInvoiceStatus.CLOSED) {
      throw new BusinessRuleException(INVOICE_NOT_CLOSED_FOR_AGREEMENT);
    }
    if (agreementRepository.existsBySourceInvoice_IdAndUserIdAndStatusIn(
        invoice.getId(), userId, BLOCKING_SOURCE_STATUSES)) {
      throw new BusinessRuleException(INVOICE_ALREADY_HAS_AGREEMENT);
    }

    CreditCard card =
        creditCardService.requireOwnedForUpdate(userId, invoice.getCreditCard().getId());
    BigDecimal invoiceRemaining = creditCardInvoiceService.remainingAmount(invoice);
    if (invoiceRemaining.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException(INVOICE_NOT_CLOSED_FOR_AGREEMENT);
    }

    BigDecimal entryAmount = normalize(entryAmountRaw);
    if (entryAmount.compareTo(invoiceRemaining) == 0) {
      throw new BusinessRuleException(ENTRY_EQUALS_REMAINING);
    }
    if (entryAmount.compareTo(invoiceRemaining) > 0) {
      throw new BusinessRuleException(ENTRY_EXCEEDS_REMAINING);
    }

    BigDecimal installmentAmount = normalize(installmentAmountRaw);
    BigDecimal invoiceSettlementAmount = normalize(invoiceRemaining.subtract(entryAmount));
    BigDecimal financedAmount = invoiceSettlementAmount;
    BigDecimal futureOriginalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal anticipatedFuturesNetAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal futuresDiscountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    List<CreditCardInvoiceAgreement> previousActive = List.of();
    List<FutureInstallmentLine> futureLines = List.of();
    if (renegotiation) {
      previousActive = agreementRepository.findAllActiveByCardForUpdate(card.getId(), userId);
      futureLines = collectFutureInstallmentLines(invoice, previousActive);
      futureOriginalAmount =
          normalize(
              futureLines.stream()
                  .map(FutureInstallmentLine::remaining)
                  .reduce(BigDecimal.ZERO, BigDecimal::add));
      anticipatedFuturesNetAmount =
          anticipatedFuturesNetAmountRaw == null
              ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
              : normalize(anticipatedFuturesNetAmountRaw);
      if (anticipatedFuturesNetAmount.compareTo(BigDecimal.ZERO) < 0) {
        throw new BusinessRuleException(ANTICIPATED_FUTURES_NET_NEGATIVE);
      }
      if (futureOriginalAmount.compareTo(BigDecimal.ZERO) == 0) {
        anticipatedFuturesNetAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      } else if (anticipatedFuturesNetAmount.compareTo(futureOriginalAmount) > 0) {
        throw new BusinessRuleException(ANTICIPATED_FUTURES_NET_EXCEEDS_ORIGINAL);
      }
      futuresDiscountAmount = normalize(futureOriginalAmount.subtract(anticipatedFuturesNetAmount));
      financedAmount = normalize(invoiceSettlementAmount.add(anticipatedFuturesNetAmount));
    }

    BigDecimal contractedTotal =
        normalize(installmentAmount.multiply(BigDecimal.valueOf(installmentCount)));
    if (contractedTotal.compareTo(financedAmount) < 0) {
      throw new BusinessRuleException(AGREEMENT_CONTRACTED_TOTAL_BELOW_FINANCED_AMOUNT);
    }

    if (renegotiation) {
      applyFuturesDiscountAndIncorporation(
          authenticatedUser, futureLines, futuresDiscountAmount, previousActive);
    }

    if (entryAmount.compareTo(BigDecimal.ZERO) > 0) {
      creditCardInvoiceService.pay(
          authenticatedUser,
          invoice.getId(),
          new PayInvoiceRequest(accountId, entryAmount, entryPaymentDate, null));
    }

    Expense categorySource =
        expenseInstallmentRepository
            .findAllByInvoice_IdAndUserIdOrderByDueDateAscIdAsc(invoice.getId(), userId)
            .stream()
            .findFirst()
            .map(ExpenseInstallment::getExpense)
            .orElseThrow(() -> new BusinessRuleException(INVOICE_NOT_CLOSED_FOR_AGREEMENT));

    Instant now = Instant.now(clock);
    Expense expense =
        expenseService.createCreditCardAgreementExpense(
            userId,
            card,
            categorySource.getCategory(),
            renegotiation ? RENEGOTIATION_DESCRIPTION : AGREEMENT_DESCRIPTION,
            contractedTotal,
            installmentCount,
            installmentAmount,
            invoice.getClosingDate());

    CreditCardInvoiceAgreement agreement = new CreditCardInvoiceAgreement();
    agreement.setId(UuidV7.create());
    agreement.setUserId(userId);
    agreement.setCreditCard(card);
    agreement.setSourceInvoice(invoice);
    agreement.setExpense(expense);
    agreement.setStatus(CreditCardInvoiceAgreementStatus.ACTIVE);
    agreement.setEntryAmount(entryAmount);
    agreement.setFinancedAmount(financedAmount);
    agreement.setInstallmentCount(installmentCount);
    agreement.setInstallmentAmount(installmentAmount);
    agreement.setCreatedAt(now);
    agreement.setUpdatedAt(now);
    agreementRepository.save(agreement);

    persistSettlementAndAllocations(agreement, invoice, invoiceSettlementAmount, now);

    invoice.setStatus(CreditCardInvoiceStatus.SETTLED_BY_AGREEMENT);
    invoice.setUpdatedAt(now);
    invoiceRepository.save(invoice);
    creditCardInvoiceService.refreshInstallmentsOfInvoice(invoice, now);

    if (renegotiation) {
      for (CreditCardInvoiceAgreement previous : previousActive) {
        previous.setStatus(CreditCardInvoiceAgreementStatus.RENEGOTIATED);
        previous.setSupersededByAgreement(agreement);
        previous.setUpdatedAt(now);
        agreementRepository.save(previous);
        creditCardInvoiceService.refreshExpenseOf(previous.getExpense(), now);
      }
    }

    return toResponse(agreement);
  }

  private List<FutureInstallmentLine> collectFutureInstallmentLines(
      CreditCardInvoice currentInvoice, List<CreditCardInvoiceAgreement> activeAgreements) {
    List<FutureInstallmentLine> lines = new ArrayList<>();
    for (CreditCardInvoiceAgreement previous : activeAgreements) {
      List<ExpenseInstallment> installments =
          expenseInstallmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
              previous.getExpense().getId(), previous.getUserId());
      for (ExpenseInstallment installment : installments) {
        if (installment.getInvoice() != null
            && installment.getInvoice().getId().equals(currentInvoice.getId())) {
          continue;
        }
        if (installment.getStatus() == ExpenseStatus.PAID
            || installment.getStatus() == ExpenseStatus.CANCELLED
            || installment.getStatus() == ExpenseStatus.REFUNDED) {
          continue;
        }
        ExpenseInstallment locked =
            expenseInstallmentRepository
                .findByIdAndExpense_IdAndUserIdForUpdate(
                    installment.getId(), previous.getExpense().getId(), previous.getUserId())
                .orElse(null);
        if (locked == null) {
          continue;
        }
        BigDecimal remaining = installmentBalanceService.remaining(locked);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
          continue;
        }
        lines.add(
            new FutureInstallmentLine(
                previous.getExpense().getId(), locked, remaining, locked.getDueDate()));
      }
    }
    return lines;
  }

  private void applyFuturesDiscountAndIncorporation(
      AuthenticatedUser authenticatedUser,
      List<FutureInstallmentLine> futureLines,
      BigDecimal futuresDiscountAmount,
      List<CreditCardInvoiceAgreement> previousActive) {
    if (futureLines.isEmpty()) {
      return;
    }
    Instant now = Instant.now(clock);
    List<InvoiceAllocationCalculator.Line> allocationLines = new ArrayList<>();
    for (FutureInstallmentLine line : futureLines) {
      allocationLines.add(
          new InvoiceAllocationCalculator.Line(
              line.installment().getId(), line.dueDate(), line.remaining()));
    }
    List<InvoiceAllocationCalculator.Share> discountShares =
        InvoiceAllocationCalculator.allocate(futuresDiscountAmount, allocationLines);
    java.util.Map<UUID, BigDecimal> discountByInstallment = new java.util.HashMap<>();
    for (InvoiceAllocationCalculator.Share share : discountShares) {
      discountByInstallment.put(share.installmentId(), share.amount());
    }

    for (FutureInstallmentLine line : futureLines) {
      BigDecimal discount =
          discountByInstallment.getOrDefault(
              line.installment().getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
      BigDecimal incorporation = normalize(line.remaining().subtract(discount));
      if (discount.compareTo(BigDecimal.ZERO) > 0) {
        expenseService.createAdjustment(
            authenticatedUser,
            line.expenseId(),
            line.installment().getId(),
            AdjustmentType.DISCOUNT,
            discount,
            RENEGOTIATION_FUTURES_FINANCIAL_DISCOUNT_REASON);
      }
      if (incorporation.compareTo(BigDecimal.ZERO) > 0) {
        expenseService.createAdjustment(
            authenticatedUser,
            line.expenseId(),
            line.installment().getId(),
            AdjustmentType.DISCOUNT,
            incorporation,
            RENEGOTIATION_FUTURES_INCORPORATION_REASON);
      }
    }
    for (CreditCardInvoiceAgreement previous : previousActive) {
      creditCardInvoiceService.refreshExpenseOf(previous.getExpense(), now);
    }
  }

  private record FutureInstallmentLine(
      UUID expenseId,
      ExpenseInstallment installment,
      BigDecimal remaining,
      java.time.LocalDate dueDate) {}

  private void persistSettlementAndAllocations(
      CreditCardInvoiceAgreement agreement,
      CreditCardInvoice invoice,
      BigDecimal invoiceSettlementAmount,
      Instant now) {
    CreditCardInvoiceAgreementSettlement settlement = new CreditCardInvoiceAgreementSettlement();
    settlement.setId(UuidV7.create());
    settlement.setUserId(agreement.getUserId());
    settlement.setAgreement(agreement);
    settlement.setInvoice(invoice);
    settlement.setAmount(invoiceSettlementAmount);
    settlement.setCreatedAt(now);
    settlementRepository.save(settlement);

    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByInvoiceIdAndUserIdForUpdate(
            invoice.getId(), invoice.getUserId());
    List<InvoiceAllocationCalculator.Line> lines = new ArrayList<>();
    for (ExpenseInstallment installment : installments) {
      if (installment.getStatus() == ExpenseStatus.CANCELLED
          || installment.getStatus() == ExpenseStatus.REFUNDED) {
        continue;
      }
      lines.add(
          new InvoiceAllocationCalculator.Line(
              installment.getId(),
              installment.getDueDate(),
              installmentBalanceService.remaining(installment)));
    }
    List<InvoiceAllocationCalculator.Share> shares =
        InvoiceAllocationCalculator.allocate(invoiceSettlementAmount, lines);
    for (InvoiceAllocationCalculator.Share share : shares) {
      ExpenseInstallment installment =
          expenseInstallmentRepository.findById(share.installmentId()).orElseThrow();
      CreditCardInvoiceAgreementSettlementAllocation allocation =
          new CreditCardInvoiceAgreementSettlementAllocation();
      allocation.setId(UuidV7.create());
      allocation.setUserId(agreement.getUserId());
      allocation.setSettlement(settlement);
      allocation.setInstallment(installment);
      allocation.setAmount(share.amount());
      allocation.setCreatedAt(now);
      settlementAllocationRepository.save(allocation);
    }
  }

  private void completeAgreementIfFullyPaid(CreditCardInvoiceAgreement agreement, Instant now) {
    if (agreement.getStatus() != CreditCardInvoiceAgreementStatus.ACTIVE) {
      return;
    }
    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            agreement.getExpense().getId(), agreement.getUserId());
    boolean allPaid =
        installments.stream()
            .allMatch(installment -> installment.getStatus() == ExpenseStatus.PAID);
    if (allPaid) {
      agreement.setStatus(CreditCardInvoiceAgreementStatus.COMPLETED);
      agreement.setUpdatedAt(now);
      agreementRepository.save(agreement);
    }
  }

  private AgreementResponse toResponse(CreditCardInvoiceAgreement agreement) {
    BigDecimal contractedTotal =
        normalize(
            agreement
                .getInstallmentAmount()
                .multiply(BigDecimal.valueOf(agreement.getInstallmentCount())));
    BigDecimal additionalCost = normalize(contractedTotal.subtract(agreement.getFinancedAmount()));
    BigDecimal additionalCostPercent =
        agreement.getFinancedAmount().compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
            : additionalCost.divide(agreement.getFinancedAmount(), 4, RoundingMode.HALF_UP);

    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            agreement.getExpense().getId(), agreement.getUserId());
    List<AgreementInstallmentResponse> installmentResponses = new ArrayList<>(installments.size());
    for (ExpenseInstallment installment : installments) {
      installmentResponses.add(
          new AgreementInstallmentResponse(
              installment.getId(),
              agreement.getExpense().getId(),
              installment.getInstallmentNumber(),
              installment.getTotalInstallments(),
              installment.getAmount(),
              installmentBalanceService.remaining(installment),
              installment.getDueDate(),
              installment.getStatus(),
              installment.getInvoice() == null ? null : installment.getInvoice().getId(),
              installment.getCreatedAt(),
              installment.getUpdatedAt()));
    }

    return new AgreementResponse(
        agreement.getId(),
        agreement.getCreditCard().getId(),
        agreement.getSourceInvoice().getId(),
        agreement.getExpense().getId(),
        agreement.getStatus(),
        agreement.getEntryAmount(),
        agreement.getFinancedAmount(),
        agreement.getInstallmentCount(),
        agreement.getInstallmentAmount(),
        contractedTotal,
        additionalCost,
        additionalCostPercent,
        agreement.getCreatedAt(),
        agreement.getSupersededByAgreement() == null
            ? null
            : agreement.getSupersededByAgreement().getId(),
        installmentResponses);
  }

  private CreditCardInvoiceAgreement requireOwned(UUID userId, UUID agreementId) {
    return agreementRepository
        .findByIdAndUserId(agreementId, userId)
        .orElseThrow(() -> new NotFoundException(AGREEMENT_NOT_FOUND));
  }

  private CreditCardInvoiceAgreement requireOwnedForUpdate(UUID userId, UUID agreementId) {
    return agreementRepository
        .findByIdAndUserIdForUpdate(agreementId, userId)
        .orElseThrow(() -> new NotFoundException(AGREEMENT_NOT_FOUND));
  }

  private CreditCardInvoice requireOwnedInvoice(UUID userId, UUID invoiceId) {
    return invoiceRepository
        .findByIdAndUserId(invoiceId, userId)
        .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
  }

  private CreditCardInvoice requireOwnedInvoiceForUpdate(UUID userId, UUID invoiceId) {
    return invoiceRepository
        .findByIdAndUserIdForUpdate(invoiceId, userId)
        .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
