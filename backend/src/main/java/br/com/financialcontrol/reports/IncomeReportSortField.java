package br.com.financialcontrol.reports;

enum IncomeReportSortField {
  EXPECTED_DATE("expectedDate"),
  DESCRIPTION("description"),
  AMOUNT("amount"),
  STATUS("status"),
  CREATED_AT("createdAt"),
  RECEIVED_AMOUNT("receivedAmount");

  private final String queryName;

  IncomeReportSortField(String queryName) {
    this.queryName = queryName;
  }

  static IncomeReportSortField fromQuery(String value) {
    for (IncomeReportSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
