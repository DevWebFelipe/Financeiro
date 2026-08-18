package br.com.financialcontrol.dashboard;

import br.com.financialcontrol.accounts.Account;
import br.com.financialcontrol.accounts.AccountService;
import br.com.financialcontrol.credit_cards.CreditCardService;
import br.com.financialcontrol.credit_cards.dto.CreditCardLimitResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.dashboard.dto.DashboardAccountBalanceResponse;
import br.com.financialcontrol.dashboard.dto.DashboardBalanceResponse;
import br.com.financialcontrol.dashboard.dto.DashboardCreditCardResponse;
import br.com.financialcontrol.dashboard.dto.DashboardPayablesResponse;
import br.com.financialcontrol.dashboard.dto.DashboardProjectionResponse;
import br.com.financialcontrol.dashboard.dto.DashboardResponse;
import br.com.financialcontrol.payables.PayablesService;
import br.com.financialcontrol.payables.PayablesSummary;
import br.com.financialcontrol.projections.ProjectionService;
import br.com.financialcontrol.projections.dto.ProjectionResponse;
import br.com.financialcontrol.receivables.ReceivablesService;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  static final String INVALID_DATA = "Dados inválidos.";
  static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");

  private final AccountService accountService;
  private final ProjectionService projectionService;
  private final PayablesService payablesService;
  private final ReceivablesService receivablesService;
  private final CreditCardService creditCardService;
  private final Clock clock;

  public DashboardService(
      AccountService accountService,
      ProjectionService projectionService,
      PayablesService payablesService,
      ReceivablesService receivablesService,
      CreditCardService creditCardService,
      Clock clock) {
    this.accountService = accountService;
    this.projectionService = projectionService;
    this.payablesService = payablesService;
    this.receivablesService = receivablesService;
    this.creditCardService = creditCardService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public DashboardResponse load(
      AuthenticatedUser authenticatedUser,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      Integer months) {
    LocalDate asOfDate = LocalDate.now(clock.withZone(FINANCIAL_ZONE));
    ProjectionResponse projection =
        projectionService.project(
            authenticatedUser, startDate, endDate, year, month, months, null, 0, 1);
    List<DashboardAccountBalanceResponse> accounts = activeAccountBalances(authenticatedUser);
    DashboardBalanceResponse balance = consolidate(accounts);
    PayablesSummary payables = payablesService.summarize(authenticatedUser);
    return new DashboardResponse(
        asOfDate,
        projection.startDate(),
        projection.endDate(),
        balance,
        new DashboardProjectionResponse(
            projection.summary(), projection.months(), projection.quarters()),
        toPayablesResponse(payables),
        receivablesService
            .list(
                authenticatedUser,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                1)
            .summary(),
        accounts,
        creditCards(authenticatedUser, payables));
  }

  private List<DashboardAccountBalanceResponse> activeAccountBalances(
      AuthenticatedUser authenticatedUser) {
    List<DashboardAccountBalanceResponse> accounts = new ArrayList<>();
    for (Account account : accountService.listOwned(authenticatedUser)) {
      if (!account.isActive()) {
        continue;
      }
      BigDecimal total = money(accountService.calculateCurrentBalance(account));
      BigDecimal reserved = money(accountService.calculateReservedAmount(account));
      accounts.add(
          new DashboardAccountBalanceResponse(
              account.getId(),
              account.getName(),
              account.getType(),
              total,
              reserved,
              money(total.subtract(reserved))));
    }
    return List.copyOf(accounts);
  }

  private static DashboardBalanceResponse consolidate(
      List<DashboardAccountBalanceResponse> accounts) {
    BigDecimal total = money(BigDecimal.ZERO);
    BigDecimal reserved = money(BigDecimal.ZERO);
    for (DashboardAccountBalanceResponse account : accounts) {
      total = money(total.add(account.totalBalance()));
      reserved = money(reserved.add(account.reservedAmount()));
    }
    return new DashboardBalanceResponse(total, reserved, money(total.subtract(reserved)));
  }

  private static DashboardPayablesResponse toPayablesResponse(PayablesSummary summary) {
    return new DashboardPayablesResponse(
        summary.totalRemaining(),
        summary.installmentRemaining(),
        summary.invoiceRemaining(),
        summary.overdueRemaining(),
        summary.overdueInstallmentRemaining(),
        summary.overdueInvoiceRemaining(),
        summary.openCount(),
        summary.overdueCount());
  }

  private List<DashboardCreditCardResponse> creditCards(
      AuthenticatedUser authenticatedUser, PayablesSummary payables) {
    List<DashboardCreditCardResponse> cards = new ArrayList<>();
    for (CreditCardResponse card : creditCardService.list(authenticatedUser, null)) {
      if (!card.active()) {
        continue;
      }
      CreditCardLimitResponse limit = creditCardService.getLimit(authenticatedUser, card.id());
      cards.add(
          new DashboardCreditCardResponse(
              card.id(),
              card.name(),
              limit.creditLimit(),
              limit.usedLimit(),
              limit.availableLimit(),
              cardAmount(payables.invoiceRemainingByCardId(), card.id()),
              cardAmount(payables.overdueInvoiceRemainingByCardId(), card.id())));
    }
    return List.copyOf(cards);
  }

  private static BigDecimal cardAmount(Map<UUID, BigDecimal> amounts, UUID creditCardId) {
    BigDecimal amount = amounts.get(creditCardId);
    return amount == null ? money(BigDecimal.ZERO) : money(amount);
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
