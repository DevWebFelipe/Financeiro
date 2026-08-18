package br.com.financialcontrol.reports;

enum ResponsibleReportSortField {
  RESPONSIBLE_TYPE("responsibleType"),
  RESPONSIBLE_NAME("responsibleName");

  private final String queryName;

  ResponsibleReportSortField(String queryName) {
    this.queryName = queryName;
  }

  static ResponsibleReportSortField fromQuery(String value) {
    for (ResponsibleReportSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
