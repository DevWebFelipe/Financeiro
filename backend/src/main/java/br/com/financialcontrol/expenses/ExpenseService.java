package br.com.financialcontrol.expenses;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoice;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_cards.CardPurchaseAccountRefund;
import br.com.financialcontrol.credit_cards.CardPurchaseAccountRefundRepository;
import br.com.financialcontrol.credit_cards.CreditCard;
import br.com.financialcontrol.credit_cards.CreditCardCreditApplicationRepository;
import br.com.financialcontrol.credit_cards.CreditCardCreditOrigin;
import br.com.financialcontrol.credit_cards.CreditCardService;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import br.com.financialcontrol.expenses.dto.CreateAdjustmentRequest;
import br.com.financialcontrol.expenses.dto.CreateExpenseRequest;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpensePageResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.expenses.dto.PayExpenseRequest;
import br.com.financialcontrol.expenses.dto.RefundExpenseRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseInstallmentRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseRequest;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.PaymentStatus;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

  static final String EXPENSE_NOT_FOUND = "Despesa não encontrada.";
  static final String PAYMENT_NOT_FOUND = "Pagamento não encontrado.";
  static final String INSTALLMENT_NOT_FOUND = "Parcela da despesa não encontrada.";
  static final String ADJUSTMENT_NOT_FOUND = "Ajuste da parcela não encontrado.";
  static final String CREDIT_CARD_NOT_ALLOWED = "Despesas no cartão não são permitidas nesta fase.";
  static final String ACCOUNT_REQUIRED_FOR_ACCOUNT_METHOD =
      "A conta é obrigatória para despesa em conta.";
  static final String ACCOUNT_NOT_ALLOWED_FOR_NONE =
      "Despesa sem cartão não deve informar conta no cadastro.";
  static final String ACCOUNT_NOT_ALLOWED_FOR_CREDIT_CARD =
      "Despesa no cartão não deve informar conta no cadastro.";
  static final String ACCOUNT_REQUIRED_FOR_PAYMENT = "A conta é obrigatória.";
  static final String OTHER_REQUIRES_NAME =
      "O nome do responsável é obrigatório quando o tipo for OTHER.";
  static final String ONLY_OPEN_CAN_BE_EDITED = "Somente despesas abertas podem ser editadas.";
  static final String ONLY_OPEN_INSTALLMENT_CAN_BE_EDITED =
      "Somente parcelas abertas podem ser editadas.";
  static final String TERMINAL_EXPENSE_INSTALLMENT_IMMUTABLE =
      "Parcela de despesa cancelada ou estornada não pode ser alterada.";
  static final String ONLY_OPEN_CAN_BE_CANCELLED = "Somente despesas abertas podem ser canceladas.";
  static final String ONLY_OPEN_OR_PARTIAL_CAN_BE_PAID =
      "Somente despesas abertas ou parcialmente pagas podem ser pagas.";
  static final String ONLY_PAID_OR_PARTIAL_CAN_BE_REFUNDED =
      "Somente despesas pagas ou parcialmente pagas podem ser estornadas.";
  static final String INSTALLMENT_NOT_PAYABLE = "A parcela não pode receber pagamento.";
  static final String INSTALLMENT_NOT_ADJUSTABLE = "A parcela não pode receber ajuste.";
  static final String PAYMENT_EXCEEDS_DUE = "O pagamento não pode ultrapassar o valor devido.";
  static final String INSUFFICIENT_BALANCE =
      "O pagamento não pode exceder o saldo disponível da conta.";
  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";
  static final String INSTALLMENT_SUM_MISMATCH =
      "A soma das parcelas deve ser igual ao valor total da despesa.";
  static final String MULTI_INSTALLMENT_TOTAL_IMMUTABLE =
      "O valor total de despesa parcelada não pode ser alterado pelo PUT da despesa.";
  static final String MULTI_INSTALLMENT_DUE_DATE_IMMUTABLE =
      "O vencimento da primeira parcela de despesa parcelada deve ser alterado na própria parcela.";
  static final String PAY_REQUIRES_SINGLE_INSTALLMENT =
      "Despesa parcelada deve ser paga identificando a parcela.";
  static final String PAYMENT_ALREADY_REVERSED = "O pagamento já está estornado.";
  static final String PAYMENT_REVERSE_NOT_ALLOWED =
      "Não é permitido estornar pagamento de despesa cancelada ou estornada.";
  static final String ADJUSTMENT_ALREADY_REVERSED = "O ajuste já está estornado.";
  static final String ADJUSTMENT_REVERSE_NOT_ALLOWED =
      "Não é permitido estornar ajuste de despesa cancelada ou estornada.";
  static final String ADJUSTMENT_INVALID_OBLIGATION =
      "O ajuste deixaria a obrigação da parcela inválida.";
  static final String ADJUSTMENT_AMOUNT_MUST_BE_POSITIVE =
      "O valor do ajuste deve ser maior que zero.";
  static final String INVALID_INSTALLMENT_OBLIGATION =
      "A alteração deixaria a obrigação da parcela inválida.";
  static final String CREDIT_CARD_REQUIRED = "O cartão é obrigatório para despesa no cartão.";
  static final String CREDIT_CARD_NOT_ALLOWED_FOR_ACCOUNT =
      "Despesa em conta não deve informar cartão.";
  static final String CARD_EXPENSE_NOT_PAYABLE =
      "Despesa no cartão não é liquidada por pagamento de despesa.";
  static final String INSTALLMENT_ON_INVOICE_IMMUTABLE =
      "Parcela vinculada a fatura não pode ser editada cadastralmente nesta fase.";
  static final String ADJUSTMENT_REASON_REQUIRED = "O motivo do ajuste é obrigatório.";
  static final String SETTLEMENT_REQUIRED =
      "O destino do estorno da compra no cartão é obrigatório.";
  static final String SETTLEMENT_ACCOUNT_REQUIRED =
      "A conta é obrigatória para devolver o valor liquidado.";
  static final String SETTLEMENT_NOT_ALLOWED =
      "Estorno de despesa em conta não aceita settlement de cartão.";
  static final String PAYMENT_METHOD_CREDIT_CARD_IMMUTABLE =
      "A forma de pagamento no cartão não pode ser alterada depois da criação.";
  static final String CREDIT_CARD_TOTAL_IMMUTABLE =
      "O valor total de despesa no cartão não pode ser alterado pelo PUT da despesa.";
  static final String CREDIT_CARD_DUE_DATE_IMMUTABLE =
      "O vencimento de despesa no cartão é o da fatura e não pode ser alterado pelo PUT.";
  static final String INVOICE_PAID_NO_ADJUSTMENT =
      "Parcela em fatura paga não pode receber ajuste.";
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  private final ExpenseRepository expenseRepository;
  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final ExpenseInstallmentAdjustmentRepository adjustmentRepository;
  private final PaymentRepository paymentRepository;
  private final AccountService accountService;
  private final CategoryService categoryService;
  private final CreditCardService creditCardService;
  private final CreditCardInvoiceService creditCardInvoiceService;
  private final InstallmentBalanceService installmentBalanceService;
  private final CreditCardInvoicePaymentAllocationRepository invoicePaymentAllocationRepository;
  private final CreditCardCreditApplicationRepository creditApplicationRepository;
  private final CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository;
  private final Clock clock;

  public ExpenseService(
      ExpenseRepository expenseRepository,
      ExpenseInstallmentRepository expenseInstallmentRepository,
      ExpenseInstallmentAdjustmentRepository adjustmentRepository,
      PaymentRepository paymentRepository,
      AccountService accountService,
      CategoryService categoryService,
      CreditCardService creditCardService,
      CreditCardInvoiceService creditCardInvoiceService,
      InstallmentBalanceService installmentBalanceService,
      CreditCardInvoicePaymentAllocationRepository invoicePaymentAllocationRepository,
      CreditCardCreditApplicationRepository creditApplicationRepository,
      CardPurchaseAccountRefundRepository cardPurchaseAccountRefundRepository,
      Clock clock) {
    this.expenseRepository = expenseRepository;
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.paymentRepository = paymentRepository;
    this.accountService = accountService;
    this.categoryService = categoryService;
    this.creditCardService = creditCardService;
    this.creditCardInvoiceService = creditCardInvoiceService;
    this.installmentBalanceService = installmentBalanceService;
    this.invoicePaymentAllocationRepository = invoicePaymentAllocationRepository;
    this.creditApplicationRepository = creditApplicationRepository;
    this.cardPurchaseAccountRefundRepository = cardPurchaseAccountRefundRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ExpensePageResponse list(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      ExpenseStatus status,
      UUID categoryId,
      UUID accountId,
      UUID creditCardId,
      ResponsibleType responsibleType,
      PaymentMethod paymentMethod,
      int page,
      int size) {
    if (page < 0) {
      throw new BusinessRuleException(INVALID_PAGE);
    }
    if (size < 1) {
      throw new BusinessRuleException(INVALID_PAGE_SIZE);
    }
    UUID userId = authenticatedUser.userId();
    Page<Expense> result =
        expenseRepository.searchByUser(
            userId,
            status,
            categoryId,
            accountId,
            creditCardId,
            responsibleType,
            paymentMethod,
            startDate,
            endDate,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
    Map<UUID, List<ExpenseInstallment>> installmentsByExpense =
        loadInstallmentsByExpense(userId, result.getContent());
    return new ExpensePageResponse(
        result.getContent().stream()
            .map(
                expense ->
                    toResponse(expense, requireMappedInstallments(expense, installmentsByExpense)))
            .toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public ExpenseResponse get(AuthenticatedUser authenticatedUser, UUID expenseId) {
    Expense expense = requireOwnedExpense(authenticatedUser.userId(), expenseId);
    return toResponse(expense, requireInstallments(expense));
  }

  @Transactional(readOnly = true)
  public List<ExpenseInstallmentResponse> listInstallments(
      AuthenticatedUser authenticatedUser, UUID expenseId) {
    Expense expense = requireOwnedExpense(authenticatedUser.userId(), expenseId);
    return requireInstallments(expense).stream()
        .map(installment -> toInstallmentResponse(expense, installment))
        .toList();
  }

  @Transactional(readOnly = true)
  public ExpenseInstallmentResponse getInstallment(
      AuthenticatedUser authenticatedUser, UUID expenseId, UUID installmentId) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpense(userId, expenseId);
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserId(installmentId, expenseId, userId)
            .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    return toInstallmentResponse(expense, installment);
  }

  @Transactional
  public ExpenseResponse create(AuthenticatedUser authenticatedUser, CreateExpenseRequest request) {
    UUID userId = authenticatedUser.userId();
    int installmentCount = request.resolvedInstallmentCount();
    Category category =
        categoryService.requireActiveOwnedExpenseCategory(userId, request.categoryId());
    Account account =
        resolveAccountForCreateOrUpdate(
            userId, request.paymentMethod(), request.accountId(), request.creditCardId());
    CreditCard creditCard = resolveCreditCardForCreate(userId, request);
    String responsibleName =
        resolveResponsibleName(request.responsibleType(), request.responsibleName());
    Instant now = Instant.now(clock);
    BigDecimal totalAmount = normalizeMoney(request.totalAmount());
    List<BigDecimal> amounts = splitInstallmentAmounts(totalAmount, installmentCount);
    assertInstallmentSum(totalAmount, amounts);

    Expense expense = new Expense();
    expense.setId(UuidV7.create());
    expense.setUserId(userId);
    expense.setCategory(category);
    expense.setAccount(account);
    expense.setCreditCard(creditCard);
    expense.setDescription(request.description());
    expense.setTotalAmount(totalAmount);
    expense.setExpenseDate(request.expenseDate());
    expense.setDueDate(request.dueDate());
    expense.setPaymentMethod(request.paymentMethod());
    expense.setStatus(ExpenseStatus.OPEN);
    expense.setResponsibleType(request.responsibleType());
    expense.setResponsibleName(responsibleName);
    expense.setBarcode(request.barcode());
    expense.setNotes(request.notes());
    expense.setCreatedAt(now);
    expense.setUpdatedAt(now);
    expenseRepository.save(expense);

    List<ExpenseInstallment> installments = new ArrayList<>(installmentCount);
    LocalDate firstDueDate = request.dueDate();
    for (int number = 1; number <= installmentCount; number++) {
      ExpenseInstallment installment = new ExpenseInstallment();
      installment.setId(UuidV7.create());
      installment.setUserId(userId);
      installment.setExpense(expense);
      installment.setInstallmentNumber(number);
      installment.setTotalInstallments(installmentCount);
      installment.setAmount(amounts.get(number - 1));
      installment.setStatus(ExpenseStatus.OPEN);
      installment.setCreatedAt(now);
      installment.setUpdatedAt(now);
      if (creditCard != null) {
        CreditCardInvoice invoice =
            creditCardInvoiceService.requireInvoiceForPurchase(
                creditCard, request.expenseDate(), number);
        installment.setInvoice(invoice);
        installment.setDueDate(invoice.getDueDate());
        if (number == 1) {
          firstDueDate = invoice.getDueDate();
        }
      } else {
        installment.setInvoice(null);
        installment.setDueDate(dueDateForInstallment(request.dueDate(), number));
      }
      installments.add(installment);
    }
    if (!firstDueDate.equals(expense.getDueDate())) {
      expense.setDueDate(firstDueDate);
      expenseRepository.save(expense);
    }
    expenseInstallmentRepository.saveAll(installments);
    assertInstallmentSum(
        totalAmount, installments.stream().map(ExpenseInstallment::getAmount).toList());

    return toResponse(expense, installments);
  }

  @Transactional
  public ExpenseResponse update(
      AuthenticatedUser authenticatedUser, UUID expenseId, UpdateExpenseRequest request) {
    Expense expense = requireOwnedExpenseForUpdate(authenticatedUser.userId(), expenseId);
    if (expense.getStatus() != ExpenseStatus.OPEN) {
      throw new BusinessRuleException(ONLY_OPEN_CAN_BE_EDITED);
    }
    List<ExpenseInstallment> installments = requireInstallments(expense);
    Category category =
        categoryService.requireActiveOwnedExpenseCategory(
            authenticatedUser.userId(), request.categoryId());
    Account account =
        resolveAccountForCreateOrUpdate(
            authenticatedUser.userId(),
            request.paymentMethod(),
            request.accountId(),
            request.creditCardId());
    if (expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD
        || request.paymentMethod() == PaymentMethod.CREDIT_CARD) {
      assertCreditCardUpdateAllowed(expense, request);
    }
    BigDecimal totalAmount = normalizeMoney(request.totalAmount());
    Instant now = Instant.now(clock);

    if (installments.size() == 1) {
      ExpenseInstallment installment =
          requireSingleInstallmentForUpdate(authenticatedUser.userId(), expense.getId());
      expense.setCategory(category);
      expense.setAccount(account);
      expense.setCreditCard(expense.getCreditCard());
      expense.setDescription(request.description());
      expense.setTotalAmount(totalAmount);
      expense.setExpenseDate(request.expenseDate());
      expense.setDueDate(request.dueDate());
      expense.setPaymentMethod(request.paymentMethod());
      expense.setResponsibleType(request.responsibleType());
      expense.setResponsibleName(
          resolveResponsibleName(request.responsibleType(), request.responsibleName()));
      expense.setBarcode(request.barcode());
      expense.setNotes(request.notes());
      expense.setUpdatedAt(now);

      installment.setAmount(totalAmount);
      installment.setDueDate(request.dueDate());
      installment.setUpdatedAt(now);
      assertValidInstallmentObligation(installment, totalAmount, authenticatedUser.userId());

      expenseRepository.save(expense);
      expenseInstallmentRepository.save(installment);
      return toResponse(expense, List.of(installment));
    }

    if (totalAmount.compareTo(expense.getTotalAmount()) != 0) {
      throw new BusinessRuleException(MULTI_INSTALLMENT_TOTAL_IMMUTABLE);
    }
    if (!request.dueDate().equals(expense.getDueDate())) {
      throw new BusinessRuleException(MULTI_INSTALLMENT_DUE_DATE_IMMUTABLE);
    }

    expense.setCategory(category);
    expense.setAccount(account);
    expense.setCreditCard(expense.getCreditCard());
    expense.setDescription(request.description());
    expense.setExpenseDate(request.expenseDate());
    expense.setPaymentMethod(request.paymentMethod());
    expense.setResponsibleType(request.responsibleType());
    expense.setResponsibleName(
        resolveResponsibleName(request.responsibleType(), request.responsibleName()));
    expense.setBarcode(request.barcode());
    expense.setNotes(request.notes());
    expense.setUpdatedAt(now);
    expenseRepository.save(expense);
    return toResponse(expense, installments);
  }

  @Transactional
  public ExpenseInstallmentResponse updateInstallment(
      AuthenticatedUser authenticatedUser,
      UUID expenseId,
      UUID installmentId,
      UpdateExpenseInstallmentRequest request) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpenseForUpdate(userId, expenseId);
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(TERMINAL_EXPENSE_INSTALLMENT_IMMUTABLE);
    }
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserIdForUpdate(installmentId, expenseId, userId)
            .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    if (installment.getInvoice() != null) {
      throw new BusinessRuleException(INSTALLMENT_ON_INVOICE_IMMUTABLE);
    }
    if (installment.getStatus() != ExpenseStatus.OPEN) {
      throw new BusinessRuleException(ONLY_OPEN_INSTALLMENT_CAN_BE_EDITED);
    }

    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expenseId, userId);
    BigDecimal newAmount = normalizeMoney(request.amount());
    Instant now = Instant.now(clock);

    BigDecimal sum =
        installments.stream()
            .map(item -> item.getId().equals(installment.getId()) ? newAmount : item.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(expense.getTotalAmount()) != 0) {
      throw new BusinessRuleException(INSTALLMENT_SUM_MISMATCH);
    }
    assertValidInstallmentObligation(installment, newAmount, userId);

    installment.setAmount(newAmount);
    installment.setDueDate(request.dueDate());
    installment.setUpdatedAt(now);

    if (installment.getInstallmentNumber() == 1
        && !request.dueDate().equals(expense.getDueDate())) {
      expense.setDueDate(request.dueDate());
      expense.setUpdatedAt(now);
      expenseRepository.save(expense);
    }

    expenseInstallmentRepository.save(installment);
    return toInstallmentResponse(expense, installment);
  }

  /** Phase 7 legacy: pay 1/1 only. N&gt;1 must use {@link #payInstallment}. */
  @Transactional
  public ExpenseResponse pay(
      AuthenticatedUser authenticatedUser, UUID expenseId, PayExpenseRequest request) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpenseForUpdate(userId, expenseId);
    if (expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
      throw new BusinessRuleException(CARD_EXPENSE_NOT_PAYABLE);
    }
    assertExpensePayable(expense);
    List<ExpenseInstallment> installments = requireInstallments(expense);
    if (installments.size() != 1) {
      throw new BusinessRuleException(PAY_REQUIRES_SINGLE_INSTALLMENT);
    }
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserIdForUpdate(
                installments.getFirst().getId(), expenseId, userId)
            .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    return registerPayment(userId, expense, installment, request);
  }

  @Transactional
  public ExpenseResponse payInstallment(
      AuthenticatedUser authenticatedUser,
      UUID expenseId,
      UUID installmentId,
      PayExpenseRequest request) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpenseForUpdate(userId, expenseId);
    if (expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
      throw new BusinessRuleException(CARD_EXPENSE_NOT_PAYABLE);
    }
    assertExpensePayable(expense);
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserIdForUpdate(installmentId, expenseId, userId)
            .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    return registerPayment(userId, expense, installment, request);
  }

  @Transactional
  public PaymentResponse reversePayment(AuthenticatedUser authenticatedUser, UUID paymentId) {
    UUID userId = authenticatedUser.userId();
    Payment paymentRef =
        paymentRepository
            .findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND));
    Expense expense = requireOwnedExpenseForUpdate(userId, paymentRef.getExpense().getId());
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(PAYMENT_REVERSE_NOT_ALLOWED);
    }
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserIdForUpdate(
                paymentRef.getInstallment().getId(), expense.getId(), userId)
            .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    Payment payment =
        paymentRepository
            .findByIdAndUserIdForUpdate(paymentId, userId)
            .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND));
    if (payment.getStatus() != PaymentStatus.ACTIVE) {
      throw new BusinessRuleException(PAYMENT_ALREADY_REVERSED);
    }

    Instant now = Instant.now(clock);
    payment.setStatus(PaymentStatus.REVERSED);
    paymentRepository.save(payment);
    recalculateFinancialStatuses(expense, installment, now);
    return PaymentResponse.from(payment);
  }

  @Transactional
  public AdjustmentResponse createAdjustment(
      AuthenticatedUser authenticatedUser,
      UUID expenseId,
      UUID installmentId,
      CreateAdjustmentRequest request) {
    return createAdjustment(
        authenticatedUser,
        expenseId,
        installmentId,
        request.type(),
        request.amount(),
        request.reason());
  }

  @Transactional
  public AdjustmentResponse createAdjustment(
      AuthenticatedUser authenticatedUser,
      UUID expenseId,
      UUID installmentId,
      AdjustmentType type,
      BigDecimal amount) {
    return createAdjustment(authenticatedUser, expenseId, installmentId, type, amount, "ajuste");
  }

  @Transactional
  public AdjustmentResponse createAdjustment(
      AuthenticatedUser authenticatedUser,
      UUID expenseId,
      UUID installmentId,
      AdjustmentType type,
      BigDecimal amount,
      String reason) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpenseForUpdate(userId, expenseId);
    assertExpenseAdjustable(expense);
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserIdForUpdate(installmentId, expenseId, userId)
            .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    assertInstallmentAdjustable(expense, installment);

    BigDecimal adjustmentAmount = normalizeMoney(amount);
    if (adjustmentAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException(ADJUSTMENT_AMOUNT_MUST_BE_POSITIVE);
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(ADJUSTMENT_REASON_REQUIRED);
    }

    InstallmentTotals totals = calculateInstallmentTotals(installment.getId(), userId);
    BigDecimal nextDiscount =
        type == AdjustmentType.DISCOUNT
            ? totals.activeDiscount().add(adjustmentAmount)
            : totals.activeDiscount();
    BigDecimal nextSurcharge =
        type == AdjustmentType.SURCHARGE
            ? totals.activeSurcharge().add(adjustmentAmount)
            : totals.activeSurcharge();
    BigDecimal nextObligation = obligation(installment.getAmount(), nextDiscount, nextSurcharge);
    if (nextObligation.compareTo(BigDecimal.ZERO) < 0
        || nextObligation.compareTo(totals.activePayments()) < 0) {
      throw new BusinessRuleException(ADJUSTMENT_INVALID_OBLIGATION);
    }

    Instant now = Instant.now(clock);
    ExpenseInstallmentAdjustment adjustment = new ExpenseInstallmentAdjustment();
    adjustment.setId(UuidV7.create());
    adjustment.setUserId(userId);
    adjustment.setInstallment(installment);
    adjustment.setType(type);
    adjustment.setAmount(adjustmentAmount);
    adjustment.setReason(reason.trim());
    adjustment.setStatus(AdjustmentStatus.ACTIVE);
    adjustment.setCreatedAt(now);
    adjustmentRepository.save(adjustment);
    recalculateFinancialStatuses(expense, installment, now);
    return AdjustmentResponse.from(adjustment);
  }

  @Transactional(readOnly = true)
  public List<AdjustmentResponse> listAdjustments(
      AuthenticatedUser authenticatedUser, UUID expenseId, UUID installmentId) {
    UUID userId = authenticatedUser.userId();
    requireOwnedExpense(userId, expenseId);
    expenseInstallmentRepository
        .findByIdAndExpense_IdAndUserId(installmentId, expenseId, userId)
        .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    return adjustmentRepository
        .findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(installmentId, userId)
        .stream()
        .map(AdjustmentResponse::from)
        .toList();
  }

  @Transactional
  public AdjustmentResponse reverseAdjustment(
      AuthenticatedUser authenticatedUser, UUID expenseId, UUID installmentId, UUID adjustmentId) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpenseForUpdate(userId, expenseId);
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(ADJUSTMENT_REVERSE_NOT_ALLOWED);
    }
    ExpenseInstallment installment =
        expenseInstallmentRepository
            .findByIdAndExpense_IdAndUserIdForUpdate(installmentId, expenseId, userId)
            .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
    ExpenseInstallmentAdjustment adjustment =
        adjustmentRepository
            .findByIdAndInstallment_IdAndUserIdForUpdate(adjustmentId, installmentId, userId)
            .orElseThrow(() -> new NotFoundException(ADJUSTMENT_NOT_FOUND));
    if (adjustment.getStatus() != AdjustmentStatus.ACTIVE) {
      throw new BusinessRuleException(ADJUSTMENT_ALREADY_REVERSED);
    }

    InstallmentTotals totals = calculateInstallmentTotals(installment.getId(), userId);
    BigDecimal nextDiscount =
        adjustment.getType() == AdjustmentType.DISCOUNT
            ? totals.activeDiscount().subtract(adjustment.getAmount())
            : totals.activeDiscount();
    BigDecimal nextSurcharge =
        adjustment.getType() == AdjustmentType.SURCHARGE
            ? totals.activeSurcharge().subtract(adjustment.getAmount())
            : totals.activeSurcharge();
    BigDecimal nextObligation = obligation(installment.getAmount(), nextDiscount, nextSurcharge);
    if (nextObligation.compareTo(BigDecimal.ZERO) < 0
        || nextObligation.compareTo(totals.activePayments()) < 0) {
      throw new BusinessRuleException(ADJUSTMENT_INVALID_OBLIGATION);
    }

    Instant now = Instant.now(clock);
    adjustment.setStatus(AdjustmentStatus.REVERSED);
    adjustmentRepository.save(adjustment);
    recalculateFinancialStatuses(expense, installment, now);
    return AdjustmentResponse.from(adjustment);
  }

  @Transactional
  public ExpenseResponse cancel(AuthenticatedUser authenticatedUser, UUID expenseId) {
    Expense expense = requireOwnedExpenseForUpdate(authenticatedUser.userId(), expenseId);
    if (expense.getStatus() != ExpenseStatus.OPEN) {
      throw new BusinessRuleException(ONLY_OPEN_CAN_BE_CANCELLED);
    }
    List<ExpenseInstallment> installments = requireInstallments(expense);
    Instant now = Instant.now(clock);
    expense.setStatus(ExpenseStatus.CANCELLED);
    expense.setUpdatedAt(now);
    for (ExpenseInstallment installment : installments) {
      if (expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD
          && installment.getInvoice() != null
          && installment.getInvoice().getStatus() == CreditCardInvoiceStatus.PAID) {
        installment.setUpdatedAt(now);
        continue;
      }
      installment.setStatus(ExpenseStatus.CANCELLED);
      installment.setUpdatedAt(now);
    }
    expenseRepository.save(expense);
    expenseInstallmentRepository.saveAll(installments);
    refreshAffectedInvoices(installments);
    return toResponse(expense, installments);
  }

  @Transactional
  public ExpenseResponse refund(AuthenticatedUser authenticatedUser, UUID expenseId) {
    return refund(authenticatedUser, expenseId, null);
  }

  @Transactional
  public ExpenseResponse refund(
      AuthenticatedUser authenticatedUser, UUID expenseId, RefundExpenseRequest request) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpenseForUpdate(userId, expenseId);
    if (expense.getStatus() != ExpenseStatus.PARTIALLY_PAID
        && expense.getStatus() != ExpenseStatus.PAID) {
      throw new BusinessRuleException(ONLY_PAID_OR_PARTIAL_CAN_BE_REFUNDED);
    }
    if (expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
      return refundCreditCardPurchase(userId, expense, request);
    }
    if (request != null && request.settlement() != null) {
      throw new BusinessRuleException(SETTLEMENT_NOT_ALLOWED);
    }
    List<ExpenseInstallment> installments = requireInstallments(expense);
    Instant now = Instant.now(clock);
    expense.setStatus(ExpenseStatus.REFUNDED);
    expense.setUpdatedAt(now);
    for (ExpenseInstallment installment : installments) {
      BigDecimal activePayments =
          zeroIfNull(
              paymentRepository.sumActiveAmountByInstallmentIdAndUserId(
                  installment.getId(), userId));
      if (activePayments.compareTo(BigDecimal.ZERO) > 0) {
        installment.setStatus(ExpenseStatus.REFUNDED);
      } else {
        installment.setStatus(ExpenseStatus.OPEN);
      }
      installment.setUpdatedAt(now);
    }
    expenseRepository.save(expense);
    expenseInstallmentRepository.saveAll(installments);
    return toResponse(expense, installments);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> listPayments(AuthenticatedUser authenticatedUser, UUID expenseId) {
    requireOwnedExpense(authenticatedUser.userId(), expenseId);
    return paymentRepository
        .findAllByExpense_IdAndUserIdOrderByCreatedAtAsc(expenseId, authenticatedUser.userId())
        .stream()
        .map(PaymentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public PaymentResponse getPayment(AuthenticatedUser authenticatedUser, UUID paymentId) {
    return PaymentResponse.from(
        paymentRepository
            .findByIdAndUserId(paymentId, authenticatedUser.userId())
            .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND)));
  }

  static List<BigDecimal> splitInstallmentAmounts(BigDecimal totalAmount, int installmentCount) {
    if (installmentCount < 1) {
      throw new BusinessRuleException("A quantidade de parcelas deve ser maior que zero.");
    }
    BigDecimal normalizedTotal = normalizeMoney(totalAmount);
    BigDecimal base =
        normalizedTotal.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.DOWN);
    List<BigDecimal> amounts = new ArrayList<>(installmentCount);
    BigDecimal others = base.multiply(BigDecimal.valueOf(installmentCount - 1L));
    amounts.add(normalizeMoney(normalizedTotal.subtract(others)));
    for (int i = 1; i < installmentCount; i++) {
      amounts.add(base);
    }
    return amounts;
  }

  static LocalDate dueDateForInstallment(LocalDate firstDueDate, int installmentNumber) {
    if (installmentNumber < 1) {
      throw new IllegalArgumentException("installmentNumber must be >= 1");
    }
    if (installmentNumber == 1) {
      return firstDueDate;
    }
    int baseDay = firstDueDate.getDayOfMonth();
    YearMonth targetMonth = YearMonth.from(firstDueDate).plusMonths(installmentNumber - 1L);
    int day = Math.min(baseDay, targetMonth.lengthOfMonth());
    return targetMonth.atDay(day);
  }

  /** RN231 remaining for tests and internal use. */
  BigDecimal calculateRemaining(UUID installmentId, UUID userId, BigDecimal originalAmount) {
    InstallmentTotals totals = calculateInstallmentTotals(installmentId, userId);
    return remaining(originalAmount, totals);
  }

  private ExpenseResponse registerPayment(
      UUID userId, Expense expense, ExpenseInstallment installment, PayExpenseRequest request) {
    assertInstallmentPayable(expense, installment);
    Account account = resolvePaymentAccount(userId, expense, request.accountId());
    BigDecimal amount = normalizeMoney(request.amount());
    InstallmentTotals totals = calculateInstallmentTotals(installment.getId(), userId);
    BigDecimal remaining = remaining(installment.getAmount(), totals);
    if (amount.compareTo(remaining) > 0) {
      throw new BusinessRuleException(PAYMENT_EXCEEDS_DUE);
    }
    if (accountService.calculateCurrentBalance(account).compareTo(amount) < 0) {
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

    recalculateFinancialStatuses(expense, installment, now);
    return toResponse(expense, requireInstallments(expense));
  }

  private void recalculateFinancialStatuses(
      Expense expense, ExpenseInstallment touchedInstallment, Instant now) {
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
      ExpenseStatus next;
      if (expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
        next = resolveCreditCardInstallmentStatus(installment);
      } else {
        next =
            resolveInstallmentFinancialStatus(
                installment, calculateInstallmentTotals(installment.getId(), expense.getUserId()));
      }
      installment.setStatus(next);
      installment.setUpdatedAt(now);
    }
    expense.setStatus(aggregateExpenseStatus(installments));
    expense.setUpdatedAt(now);
    expenseRepository.save(expense);
    expenseInstallmentRepository.saveAll(installments);
    touchedInstallment.setStatus(
        installments.stream()
            .filter(item -> item.getId().equals(touchedInstallment.getId()))
            .findFirst()
            .orElse(touchedInstallment)
            .getStatus());
  }

  private ExpenseStatus resolveCreditCardInstallmentStatus(ExpenseInstallment installment) {
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

  private static ExpenseStatus resolveInstallmentFinancialStatus(
      ExpenseInstallment installment, InstallmentTotals totals) {
    BigDecimal remaining = remaining(installment.getAmount(), totals);
    if (remaining.compareTo(BigDecimal.ZERO) == 0) {
      return ExpenseStatus.PAID;
    }
    if (totals.activePayments().compareTo(BigDecimal.ZERO) > 0) {
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
    return ExpenseStatus.PARTIALLY_PAID;
  }

  private void assertValidInstallmentObligation(
      ExpenseInstallment installment, BigDecimal amount, UUID userId) {
    InstallmentTotals totals = calculateInstallmentTotals(installment.getId(), userId);
    BigDecimal nextObligation =
        obligation(amount, totals.activeDiscount(), totals.activeSurcharge());
    if (nextObligation.compareTo(BigDecimal.ZERO) < 0
        || nextObligation.compareTo(totals.activePayments()) < 0) {
      throw new BusinessRuleException(INVALID_INSTALLMENT_OBLIGATION);
    }
  }

  private InstallmentTotals calculateInstallmentTotals(UUID installmentId, UUID userId) {
    return new InstallmentTotals(
        zeroIfNull(
            paymentRepository.sumActiveAmountByInstallmentIdAndUserId(installmentId, userId)),
        zeroIfNull(
            adjustmentRepository.sumActiveDiscountAmountByInstallmentIdAndUserId(
                installmentId, userId)),
        zeroIfNull(
            adjustmentRepository.sumActiveSurchargeAmountByInstallmentIdAndUserId(
                installmentId, userId)));
  }

  private static BigDecimal obligation(
      BigDecimal originalAmount, BigDecimal activeDiscount, BigDecimal activeSurcharge) {
    return normalizeMoney(originalAmount.add(activeSurcharge).subtract(activeDiscount));
  }

  private static BigDecimal remaining(BigDecimal originalAmount, InstallmentTotals totals) {
    BigDecimal value =
        obligation(originalAmount, totals.activeDiscount(), totals.activeSurcharge())
            .subtract(totals.activePayments());
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return normalizeMoney(value);
  }

  private ExpenseResponse refundCreditCardPurchase(
      UUID userId, Expense expense, RefundExpenseRequest request) {
    if (request == null || request.settlement() == null) {
      throw new BusinessRuleException(SETTLEMENT_REQUIRED);
    }
    CreditCard card =
        creditCardService.requireOwnedForUpdate(userId, expense.getCreditCard().getId());
    List<ExpenseInstallment> installments = requireInstallments(expense);
    BigDecimal bankLiquidated =
        zeroIfNull(
            invoicePaymentAllocationRepository.sumActiveAmountByExpenseIdAndUserId(
                expense.getId(), userId));
    BigDecimal creditLiquidated =
        zeroIfNull(
            creditApplicationRepository.sumAmountByExpenseIdAndUserId(expense.getId(), userId));
    BigDecimal totalLiquidated = normalizeMoney(bankLiquidated.add(creditLiquidated));

    Account refundAccount = null;
    if (request.settlement() == RefundExpenseRequest.RefundSettlement.ACCOUNT) {
      if (request.accountId() == null) {
        throw new BusinessRuleException(SETTLEMENT_ACCOUNT_REQUIRED);
      }
      refundAccount = accountService.requireActiveOwnedAccount(userId, request.accountId());
    }

    Instant now = Instant.now(clock);
    expense.setStatus(ExpenseStatus.REFUNDED);
    expense.setUpdatedAt(now);
    for (ExpenseInstallment installment : installments) {
      if (installment.getInvoice() != null
          && installment.getInvoice().getStatus() == CreditCardInvoiceStatus.PAID) {
        installment.setUpdatedAt(now);
        continue;
      }
      boolean liquidated = installmentHasLiquidation(installment);
      installment.setStatus(liquidated ? ExpenseStatus.REFUNDED : ExpenseStatus.CANCELLED);
      installment.setUpdatedAt(now);
    }
    expenseRepository.save(expense);
    expenseInstallmentRepository.saveAll(installments);

    if (request.settlement() == RefundExpenseRequest.RefundSettlement.CARD_CREDIT) {
      creditCardInvoiceService.persistCredit(
          card,
          totalLiquidated,
          "estorno da compra",
          CreditCardCreditOrigin.CARD_PURCHASE_REFUND,
          expense);
    } else {
      if (bankLiquidated.compareTo(BigDecimal.ZERO) > 0) {
        CardPurchaseAccountRefund refund = new CardPurchaseAccountRefund();
        refund.setId(UuidV7.create());
        refund.setUserId(userId);
        refund.setExpense(expense);
        refund.setAccount(refundAccount);
        refund.setAmount(bankLiquidated);
        refund.setCreatedAt(now);
        cardPurchaseAccountRefundRepository.save(refund);
      }
      creditCardInvoiceService.persistCredit(
          card,
          creditLiquidated,
          "estorno da compra",
          CreditCardCreditOrigin.CARD_PURCHASE_REFUND,
          expense);
    }
    creditCardInvoiceService.applyAvailableCredits(card);
    refreshAffectedInvoices(installments);
    return toResponse(expense, installments);
  }

  private boolean installmentHasLiquidation(ExpenseInstallment installment) {
    BigDecimal bank =
        zeroIfNull(
            invoicePaymentAllocationRepository.sumActiveAmountByInstallmentIdAndUserId(
                installment.getId(), installment.getUserId()));
    BigDecimal credit =
        zeroIfNull(
            creditApplicationRepository.sumAmountByInstallmentIdAndUserId(
                installment.getId(), installment.getUserId()));
    return bank.add(credit).compareTo(BigDecimal.ZERO) > 0;
  }

  private void refreshAffectedInvoices(List<ExpenseInstallment> installments) {
    installments.stream()
        .map(ExpenseInstallment::getInvoice)
        .filter(invoice -> invoice != null)
        .collect(
            Collectors.toMap(CreditCardInvoice::getId, invoice -> invoice, (left, right) -> left))
        .values()
        .forEach(creditCardInvoiceService::refreshOperationalState);
  }

  private Account resolveAccountForCreateOrUpdate(
      UUID userId, PaymentMethod paymentMethod, UUID accountId, UUID creditCardId) {
    if (paymentMethod == PaymentMethod.CREDIT_CARD) {
      if (accountId != null) {
        throw new BusinessRuleException(ACCOUNT_NOT_ALLOWED_FOR_CREDIT_CARD);
      }
      if (creditCardId == null) {
        throw new BusinessRuleException(CREDIT_CARD_REQUIRED);
      }
      return null;
    }
    if (creditCardId != null) {
      throw new BusinessRuleException(CREDIT_CARD_NOT_ALLOWED_FOR_ACCOUNT);
    }
    if (paymentMethod == PaymentMethod.ACCOUNT) {
      if (accountId == null) {
        throw new BusinessRuleException(ACCOUNT_REQUIRED_FOR_ACCOUNT_METHOD);
      }
      return accountService.requireActiveOwnedAccount(userId, accountId);
    }
    if (accountId != null) {
      throw new BusinessRuleException(ACCOUNT_NOT_ALLOWED_FOR_NONE);
    }
    return null;
  }

  private CreditCard resolveCreditCardForCreate(UUID userId, CreateExpenseRequest request) {
    if (request.paymentMethod() != PaymentMethod.CREDIT_CARD) {
      return null;
    }
    return creditCardService.requireActiveOwned(userId, request.creditCardId());
  }

  private void assertCreditCardUpdateAllowed(Expense expense, UpdateExpenseRequest request) {
    if (expense.getPaymentMethod() != PaymentMethod.CREDIT_CARD
        || request.paymentMethod() != PaymentMethod.CREDIT_CARD) {
      throw new BusinessRuleException(PAYMENT_METHOD_CREDIT_CARD_IMMUTABLE);
    }
    UUID currentCardId = expense.getCreditCard() == null ? null : expense.getCreditCard().getId();
    if (request.creditCardId() != null && !request.creditCardId().equals(currentCardId)) {
      throw new BusinessRuleException(PAYMENT_METHOD_CREDIT_CARD_IMMUTABLE);
    }
    if (request.totalAmount().compareTo(expense.getTotalAmount()) != 0) {
      throw new BusinessRuleException(CREDIT_CARD_TOTAL_IMMUTABLE);
    }
    if (!request.dueDate().equals(expense.getDueDate())) {
      throw new BusinessRuleException(CREDIT_CARD_DUE_DATE_IMMUTABLE);
    }
  }

  /** RN228: payments.account_id may differ from expenses.account_id. */
  private Account resolvePaymentAccount(UUID userId, Expense expense, UUID requestedAccountId) {
    if (expense.getPaymentMethod() == PaymentMethod.ACCOUNT) {
      UUID accountId =
          requestedAccountId == null ? expense.getAccount().getId() : requestedAccountId;
      return accountService.requireActiveOwnedAccount(userId, accountId);
    }
    if (requestedAccountId == null) {
      throw new BusinessRuleException(ACCOUNT_REQUIRED_FOR_PAYMENT);
    }
    return accountService.requireActiveOwnedAccount(userId, requestedAccountId);
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

  private void assertExpensePayable(Expense expense) {
    if (expense.getStatus() != ExpenseStatus.OPEN
        && expense.getStatus() != ExpenseStatus.PARTIALLY_PAID) {
      throw new BusinessRuleException(ONLY_OPEN_OR_PARTIAL_CAN_BE_PAID);
    }
  }

  private void assertExpenseAdjustable(Expense expense) {
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(INSTALLMENT_NOT_ADJUSTABLE);
    }
  }

  private void assertInstallmentPayable(Expense expense, ExpenseInstallment installment) {
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(INSTALLMENT_NOT_PAYABLE);
    }
    if (installment.getStatus() == ExpenseStatus.CANCELLED
        || installment.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(INSTALLMENT_NOT_PAYABLE);
    }
  }

  private void assertInstallmentAdjustable(Expense expense, ExpenseInstallment installment) {
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(INSTALLMENT_NOT_ADJUSTABLE);
    }
    if (installment.getStatus() == ExpenseStatus.CANCELLED
        || installment.getStatus() == ExpenseStatus.REFUNDED) {
      throw new BusinessRuleException(INSTALLMENT_NOT_ADJUSTABLE);
    }
    if (installment.getInvoice() != null
        && installment.getInvoice().getStatus() == CreditCardInvoiceStatus.PAID) {
      throw new BusinessRuleException(INVOICE_PAID_NO_ADJUSTMENT);
    }
  }

  private Expense requireOwnedExpense(UUID userId, UUID expenseId) {
    return expenseRepository
        .findByIdAndUserId(expenseId, userId)
        .orElseThrow(() -> new NotFoundException(EXPENSE_NOT_FOUND));
  }

  private Expense requireOwnedExpenseForUpdate(UUID userId, UUID expenseId) {
    return expenseRepository
        .findByIdAndUserIdForUpdate(expenseId, userId)
        .orElseThrow(() -> new NotFoundException(EXPENSE_NOT_FOUND));
  }

  private List<ExpenseInstallment> requireInstallments(Expense expense) {
    List<ExpenseInstallment> installments =
        expenseInstallmentRepository.findAllByExpense_IdAndUserIdOrderByInstallmentNumberAsc(
            expense.getId(), expense.getUserId());
    if (installments.isEmpty()) {
      throw new NotFoundException(INSTALLMENT_NOT_FOUND);
    }
    return installments;
  }

  private ExpenseInstallment requireSingleInstallmentForUpdate(UUID userId, UUID expenseId) {
    return expenseInstallmentRepository
        .findSingleByExpenseIdAndUserIdForUpdate(expenseId, userId)
        .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
  }

  private Map<UUID, List<ExpenseInstallment>> loadInstallmentsByExpense(
      UUID userId, List<Expense> expenses) {
    if (expenses.isEmpty()) {
      return Map.of();
    }
    List<UUID> ids = expenses.stream().map(Expense::getId).toList();
    return expenseInstallmentRepository
        .findAllByExpense_IdInAndUserIdOrderByExpense_IdAscInstallmentNumberAsc(ids, userId)
        .stream()
        .collect(
            Collectors.groupingBy(
                installment -> installment.getExpense().getId(),
                LinkedHashMap::new,
                Collectors.toList()));
  }

  private List<ExpenseInstallment> requireMappedInstallments(
      Expense expense, Map<UUID, List<ExpenseInstallment>> installmentsByExpense) {
    List<ExpenseInstallment> installments = installmentsByExpense.get(expense.getId());
    if (installments == null || installments.isEmpty()) {
      throw new NotFoundException(INSTALLMENT_NOT_FOUND);
    }
    return installments;
  }

  private ExpenseResponse toResponse(Expense expense, List<ExpenseInstallment> installments) {
    ExpenseInstallment first = installments.getFirst();
    return ExpenseResponse.from(expense, first.getId(), isExpenseOverdue(expense, installments));
  }

  private ExpenseInstallmentResponse toInstallmentResponse(
      Expense expense, ExpenseInstallment installment) {
    BigDecimal remaining =
        expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD
            ? installmentBalanceService.remaining(installment)
            : calculateRemaining(installment.getId(), expense.getUserId(), installment.getAmount());
    return ExpenseInstallmentResponse.from(
        installment, remaining, isInstallmentOverdue(expense, installment, remaining));
  }

  private boolean isExpenseOverdue(Expense expense, List<ExpenseInstallment> installments) {
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      return false;
    }
    if (installments.size() == 1) {
      if (expense.getStatus() != ExpenseStatus.OPEN
          && expense.getStatus() != ExpenseStatus.PARTIALLY_PAID) {
        return false;
      }
      return expense.getDueDate().isBefore(today());
    }
    return installments.stream()
        .anyMatch(
            installment -> {
              BigDecimal remaining =
                  expense.getPaymentMethod() == PaymentMethod.CREDIT_CARD
                      ? installmentBalanceService.remaining(installment)
                      : calculateRemaining(
                          installment.getId(), expense.getUserId(), installment.getAmount());
              return isInstallmentOverdue(expense, installment, remaining);
            });
  }

  /** RN241 with financial remaining. */
  private boolean isInstallmentOverdue(
      Expense expense, ExpenseInstallment installment, BigDecimal remaining) {
    if (expense.getStatus() == ExpenseStatus.CANCELLED
        || expense.getStatus() == ExpenseStatus.REFUNDED) {
      return false;
    }
    if (installment.getStatus() != ExpenseStatus.OPEN
        && installment.getStatus() != ExpenseStatus.PARTIALLY_PAID) {
      return false;
    }
    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    return installment.getDueDate().isBefore(today());
  }

  private LocalDate today() {
    return LocalDate.now(clock.withZone(FINANCIAL_ZONE));
  }

  private static void assertInstallmentSum(BigDecimal totalAmount, List<BigDecimal> amounts) {
    BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(totalAmount) != 0) {
      throw new BusinessRuleException(INSTALLMENT_SUM_MISMATCH);
    }
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private record InstallmentTotals(
      BigDecimal activePayments, BigDecimal activeDiscount, BigDecimal activeSurcharge) {}
}
