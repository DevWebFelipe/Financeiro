package br.com.financialcontrol.payables;

enum PayableSortField {
  NAME("name"),
  PURCHASE_DATE("purchaseDate"),
  DUE_DATE("dueDate"),
  ORIGINAL_AMOUNT("originalAmount"),
  REMAINING_AMOUNT("remainingAmount"),
  STATUS("status"),
  PAID_AMOUNT("paidAmount");

  private final String queryName;

  PayableSortField(String queryName) {
    this.queryName = queryName;
  }

  String queryName() {
    return queryName;
  }

  static PayableSortField fromQuery(String value) {
    for (PayableSortField field : values()) {
      if (field.queryName.equals(value)) {
        return field;
      }
    }
    return null;
  }
}
