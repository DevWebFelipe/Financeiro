package br.com.financialcontrol.schema;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class SchemaContractTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void shouldCreateOfficialTables() {
    List<String> tables =
        jdbcTemplate.queryForList(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
            """,
            String.class);

    assertThat(tables)
        .contains(
            "users",
            "accounts",
            "categories",
            "credit_cards",
            "incomes",
            "expenses",
            "expense_installments",
            "expense_installment_adjustments",
            "payments",
            "transfers",
            "credit_card_invoices",
            "credit_card_invoice_payments",
            "credit_card_invoice_installments",
            "credit_card_invoice_payment_allocations",
            "credit_card_credits",
            "credit_card_credit_applications",
            "credit_card_invoice_adjustments",
            "credit_card_invoice_adjustment_allocations",
            "credit_card_invoice_agreements",
            "credit_card_invoice_agreement_settlements",
            "credit_card_invoice_agreement_settlement_allocations",
            "card_purchase_account_refunds",
            "financial_goals",
            "goal_contributions");
  }

  @Test
  void shouldNotStoreInvoiceIdOnExpenses() {
    assertThat(columnsOf("expenses")).doesNotContain("invoice_id");
  }

  @Test
  void shouldStoreInvoiceIdOnExpenseInstallments() {
    assertThat(columnsOf("expense_installments")).contains("invoice_id", "user_id", "expense_id");
  }

  @Test
  void shouldNotPersistDerivedInvoiceAmounts() {
    assertThat(columnsOf("credit_card_invoices"))
        .doesNotContain("total_amount", "paid_amount", "remaining_amount");
  }

  @Test
  void shouldNotPersistPaidAmountOnInstallments() {
    assertThat(columnsOf("expense_installments")).doesNotContain("paid_amount", "remaining_amount");
  }

  @Test
  void shouldAllowNullableLastFourDigitsOnCreditCards() {
    String nullable =
        jdbcTemplate.queryForObject(
            """
            SELECT is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'credit_cards'
              AND column_name = 'last_four_digits'
            """,
            String.class);
    assertThat(nullable).isEqualTo("YES");
  }

  @Test
  void shouldUsePhase9AndPhase13InvoiceStatuses() {
    String check =
        jdbcTemplate.queryForObject(
            """
            SELECT pg_get_constraintdef(oid)
            FROM pg_constraint
            WHERE conrelid = 'credit_card_invoices'::regclass
              AND contype = 'c'
              AND conname = 'ck_credit_card_invoices_status'
            """,
            String.class);
    assertThat(check).contains("SCHEDULED");
    assertThat(check).contains("OPEN");
    assertThat(check).contains("CLOSED");
    assertThat(check).contains("PAID");
    assertThat(check).contains("SETTLED_BY_AGREEMENT");
    assertThat(check).doesNotContain("PARTIALLY_PAID");
  }

  @Test
  void shouldNotPersistDerivedGoalCurrentAmount() {
    assertThat(columnsOf("financial_goals")).doesNotContain("current_amount");
  }

  @Test
  void shouldKeepPaymentsTypeAsUnconstrainedVarchar() {
    assertThat(columnsOf("payments")).contains("type", "status");
    Integer checkCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_constraint
            WHERE conrelid = 'payments'::regclass
              AND contype = 'c'
              AND pg_get_constraintdef(oid) ILIKE '%type%'
              AND conname <> 'ck_payments_status'
            """,
            Integer.class);
    assertThat(checkCount).isZero();
  }

  @Test
  void shouldEnforceUniqueInstallmentNumberPerExpense() {
    assertThat(constraintNames("expense_installments"))
        .contains("uq_expense_installments_expense_number");
  }

  @Test
  void shouldEnforceEmailUniqueness() {
    Integer uniqueCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_constraint
            WHERE conrelid = 'users'::regclass
              AND contype = 'u'
              AND conname = 'uq_users_email'
            """,
            Integer.class);
    assertThat(uniqueCount).isEqualTo(1);
  }

  @Test
  void shouldDefineOwnershipUniqueTargets() {
    assertThat(constraintNames("accounts")).contains("uq_accounts_id_user");
    assertThat(constraintNames("categories")).contains("uq_categories_id_user");
    assertThat(constraintNames("credit_cards")).contains("uq_credit_cards_id_user");
    assertThat(constraintNames("expenses")).contains("uq_expenses_id_user");
    assertThat(constraintNames("credit_card_invoices")).contains("uq_credit_card_invoices_id_user");
  }

  @Test
  void shouldAllowNullIncomeResponsibleTypeWithoutDroppingTheColumn() {
    assertThat(columnsOf("incomes")).contains("responsible_type", "responsible_name");
    String nullable =
        jdbcTemplate.queryForObject(
            """
            SELECT is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'incomes'
              AND column_name = 'responsible_type'
            """,
            String.class);
    assertThat(nullable).isEqualTo("YES");
  }

  @Test
  void shouldEnforceCaseInsensitiveCategoryNameUniquenessPerUserAndType() {
    List<String> indexes =
        jdbcTemplate.queryForList(
            """
            SELECT indexname
            FROM pg_indexes
            WHERE schemaname = 'public'
              AND tablename = 'categories'
            """,
            String.class);
    assertThat(indexes).contains("uq_categories_user_type_lower_name");
    assertThat(constraintNames("categories")).doesNotContain("uq_categories_user_name_type");
  }

  private List<String> columnsOf(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = ?
        """,
        String.class,
        table);
  }

  private List<String> constraintNames(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = ?::regclass
        """,
        String.class,
        table);
  }
}
