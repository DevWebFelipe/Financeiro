package br.com.financialcontrol.credit_card_invoices;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Pure RN247 allocation: remaining ASC, due_date ASC, id ASC, HALF_UP, residual on the last line.
 * Never uses double/float.
 */
public final class InvoiceAllocationCalculator {

  private static final RoundingMode HALF_UP = RoundingMode.HALF_UP;

  private InvoiceAllocationCalculator() {}

  public record Line(UUID installmentId, LocalDate dueDate, BigDecimal remaining) {}

  public record Share(UUID installmentId, BigDecimal amount) {}

  public static List<Share> allocate(BigDecimal amount, List<Line> lines) {
    BigDecimal toAllocate = normalize(amount);
    if (toAllocate.compareTo(BigDecimal.ZERO) <= 0) {
      return List.of();
    }
    List<Line> eligible =
        lines.stream()
            .filter(line -> line.remaining().compareTo(BigDecimal.ZERO) > 0)
            .sorted(
                Comparator.comparing(Line::remaining)
                    .thenComparing(Line::dueDate)
                    .thenComparing(Line::installmentId))
            .toList();
    if (eligible.isEmpty()) {
      return List.of();
    }

    BigDecimal totalRemaining =
        eligible.stream().map(Line::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal target = toAllocate.min(totalRemaining);
    if (eligible.size() == 1) {
      BigDecimal share = target.min(eligible.getFirst().remaining());
      return share.compareTo(BigDecimal.ZERO) > 0
          ? List.of(new Share(eligible.getFirst().installmentId(), normalize(share)))
          : List.of();
    }

    List<Share> shares = new ArrayList<>();
    BigDecimal leftover = target;
    for (int i = 0; i < eligible.size() - 1; i++) {
      Line line = eligible.get(i);
      BigDecimal proportional =
          target.multiply(line.remaining()).divide(totalRemaining, 2, HALF_UP);
      BigDecimal share = proportional.min(line.remaining()).min(leftover);
      share = normalize(share);
      if (share.compareTo(BigDecimal.ZERO) < 0) {
        share = BigDecimal.ZERO.setScale(2, HALF_UP);
      }
      leftover = leftover.subtract(share);
      if (share.compareTo(BigDecimal.ZERO) > 0) {
        shares.add(new Share(line.installmentId(), share));
      }
    }

    Line last = eligible.getLast();
    BigDecimal lastShare = leftover.min(last.remaining());
    lastShare = normalize(lastShare);
    if (lastShare.compareTo(BigDecimal.ZERO) > 0) {
      shares.add(new Share(last.installmentId(), lastShare));
    }
    return List.copyOf(shares);
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(2, HALF_UP);
  }
}
