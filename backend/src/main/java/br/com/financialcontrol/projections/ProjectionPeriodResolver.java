package br.com.financialcontrol.projections;

import br.com.financialcontrol.config.InvalidRequestException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

final class ProjectionPeriodResolver {

  static final String INVALID_DATA = "Dados inválidos.";
  private static final int MAX_MONTHS = 12;

  private ProjectionPeriodResolver() {}

  static ProjectionHorizon resolve(
      LocalDate asOfDate,
      LocalDate startDate,
      LocalDate endDate,
      Integer year,
      Integer month,
      Integer months) {
    boolean hasRange = startDate != null || endDate != null;
    boolean hasYearOrMonth = year != null || month != null;
    boolean hasMonths = months != null;
    if (hasRange && hasYearOrMonth) {
      throw invalid();
    }
    if (hasRange && hasMonths) {
      throw invalid();
    }
    if ((year == null) != (month == null)) {
      throw invalid();
    }
    if ((startDate == null) != (endDate == null)) {
      throw invalid();
    }
    if (hasMonths && (months < 1 || months > MAX_MONTHS)) {
      throw invalid();
    }
    if (month != null && (month < 1 || month > 12)) {
      throw invalid();
    }

    YearMonth first;
    YearMonth last;
    LocalDate requestedStart;
    LocalDate requestedEnd;
    if (startDate != null) {
      if (startDate.isAfter(endDate)) {
        throw invalid();
      }
      requestedStart = startDate;
      requestedEnd = endDate;
      first = YearMonth.from(startDate);
      last = YearMonth.from(endDate);
    } else if (year != null) {
      first = YearMonth.of(year, month);
      int count = hasMonths ? months : 1;
      last = first.plusMonths(count - 1L);
      requestedStart = first.atDay(1);
      requestedEnd = last.atEndOfMonth();
    } else if (hasMonths) {
      first = YearMonth.from(asOfDate);
      last = first.plusMonths(months - 1L);
      requestedStart = first.atDay(1);
      requestedEnd = last.atEndOfMonth();
    } else {
      first = YearMonth.from(asOfDate);
      last = first.plusMonths(MAX_MONTHS - 1L);
      requestedStart = first.atDay(1);
      requestedEnd = last.atEndOfMonth();
    }

    if (ChronoUnit.MONTHS.between(first, last) + 1 > MAX_MONTHS) {
      throw invalid();
    }
    if (requestedEnd.isBefore(asOfDate)) {
      throw invalid();
    }

    LocalDate effectiveStart = requestedStart.isBefore(asOfDate) ? asOfDate : requestedStart;
    List<YearMonth> returned = new ArrayList<>();
    YearMonth cursor = YearMonth.from(effectiveStart);
    YearMonth lastReturned = YearMonth.from(requestedEnd);
    while (!cursor.isAfter(lastReturned)) {
      returned.add(cursor);
      cursor = cursor.plusMonths(1);
    }
    return new ProjectionHorizon(asOfDate, effectiveStart, requestedEnd, List.copyOf(returned));
  }

  private static InvalidRequestException invalid() {
    return new InvalidRequestException(INVALID_DATA);
  }
}
