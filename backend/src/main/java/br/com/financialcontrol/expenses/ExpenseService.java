package br.com.financialcontrol.expenses;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryService;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.expenses.dto.CreateExpenseRequest;
import br.com.financialcontrol.expenses.dto.ExpensePageResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.expenses.dto.PayExpenseRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseRequest;
import br.com.financialcontrol.payments.Payment;
import br.com.financialcontrol.payments.PaymentRepository;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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
  static final String CREDIT_CARD_NOT_ALLOWED = "Despesas no cartão não são permitidas nesta fase.";
  static final String ACCOUNT_REQUIRED_FOR_ACCOUNT_METHOD =
      "A conta é obrigatória para despesa em conta.";
  static final String ACCOUNT_NOT_ALLOWED_FOR_NONE =
      "Despesa sem cartão não deve informar conta no cadastro.";
  static final String ACCOUNT_REQUIRED_FOR_PAYMENT = "A conta é obrigatória.";
  static final String ACCOUNT_MUST_MATCH_EXPENSE =
      "O pagamento de despesa em conta deve utilizar a mesma conta da despesa.";
  static final String OTHER_REQUIRES_NAME =
      "O nome do responsável é obrigatório quando o tipo for OTHER.";
  static final String ONLY_OPEN_CAN_BE_EDITED = "Somente despesas abertas podem ser editadas.";
  static final String ONLY_OPEN_CAN_BE_CANCELLED = "Somente despesas abertas podem ser canceladas.";
  static final String ONLY_OPEN_OR_PARTIAL_CAN_BE_PAID =
      "Somente despesas abertas ou parcialmente pagas podem ser pagas.";
  static final String ONLY_PAID_OR_PARTIAL_CAN_BE_REFUNDED =
      "Somente despesas pagas ou parcialmente pagas podem ser estornadas.";
  static final String PAYMENT_EXCEEDS_DUE = "O pagamento não pode ultrapassar o valor devido.";
  static final String INSUFFICIENT_BALANCE =
      "O pagamento não pode exceder o saldo disponível da conta.";
  static final String INVALID_PAGE = "A página deve ser maior ou igual a zero.";
  static final String INVALID_PAGE_SIZE = "O tamanho da página deve ser maior que zero.";
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  private final ExpenseRepository expenseRepository;
  private final ExpenseInstallmentRepository expenseInstallmentRepository;
  private final PaymentRepository paymentRepository;
  private final AccountService accountService;
  private final CategoryService categoryService;
  private final Clock clock;

  public ExpenseService(
      ExpenseRepository expenseRepository,
      ExpenseInstallmentRepository expenseInstallmentRepository,
      PaymentRepository paymentRepository,
      AccountService accountService,
      CategoryService categoryService,
      Clock clock) {
    this.expenseRepository = expenseRepository;
    this.expenseInstallmentRepository = expenseInstallmentRepository;
    this.paymentRepository = paymentRepository;
    this.accountService = accountService;
    this.categoryService = categoryService;
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
            responsibleType,
            paymentMethod,
            startDate,
            endDate,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
    Map<UUID, ExpenseInstallment> installments =
        loadSingleInstallments(userId, result.getContent());
    return new ExpensePageResponse(
        result.getContent().stream()
            .map(expense -> toResponse(expense, requireMappedInstallment(expense, installments)))
            .toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public ExpenseResponse get(AuthenticatedUser authenticatedUser, UUID expenseId) {
    Expense expense = requireOwnedExpense(authenticatedUser.userId(), expenseId);
    return toResponse(expense, requireSingleInstallment(expense));
  }

  @Transactional
  public ExpenseResponse create(AuthenticatedUser authenticatedUser, CreateExpenseRequest request) {
    UUID userId = authenticatedUser.userId();
    Category category =
        categoryService.requireActiveOwnedExpenseCategory(userId, request.categoryId());
    Account account =
        resolveAccountForCreateOrUpdate(userId, request.paymentMethod(), request.accountId());
    String responsibleName =
        resolveResponsibleName(request.responsibleType(), request.responsibleName());
    Instant now = Instant.now(clock);
    BigDecimal totalAmount = normalizeMoney(request.totalAmount());

    Expense expense = new Expense();
    expense.setId(UuidV7.create());
    expense.setUserId(userId);
    expense.setCategory(category);
    expense.setAccount(account);
    expense.setCreditCard(null);
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

    ExpenseInstallment installment = new ExpenseInstallment();
    installment.setId(UuidV7.create());
    installment.setUserId(userId);
    installment.setExpense(expense);
    installment.setInvoice(null);
    installment.setInstallmentNumber(1);
    installment.setTotalInstallments(1);
    installment.setAmount(totalAmount);
    installment.setDueDate(request.dueDate());
    installment.setStatus(ExpenseStatus.OPEN);
    installment.setCreatedAt(now);
    installment.setUpdatedAt(now);
    expenseInstallmentRepository.save(installment);

    return toResponse(expense, installment);
  }

  @Transactional
  public ExpenseResponse update(
      AuthenticatedUser authenticatedUser, UUID expenseId, UpdateExpenseRequest request) {
    Expense expense = requireOwnedExpenseForUpdate(authenticatedUser.userId(), expenseId);
    if (expense.getStatus() != ExpenseStatus.OPEN) {
      throw new BusinessRuleException(ONLY_OPEN_CAN_BE_EDITED);
    }
    ExpenseInstallment installment =
        requireSingleInstallmentForUpdate(authenticatedUser.userId(), expense.getId());
    Category category =
        categoryService.requireActiveOwnedExpenseCategory(
            authenticatedUser.userId(), request.categoryId());
    Account account =
        resolveAccountForCreateOrUpdate(
            authenticatedUser.userId(), request.paymentMethod(), request.accountId());
    BigDecimal totalAmount = normalizeMoney(request.totalAmount());
    Instant now = Instant.now(clock);

    expense.setCategory(category);
    expense.setAccount(account);
    expense.setCreditCard(null);
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

    expenseRepository.save(expense);
    expenseInstallmentRepository.save(installment);
    return toResponse(expense, installment);
  }

  @Transactional
  public ExpenseResponse pay(
      AuthenticatedUser authenticatedUser, UUID expenseId, PayExpenseRequest request) {
    UUID userId = authenticatedUser.userId();
    Expense expense = requireOwnedExpenseForUpdate(userId, expenseId);
    if (expense.getStatus() != ExpenseStatus.OPEN
        && expense.getStatus() != ExpenseStatus.PARTIALLY_PAID) {
      throw new BusinessRuleException(ONLY_OPEN_OR_PARTIAL_CAN_BE_PAID);
    }
    ExpenseInstallment installment = requireSingleInstallmentForUpdate(userId, expense.getId());
    Account account = resolvePaymentAccount(userId, expense, request.accountId());
    BigDecimal amount = normalizeMoney(request.amount());
    BigDecimal alreadyPaid =
        zeroIfNull(
            paymentRepository.sumAmountByInstallmentIdAndUserId(installment.getId(), userId));
    BigDecimal remaining = installment.getAmount().subtract(alreadyPaid);
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
    payment.setType(null);
    payment.setNotes(request.notes());
    payment.setCreatedAt(now);
    paymentRepository.save(payment);

    BigDecimal totalPaid = alreadyPaid.add(amount);
    ExpenseStatus nextStatus =
        totalPaid.compareTo(installment.getAmount()) == 0
            ? ExpenseStatus.PAID
            : ExpenseStatus.PARTIALLY_PAID;
    expense.setStatus(nextStatus);
    expense.setUpdatedAt(now);
    installment.setStatus(nextStatus);
    installment.setUpdatedAt(now);
    expenseRepository.save(expense);
    expenseInstallmentRepository.save(installment);
    return toResponse(expense, installment);
  }

  @Transactional
  public ExpenseResponse cancel(AuthenticatedUser authenticatedUser, UUID expenseId) {
    Expense expense = requireOwnedExpenseForUpdate(authenticatedUser.userId(), expenseId);
    if (expense.getStatus() != ExpenseStatus.OPEN) {
      throw new BusinessRuleException(ONLY_OPEN_CAN_BE_CANCELLED);
    }
    ExpenseInstallment installment =
        requireSingleInstallmentForUpdate(authenticatedUser.userId(), expense.getId());
    Instant now = Instant.now(clock);
    expense.setStatus(ExpenseStatus.CANCELLED);
    expense.setUpdatedAt(now);
    installment.setStatus(ExpenseStatus.CANCELLED);
    installment.setUpdatedAt(now);
    expenseRepository.save(expense);
    expenseInstallmentRepository.save(installment);
    return toResponse(expense, installment);
  }

  @Transactional
  public ExpenseResponse refund(AuthenticatedUser authenticatedUser, UUID expenseId) {
    Expense expense = requireOwnedExpenseForUpdate(authenticatedUser.userId(), expenseId);
    if (expense.getStatus() != ExpenseStatus.PARTIALLY_PAID
        && expense.getStatus() != ExpenseStatus.PAID) {
      throw new BusinessRuleException(ONLY_PAID_OR_PARTIAL_CAN_BE_REFUNDED);
    }
    ExpenseInstallment installment =
        requireSingleInstallmentForUpdate(authenticatedUser.userId(), expense.getId());
    Instant now = Instant.now(clock);
    expense.setStatus(ExpenseStatus.REFUNDED);
    expense.setUpdatedAt(now);
    installment.setStatus(ExpenseStatus.REFUNDED);
    installment.setUpdatedAt(now);
    expenseRepository.save(expense);
    expenseInstallmentRepository.save(installment);
    return toResponse(expense, installment);
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

  private Account resolveAccountForCreateOrUpdate(
      UUID userId, PaymentMethod paymentMethod, UUID accountId) {
    if (paymentMethod == PaymentMethod.CREDIT_CARD) {
      throw new BusinessRuleException(CREDIT_CARD_NOT_ALLOWED);
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

  private Account resolvePaymentAccount(UUID userId, Expense expense, UUID requestedAccountId) {
    if (expense.getPaymentMethod() == PaymentMethod.ACCOUNT) {
      UUID expenseAccountId = expense.getAccount().getId();
      UUID accountId = requestedAccountId == null ? expenseAccountId : requestedAccountId;
      if (!accountId.equals(expenseAccountId)) {
        throw new BusinessRuleException(ACCOUNT_MUST_MATCH_EXPENSE);
      }
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

  private ExpenseInstallment requireSingleInstallment(Expense expense) {
    return expenseInstallmentRepository
        .findByExpense_IdAndUserIdAndInstallmentNumber(expense.getId(), expense.getUserId(), 1)
        .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
  }

  private ExpenseInstallment requireSingleInstallmentForUpdate(UUID userId, UUID expenseId) {
    return expenseInstallmentRepository
        .findSingleByExpenseIdAndUserIdForUpdate(expenseId, userId)
        .orElseThrow(() -> new NotFoundException(INSTALLMENT_NOT_FOUND));
  }

  private Map<UUID, ExpenseInstallment> loadSingleInstallments(
      UUID userId, List<Expense> expenses) {
    if (expenses.isEmpty()) {
      return Map.of();
    }
    List<UUID> ids = expenses.stream().map(Expense::getId).toList();
    return expenseInstallmentRepository.findSingleByExpenseIdsAndUserId(ids, userId).stream()
        .collect(
            Collectors.toMap(installment -> installment.getExpense().getId(), Function.identity()));
  }

  private ExpenseInstallment requireMappedInstallment(
      Expense expense, Map<UUID, ExpenseInstallment> installments) {
    ExpenseInstallment installment = installments.get(expense.getId());
    if (installment == null) {
      throw new NotFoundException(INSTALLMENT_NOT_FOUND);
    }
    return installment;
  }

  private ExpenseResponse toResponse(Expense expense, ExpenseInstallment installment) {
    return ExpenseResponse.from(expense, installment.getId(), isOverdue(expense));
  }

  private boolean isOverdue(Expense expense) {
    if (expense.getStatus() != ExpenseStatus.OPEN
        && expense.getStatus() != ExpenseStatus.PARTIALLY_PAID) {
      return false;
    }
    return expense.getDueDate().isBefore(LocalDate.now(clock.withZone(FINANCIAL_ZONE)));
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
