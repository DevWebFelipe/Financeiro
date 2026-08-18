package br.com.financialcontrol.reports;

enum ExpenseReportSortField {
  DUE_DATE("dueDate"),
  EXPENSE_DATE("expenseDate"),
  DESCRIPTION("description"),
  STATUS("status"),
  CREATED_AT("createdAt"),
  PERIOD_OBLIGATION("periodObligation"),
  PERIOD_REMAINING("periodRemaining");

  private final String queryName;

  ExpenseReportSortField(String queryName) {
    this.queryName = queryName;
  }

  static ExpenseReportSortField fromQuery(String value) {
    for (ExpenseReportSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
