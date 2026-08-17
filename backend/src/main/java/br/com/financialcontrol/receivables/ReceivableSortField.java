package br.com.financialcontrol.receivables;

enum ReceivableSortField {
  AMOUNT("amount", "amount"),
  EXPECTED_DATE("expectedDate", "expectedDate"),
  RECEIVED_DATE("receivedDate", "receivedDate"),
  DESCRIPTION("description", "description"),
  STATUS("status", "status"),
  CREATED_AT("createdAt", "createdAt");

  private final String queryName;
  private final String property;

  ReceivableSortField(String queryName, String property) {
    this.queryName = queryName;
    this.property = property;
  }

  String property() {
    return property;
  }

  static ReceivableSortField fromQuery(String value) {
    for (ReceivableSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
