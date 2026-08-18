package br.com.financialcontrol.reports;

enum CardReportSortField {
  NAME("name"),
  HOLDER_NAME("holderName");

  private final String queryName;

  CardReportSortField(String queryName) {
    this.queryName = queryName;
  }

  static CardReportSortField fromQuery(String value) {
    for (CardReportSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
