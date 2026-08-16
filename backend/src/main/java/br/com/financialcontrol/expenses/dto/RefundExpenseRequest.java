package br.com.financialcontrol.expenses.dto;

public record RefundExpenseRequest(RefundSettlement settlement, java.util.UUID accountId) {

  public enum RefundSettlement {
    CARD_CREDIT,
    ACCOUNT
  }
}
