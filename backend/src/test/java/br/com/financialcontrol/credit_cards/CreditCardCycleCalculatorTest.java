package br.com.financialcontrol.credit_cards;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CreditCardCycleCalculatorTest {

  @Test
  void purchaseBeforeClosingBelongsToCurrentCycle() {
    LocalDate closing =
        CreditCardCycleCalculator.closingDateForPurchase(LocalDate.of(2026, 8, 9), 10);
    assertThat(closing).isEqualTo(LocalDate.of(2026, 8, 10));
  }

  @Test
  void purchaseOnClosingDayBelongsToNextCycle() {
    LocalDate closing =
        CreditCardCycleCalculator.closingDateForPurchase(LocalDate.of(2026, 8, 10), 10);
    assertThat(closing).isEqualTo(LocalDate.of(2026, 9, 10));
  }

  @Test
  void purchaseAfterClosingBelongsToNextCycle() {
    LocalDate closing =
        CreditCardCycleCalculator.closingDateForPurchase(LocalDate.of(2026, 8, 11), 10);
    assertThat(closing).isEqualTo(LocalDate.of(2026, 9, 10));
  }

  @Test
  void closingDay31InFebruaryUsesLastDayOfMonth() {
    LocalDate closing =
        CreditCardCycleCalculator.closingDateForPurchase(LocalDate.of(2026, 2, 10), 31);
    assertThat(closing).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void dueDayAfterClosingDayStaysInSameMonth() {
    LocalDate due = CreditCardCycleCalculator.dueDate(LocalDate.of(2026, 9, 10), 10, 20);
    assertThat(due).isEqualTo(LocalDate.of(2026, 9, 20));
  }

  @Test
  void dueDayBeforeOrEqualClosingDayGoesToNextMonth() {
    assertThat(CreditCardCycleCalculator.dueDate(LocalDate.of(2026, 8, 25), 25, 5))
        .isEqualTo(LocalDate.of(2026, 9, 5));
    assertThat(CreditCardCycleCalculator.dueDate(LocalDate.of(2026, 9, 10), 10, 10))
        .isEqualTo(LocalDate.of(2026, 10, 10));
  }

  @Test
  void dueDayEqualClosingDay31InFebruaryGoesToMarch() {
    LocalDate due = CreditCardCycleCalculator.dueDate(LocalDate.of(2026, 2, 28), 31, 31);
    assertThat(due).isEqualTo(LocalDate.of(2026, 3, 31));
  }

  @Test
  void dueDay31AfterJanuaryClosingUsesFebruaryLastDay() {
    LocalDate due = CreditCardCycleCalculator.dueDate(LocalDate.of(2026, 1, 31), 31, 31);
    assertThat(due).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void laterInstallmentsAdvanceClosingMonth() {
    LocalDate first =
        CreditCardCycleCalculator.closingDateForInstallment(LocalDate.of(2026, 8, 11), 10, 1);
    LocalDate second =
        CreditCardCycleCalculator.closingDateForInstallment(LocalDate.of(2026, 8, 11), 10, 2);
    assertThat(first).isEqualTo(LocalDate.of(2026, 9, 10));
    assertThat(second).isEqualTo(LocalDate.of(2026, 10, 10));
  }
}
