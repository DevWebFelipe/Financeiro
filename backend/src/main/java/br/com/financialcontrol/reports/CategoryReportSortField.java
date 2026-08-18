package br.com.financialcontrol.reports;

enum CategoryReportSortField {
  NAME("name"),
  TYPE("type");

  private final String queryName;

  CategoryReportSortField(String queryName) {
    this.queryName = queryName;
  }

  static CategoryReportSortField fromQuery(String value) {
    for (CategoryReportSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
