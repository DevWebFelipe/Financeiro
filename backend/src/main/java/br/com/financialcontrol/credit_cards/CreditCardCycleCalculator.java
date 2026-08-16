package br.com.financialcontrol.credit_cards;

import java.time.LocalDate;
import java.time.YearMonth;

/** Pure calendar rules for credit-card cycles (RN095, RN098, RN099B). No persistence, no Spring. */
public final class CreditCardCycleCalculator {

  private CreditCardCycleCalculator() {}

  /** Closing date of the cycle that owns {@code purchaseDate} (RN093–RN095). */
  public static LocalDate closingDateForPurchase(LocalDate purchaseDate, int closingDay) {
    YearMonth month = YearMonth.from(purchaseDate);
    LocalDate closingThisMonth = clampDay(month, closingDay);
    if (purchaseDate.isBefore(closingThisMonth)) {
      return closingThisMonth;
    }
    return clampDay(month.plusMonths(1), closingDay);
  }

  public static LocalDate closingDateForInstallment(
      LocalDate purchaseDate, int closingDay, int installmentNumber) {
    LocalDate firstClosing = closingDateForPurchase(purchaseDate, closingDay);
    if (installmentNumber <= 1) {
      return firstClosing;
    }
    YearMonth month = YearMonth.from(firstClosing).plusMonths(installmentNumber - 1L);
    return clampDay(month, closingDay);
  }

  /**
   * Invoice due date from the configured card days and the cycle {@code closingDate} (RN099B,
   * RN098). Compares configured {@code dueDay} and {@code closingDay}, not the effective day of
   * {@code closingDate}.
   */
  public static LocalDate dueDate(LocalDate closingDate, int closingDay, int dueDay) {
    YearMonth closingMonth = YearMonth.from(closingDate);
    if (dueDay > closingDay) {
      return clampDay(closingMonth, dueDay);
    }
    return clampDay(closingMonth.plusMonths(1), dueDay);
  }

  /** RN098: missing day in the month → last day of that month. */
  public static LocalDate clampDay(YearMonth month, int day) {
    int lastDay = month.lengthOfMonth();
    return month.atDay(Math.min(day, lastDay));
  }
}
