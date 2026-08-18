package br.com.financialcontrol.projections;

import br.com.financialcontrol.projections.dto.ProjectionEventResponse;
import br.com.financialcontrol.projections.dto.ProjectionMonthResponse;
import br.com.financialcontrol.projections.dto.ProjectionQuarterResponse;
import br.com.financialcontrol.projections.dto.ProjectionSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProjectionCalculator {

  public ProjectionComputation calculate(
      BigDecimal currentBalance,
      BigDecimal reservedAmount,
      ProjectionHorizon horizon,
      List<ProjectionEventInput> inputs) {
    BigDecimal opening = money(currentBalance);
    BigDecimal reserved = money(reservedAmount);
    List<ProjectionEventInput> undatedInputs = new ArrayList<>();
    List<DatedEvent> dated = new ArrayList<>();
    for (ProjectionEventInput input : inputs) {
      if (input.date() == null) {
        undatedInputs.add(input);
        continue;
      }
      LocalDate effectiveDate = effectiveDate(input.date(), horizon.asOfDate());
      if (effectiveDate.isAfter(horizon.endDate())) {
        continue;
      }
      dated.add(new DatedEvent(input, effectiveDate, toResponse(input)));
    }
    dated.sort(
        Comparator.comparing(DatedEvent::effectiveDate)
            .thenComparing(event -> event.input().date())
            .thenComparing(event -> event.input().sourceId()));

    List<YearMonth> months = horizon.months();
    YearMonth firstMonth = months.getFirst();
    LocalDate firstMonthStart =
        YearMonth.from(horizon.asOfDate()).equals(firstMonth)
            ? horizon.asOfDate()
            : firstMonth.atDay(1);

    BigDecimal monthOpening = opening;
    BigDecimal projectedIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal projectedExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (DatedEvent event : dated) {
      projectedIncome = addIf(projectedIncome, event, ProjectionDirection.IN);
      projectedExpense = addIf(projectedExpense, event, ProjectionDirection.OUT);
      if (event.effectiveDate().isBefore(firstMonthStart)) {
        monthOpening = apply(monthOpening, event);
      }
    }

    List<ProjectionMonthResponse> monthResponses = new ArrayList<>();
    for (YearMonth month : months) {
      LocalDate monthStart =
          YearMonth.from(horizon.asOfDate()).equals(month) ? horizon.asOfDate() : month.atDay(1);
      LocalDate monthEnd = month.atEndOfMonth();
      if (monthEnd.isAfter(horizon.endDate())) {
        monthEnd = horizon.endDate();
      }
      BigDecimal totalIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      BigDecimal totalExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      BigDecimal running = monthOpening;
      BigDecimal monthMin = running;
      LocalDate monthMinDate = monthStart;
      for (DatedEvent event : dated) {
        if (event.effectiveDate().isBefore(monthStart) || event.effectiveDate().isAfter(monthEnd)) {
          continue;
        }
        totalIncome = addIf(totalIncome, event, ProjectionDirection.IN);
        totalExpense = addIf(totalExpense, event, ProjectionDirection.OUT);
        running = apply(running, event);
        if (running.compareTo(monthMin) < 0) {
          monthMin = running;
          monthMinDate = event.effectiveDate();
        }
      }
      BigDecimal net = money(totalIncome.subtract(totalExpense));
      BigDecimal closing = money(monthOpening.add(net));
      monthResponses.add(
          new ProjectionMonthResponse(
              month.toString(),
              monthOpening,
              totalIncome,
              totalExpense,
              net,
              closing,
              monthMin,
              monthMinDate,
              closing.compareTo(BigDecimal.ZERO) < 0 || monthMin.compareTo(BigDecimal.ZERO) < 0,
              reserved,
              money(closing.subtract(reserved))));
      monthOpening = closing;
    }

    BigDecimal running = opening;
    BigDecimal horizonMin = running;
    LocalDate horizonMinDate = horizon.asOfDate();
    for (DatedEvent event : dated) {
      running = apply(running, event);
      if (running.compareTo(horizonMin) < 0) {
        horizonMin = running;
        horizonMinDate = event.effectiveDate();
      }
    }
    BigDecimal projectedNet = money(projectedIncome.subtract(projectedExpense));
    BigDecimal projectedFinal = monthResponses.getLast().closingBalance();
    ProjectionSummaryResponse summary =
        new ProjectionSummaryResponse(
            opening,
            projectedFinal,
            projectedIncome,
            projectedExpense,
            projectedNet,
            horizonMin,
            horizonMinDate,
            reserved,
            money(projectedFinal.subtract(reserved)));
    return new ProjectionComputation(
        horizon.startDate(),
        horizon.endDate(),
        summary,
        List.copyOf(monthResponses),
        quarters(monthResponses),
        dated.stream().map(DatedEvent::response).toList(),
        undatedInputs.stream().map(ProjectionCalculator::toResponse).toList());
  }

  private static List<ProjectionQuarterResponse> quarters(List<ProjectionMonthResponse> months) {
    Map<String, ProjectionMonthResponse> byPeriod = new HashMap<>();
    for (ProjectionMonthResponse month : months) {
      byPeriod.put(month.period(), month);
    }
    if (months.isEmpty()) {
      return List.of();
    }
    YearMonth first = YearMonth.parse(months.getFirst().period());
    YearMonth last = YearMonth.parse(months.getLast().period());
    List<ProjectionQuarterResponse> result = new ArrayList<>();
    int year = first.getYear();
    int lastYear = last.getYear();
    for (int currentYear = year; currentYear <= lastYear; currentYear++) {
      for (int quarter = 1; quarter <= 4; quarter++) {
        int startMonth = (quarter - 1) * 3 + 1;
        List<String> quarterMonths = new ArrayList<>();
        boolean complete = true;
        for (int offset = 0; offset < 3; offset++) {
          String period = YearMonth.of(currentYear, startMonth + offset).toString();
          if (!byPeriod.containsKey(period)) {
            complete = false;
            break;
          }
          quarterMonths.add(period);
        }
        if (!complete) {
          continue;
        }
        ProjectionMonthResponse firstMonth = byPeriod.get(quarterMonths.getFirst());
        ProjectionMonthResponse lastMonth = byPeriod.get(quarterMonths.getLast());
        BigDecimal totalIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (String period : quarterMonths) {
          ProjectionMonthResponse month = byPeriod.get(period);
          totalIncome = money(totalIncome.add(month.totalIncome()));
          totalExpense = money(totalExpense.add(month.totalExpense()));
        }
        result.add(
            new ProjectionQuarterResponse(
                currentYear + "-Q" + quarter,
                List.copyOf(quarterMonths),
                totalIncome,
                totalExpense,
                money(totalIncome.subtract(totalExpense)),
                firstMonth.openingBalance(),
                lastMonth.closingBalance()));
      }
    }
    return List.copyOf(result);
  }

  private static LocalDate effectiveDate(LocalDate date, LocalDate asOfDate) {
    return date.isBefore(asOfDate) ? asOfDate : date;
  }

  private static BigDecimal addIf(
      BigDecimal total, DatedEvent event, ProjectionDirection direction) {
    if (event.input().direction() != direction) {
      return total;
    }
    return money(total.add(event.input().amount()));
  }

  private static BigDecimal apply(BigDecimal balance, DatedEvent event) {
    if (event.input().direction() == ProjectionDirection.IN) {
      return money(balance.add(event.input().amount()));
    }
    return money(balance.subtract(event.input().amount()));
  }

  private static ProjectionEventResponse toResponse(ProjectionEventInput input) {
    return new ProjectionEventResponse(
        input.date(),
        input.type(),
        input.description(),
        money(input.amount()),
        input.direction(),
        input.sourceId(),
        input.type(),
        input.overdue(),
        ProjectionAccountAssignment.UNASSIGNED);
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  public record ProjectionComputation(
      LocalDate startDate,
      LocalDate endDate,
      ProjectionSummaryResponse summary,
      List<ProjectionMonthResponse> months,
      List<ProjectionQuarterResponse> quarters,
      List<ProjectionEventResponse> datedEvents,
      List<ProjectionEventResponse> undatedEvents) {}

  private record DatedEvent(
      ProjectionEventInput input, LocalDate effectiveDate, ProjectionEventResponse response) {}
}
