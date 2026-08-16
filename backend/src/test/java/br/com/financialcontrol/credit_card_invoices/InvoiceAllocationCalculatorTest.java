package br.com.financialcontrol.credit_card_invoices;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financialcontrol.credit_card_invoices.InvoiceAllocationCalculator.Line;
import br.com.financialcontrol.credit_card_invoices.InvoiceAllocationCalculator.Share;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvoiceAllocationCalculatorTest {

  private static final UUID ID_A = UUID.fromString("01800000-0000-7000-8000-00000000000a");
  private static final UUID ID_B = UUID.fromString("01800000-0000-7000-8000-00000000000b");
  private static final UUID ID_C = UUID.fromString("01800000-0000-7000-8000-00000000000c");

  @Test
  void allocatesProportionallyToRemainingWithHalfUp() {
    List<Share> shares =
        InvoiceAllocationCalculator.allocate(
            new BigDecimal("100.00"),
            List.of(
                line(ID_C, "2026-09-10", "50.00"),
                line(ID_A, "2026-08-10", "30.00"),
                line(ID_B, "2026-08-20", "20.00")));

    assertThat(shares).hasSize(3);
    assertExactSum(shares, "100.00");
    assertThat(shareOf(shares, ID_B)).isEqualByComparingTo("20.00");
    assertThat(shareOf(shares, ID_A)).isEqualByComparingTo("30.00");
    assertThat(shareOf(shares, ID_C)).isEqualByComparingTo("50.00");
  }

  @Test
  void tieBreakOrdersByDueDateThenIdAndSendsResidualToLast() {
    List<Share> shares =
        InvoiceAllocationCalculator.allocate(
            new BigDecimal("0.01"),
            List.of(
                line(ID_C, "2026-08-10", "10.00"),
                line(ID_A, "2026-08-10", "10.00"),
                line(ID_B, "2026-08-11", "10.00")));

    assertExactSum(shares, "0.01");
    assertThat(shareOf(shares, ID_B)).isEqualByComparingTo("0.01");
  }

  @Test
  void residualGoesToLastInOrdering() {
    List<Share> shares =
        InvoiceAllocationCalculator.allocate(
            new BigDecimal("10.00"),
            List.of(
                line(ID_A, "2026-08-10", "3.00"),
                line(ID_B, "2026-08-10", "3.00"),
                line(ID_C, "2026-08-10", "4.00")));

    assertExactSum(shares, "10.00");
    assertThat(shareOf(shares, ID_A).add(shareOf(shares, ID_B)).add(shareOf(shares, ID_C)))
        .isEqualByComparingTo("10.00");
    assertThat(shareOf(shares, ID_A)).isLessThanOrEqualTo(new BigDecimal("3.00"));
    assertThat(shareOf(shares, ID_B)).isLessThanOrEqualTo(new BigDecimal("3.00"));
    assertThat(shareOf(shares, ID_C)).isLessThanOrEqualTo(new BigDecimal("4.00"));
  }

  @Test
  void oddCentsDoNotUseDouble() {
    List<Share> shares =
        InvoiceAllocationCalculator.allocate(
            new BigDecimal("100.00"),
            List.of(
                line(ID_A, "2026-08-10", "33.33"),
                line(ID_B, "2026-08-11", "33.33"),
                line(ID_C, "2026-08-12", "33.34")));

    assertExactSum(shares, "100.00");
    shares.forEach(share -> assertThat(share.amount().scale()).isEqualTo(2));
    assertThat(shareOf(shares, ID_A)).isLessThanOrEqualTo(new BigDecimal("33.33"));
    assertThat(shareOf(shares, ID_B)).isLessThanOrEqualTo(new BigDecimal("33.33"));
    assertThat(shareOf(shares, ID_C)).isLessThanOrEqualTo(new BigDecimal("33.34"));
  }

  @Test
  void oneCentSplitAcrossThreeRemainings() {
    List<Share> shares =
        InvoiceAllocationCalculator.allocate(
            new BigDecimal("0.01"),
            List.of(
                line(ID_A, "2026-08-10", "10.00"),
                line(ID_B, "2026-08-10", "20.00"),
                line(ID_C, "2026-08-10", "30.00")));

    assertExactSum(shares, "0.01");
    assertThat(shareOf(shares, ID_C)).isEqualByComparingTo("0.01");
  }

  @Test
  void doesNotAllocateMoreThanRemaining() {
    List<Share> shares =
        InvoiceAllocationCalculator.allocate(
            new BigDecimal("80.00"),
            List.of(line(ID_A, "2026-08-10", "50.00"), line(ID_B, "2026-08-11", "20.00")));

    assertExactSum(shares, "70.00");
    assertThat(shareOf(shares, ID_A)).isEqualByComparingTo("50.00");
    assertThat(shareOf(shares, ID_B)).isEqualByComparingTo("20.00");
  }

  @Test
  void ignoresZeroRemainingLines() {
    List<Share> shares =
        InvoiceAllocationCalculator.allocate(
            new BigDecimal("10.00"),
            List.of(line(ID_A, "2026-08-10", "0.00"), line(ID_B, "2026-08-11", "10.00")));

    assertThat(shares).hasSize(1);
    assertThat(shares.getFirst().installmentId()).isEqualTo(ID_B);
    assertThat(shares.getFirst().amount()).isEqualByComparingTo("10.00");
  }

  private static Line line(UUID id, String dueDate, String remaining) {
    return new Line(id, LocalDate.parse(dueDate), new BigDecimal(remaining));
  }

  private static BigDecimal shareOf(List<Share> shares, UUID id) {
    return shares.stream()
        .filter(share -> share.installmentId().equals(id))
        .map(Share::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static void assertExactSum(List<Share> shares, String expected) {
    BigDecimal sum = shares.stream().map(Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo(expected);
  }
}
