package br.com.financialcontrol.projections;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financialcontrol.projections.dto.ProjectionMonthResponse;
import br.com.financialcontrol.projections.dto.ProjectionQuarterResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectionCalculatorTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 8, 17);
  private final ProjectionCalculator calculator = new ProjectionCalculator();

  @Test
  void shouldKeepBalanceConstantWhenThereAreNoDatedEvents() {
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(money("10000.00"), money("0.00"), augustHorizon(), List.of());

    assertThat(result.summary().currentBalance()).isEqualByComparingTo("10000.00");
    assertThat(result.summary().projectedFinalBalance()).isEqualByComparingTo("10000.00");
    assertThat(result.summary().projectedIncome()).isEqualByComparingTo("0.00");
    assertThat(result.summary().projectedExpense()).isEqualByComparingTo("0.00");
    assertThat(result.months()).hasSize(1);
    assertThat(result.months().getFirst().closingBalance()).isEqualByComparingTo("10000.00");
    assertThat(result.datedEvents()).isEmpty();
    assertThat(result.undatedEvents()).isEmpty();
  }

  @Test
  void shouldApplyFutureIncomeAndExpenseToCurrentBalance() {
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("1000.00"),
            money("0.00"),
            augustHorizon(),
            List.of(
                income("1000.00", LocalDate.of(2026, 8, 20), false),
                expense("5000.00", LocalDate.of(2026, 8, 25), false)));

    assertThat(result.summary().projectedIncome()).isEqualByComparingTo("1000.00");
    assertThat(result.summary().projectedExpense()).isEqualByComparingTo("5000.00");
    assertThat(result.summary().projectedNetCashFlow()).isEqualByComparingTo("-4000.00");
    assertThat(result.summary().projectedFinalBalance()).isEqualByComparingTo("-3000.00");
    assertThat(result.summary().minimumProjectedBalance()).isEqualByComparingTo("-3000.00");
    assertThat(result.summary().minimumProjectedBalanceDate()).isEqualTo(LocalDate.of(2026, 8, 25));
    assertThat(result.months().getFirst().negative()).isTrue();
    assertThat(result.months().getFirst().closingBalance()).isEqualByComparingTo("-3000.00");
  }

  @Test
  void shouldPlaceOverdueEventsInTheFirstPeriod() {
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("100.00"),
            money("0.00"),
            augustHorizon(),
            List.of(income("50.00", LocalDate.of(2026, 7, 10), true)));

    assertThat(result.months().getFirst().totalIncome()).isEqualByComparingTo("50.00");
    assertThat(result.months().getFirst().closingBalance()).isEqualByComparingTo("150.00");
    assertThat(result.datedEvents().getFirst().date()).isEqualTo(LocalDate.of(2026, 7, 10));
    assertThat(result.datedEvents().getFirst().overdue()).isTrue();
  }

  @Test
  void shouldKeepUndatedEventsOutOfBalancesAndInASeparateGroup() {
    ProjectionEventInput undated =
        new ProjectionEventInput(
            UUID.fromString("01900000-0000-7000-8000-000000000099"),
            ProjectionEventType.EXPENSE,
            "Sem data",
            money("999.00"),
            ProjectionDirection.OUT,
            null,
            false);
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("500.00"),
            money("0.00"),
            augustHorizon(),
            List.of(income("100.00", LocalDate.of(2026, 8, 20), false), undated));

    assertThat(result.months().getFirst().closingBalance()).isEqualByComparingTo("600.00");
    assertThat(result.summary().projectedFinalBalance()).isEqualByComparingTo("600.00");
    assertThat(result.summary().projectedExpense()).isEqualByComparingTo("0.00");
    assertThat(result.undatedEvents()).hasSize(1);
    assertThat(result.undatedEvents().getFirst().date()).isNull();
    assertThat(result.undatedEvents().getFirst().amount()).isEqualByComparingTo("999.00");
    assertThat(result.datedEvents()).hasSize(1);
  }

  @Test
  void shouldChainMonthlyOpeningAndClosingBalances() {
    ProjectionHorizon horizon =
        new ProjectionHorizon(
            AS_OF,
            AS_OF,
            LocalDate.of(2026, 10, 31),
            List.of(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)));
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("100.00"),
            money("0.00"),
            horizon,
            List.of(
                income("40.00", LocalDate.of(2026, 8, 20), false),
                expense("10.00", LocalDate.of(2026, 9, 5), false),
                income("20.00", LocalDate.of(2026, 10, 1), false)));

    ProjectionMonthResponse august = result.months().get(0);
    ProjectionMonthResponse september = result.months().get(1);
    ProjectionMonthResponse october = result.months().get(2);
    assertThat(august.openingBalance()).isEqualByComparingTo("100.00");
    assertThat(august.closingBalance()).isEqualByComparingTo("140.00");
    assertThat(september.openingBalance()).isEqualByComparingTo("140.00");
    assertThat(september.closingBalance()).isEqualByComparingTo("130.00");
    assertThat(october.openingBalance()).isEqualByComparingTo("130.00");
    assertThat(october.closingBalance()).isEqualByComparingTo("150.00");
    assertThat(result.summary().projectedFinalBalance()).isEqualByComparingTo("150.00");
  }

  @Test
  void shouldIncludeCompleteCalendarQuartersOnly() {
    ProjectionHorizon horizon =
        new ProjectionHorizon(
            AS_OF,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 12, 31),
            List.of(YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12)));
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("0.00"),
            money("0.00"),
            horizon,
            List.of(
                income("10.00", LocalDate.of(2026, 10, 2), false),
                expense("3.00", LocalDate.of(2026, 11, 10), false),
                income("5.00", LocalDate.of(2026, 12, 1), false)));

    assertThat(result.quarters()).hasSize(1);
    ProjectionQuarterResponse quarter = result.quarters().getFirst();
    assertThat(quarter.period()).isEqualTo("2026-Q4");
    assertThat(quarter.months()).containsExactly("2026-10", "2026-11", "2026-12");
    assertThat(quarter.totalIncome()).isEqualByComparingTo("15.00");
    assertThat(quarter.totalExpense()).isEqualByComparingTo("3.00");
    assertThat(quarter.netCashFlow()).isEqualByComparingTo("12.00");
    assertThat(quarter.openingBalance()).isEqualByComparingTo("0.00");
    assertThat(quarter.closingBalance()).isEqualByComparingTo("12.00");
  }

  @Test
  void shouldNotInventAPartialQuarter() {
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(money("0.00"), money("0.00"), augustHorizon(), List.of());
    assertThat(result.quarters()).isEmpty();
  }

  @Test
  void shouldExposeReservedAmountWithoutReducingClosingBalance() {
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("1000.00"),
            money("200.00"),
            augustHorizon(),
            List.of(income("100.00", LocalDate.of(2026, 8, 20), false)));

    assertThat(result.months().getFirst().closingBalance()).isEqualByComparingTo("1100.00");
    assertThat(result.months().getFirst().reservedAmount()).isEqualByComparingTo("200.00");
    assertThat(result.months().getFirst().availableProjectedBalance())
        .isEqualByComparingTo("900.00");
    assertThat(result.summary().reservedAmount()).isEqualByComparingTo("200.00");
    assertThat(result.summary().availableProjectedBalance()).isEqualByComparingTo("900.00");
  }

  @Test
  void shouldApplyInterveningEventsToTheOpeningOfAFutureFirstMonth() {
    ProjectionHorizon horizon =
        new ProjectionHorizon(
            AS_OF,
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 31),
            List.of(YearMonth.of(2026, 12)));
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("100.00"),
            money("0.00"),
            horizon,
            List.of(
                income("30.00", LocalDate.of(2026, 8, 20), false),
                expense("10.00", LocalDate.of(2026, 12, 10), false)));

    assertThat(result.months().getFirst().openingBalance()).isEqualByComparingTo("130.00");
    assertThat(result.months().getFirst().totalExpense()).isEqualByComparingTo("10.00");
    assertThat(result.months().getFirst().closingBalance()).isEqualByComparingTo("120.00");
    assertThat(result.summary().projectedFinalBalance()).isEqualByComparingTo("120.00");
    assertThat(result.datedEvents()).hasSize(2);
  }

  @Test
  void shouldUseAsOfDateAsMinimumDateWhenOpeningIsTheMinimum() {
    ProjectionCalculator.ProjectionComputation result =
        calculator.calculate(
            money("50.00"),
            money("0.00"),
            augustHorizon(),
            List.of(income("20.00", LocalDate.of(2026, 8, 20), false)));

    assertThat(result.summary().minimumProjectedBalance()).isEqualByComparingTo("50.00");
    assertThat(result.summary().minimumProjectedBalanceDate()).isEqualTo(AS_OF);
  }

  private static ProjectionHorizon augustHorizon() {
    return new ProjectionHorizon(
        AS_OF, AS_OF, LocalDate.of(2026, 8, 31), List.of(YearMonth.of(2026, 8)));
  }

  private static ProjectionEventInput income(String amount, LocalDate date, boolean overdue) {
    return new ProjectionEventInput(
        UUID.randomUUID(),
        ProjectionEventType.INCOME,
        "Receita",
        money(amount),
        ProjectionDirection.IN,
        date,
        overdue);
  }

  private static ProjectionEventInput expense(String amount, LocalDate date, boolean overdue) {
    return new ProjectionEventInput(
        UUID.randomUUID(),
        ProjectionEventType.EXPENSE,
        "Despesa",
        money(amount),
        ProjectionDirection.OUT,
        date,
        overdue);
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }
}
