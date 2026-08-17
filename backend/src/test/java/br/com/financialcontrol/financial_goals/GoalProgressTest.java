package br.com.financialcontrol.financial_goals;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GoalProgressTest {

  @Test
  void shouldCalculateProgressPercentWithHalfUpScaleTwoAndNoCap() {
    assertThat(FinancialGoal.progressPercent(new BigDecimal("2500.00"), new BigDecimal("5000.00")))
        .isEqualByComparingTo("50.00");
    assertThat(FinancialGoal.progressPercent(new BigDecimal("5000.00"), new BigDecimal("5000.00")))
        .isEqualByComparingTo("100.00");
    assertThat(FinancialGoal.progressPercent(new BigDecimal("5500.00"), new BigDecimal("5000.00")))
        .isEqualByComparingTo("110.00");
    assertThat(FinancialGoal.progressPercent(new BigDecimal("7500.00"), new BigDecimal("5000.00")))
        .isEqualByComparingTo("150.00");
    assertThat(FinancialGoal.progressPercent(BigDecimal.ZERO, new BigDecimal("5000.00")))
        .isEqualByComparingTo("0.00");
  }
}
