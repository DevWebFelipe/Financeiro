package br.com.financialcontrol.reports;

enum CashFlowSortField {
  DATE("date"),
  AMOUNT("amount"),
  TYPE("type");

  private final String queryName;

  CashFlowSortField(String queryName) {
    this.queryName = queryName;
  }

  static CashFlowSortField fromQuery(String value) {
    for (CashFlowSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
