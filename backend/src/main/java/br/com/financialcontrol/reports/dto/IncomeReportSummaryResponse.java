package br.com.financialcontrol.reports.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncomeReportSummaryResponse(
    BigDecimal amount,
    BigDecimal accruedAmount,
    BigDecimal receivedAmount,
    BigDecimal remainingAmount,
    BigDecimal periodReceivedAmount) {

  public static IncomeReportSummaryResponse expected(
      BigDecimal amount,
      BigDecimal accruedAmount,
      BigDecimal receivedAmount,
      BigDecimal remainingAmount) {
    return new IncomeReportSummaryResponse(
        amount, accruedAmount, receivedAmount, remainingAmount, null);
  }

  public static IncomeReportSummaryResponse received(BigDecimal periodReceivedAmount) {
    return new IncomeReportSummaryResponse(null, null, null, null, periodReceivedAmount);
  }
}
