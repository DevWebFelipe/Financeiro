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
            "payments",
            "transfers",
            "credit_card_invoices",
            "credit_card_invoice_payments",
            "credit_card_invoice_installments",
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
  void shouldNotPersistDerivedGoalCurrentAmount() {
    assertThat(columnsOf("financial_goals")).doesNotContain("current_amount");
  }

  @Test
  void shouldKeepPaymentsTypeAsUnconstrainedVarchar() {
    assertThat(columnsOf("payments")).contains("type");
    Integer checkCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_constraint
            WHERE conrelid = 'payments'::regclass
              AND contype = 'c'
              AND pg_get_constraintdef(oid) ILIKE '%type%'
            """,
            Integer.class);
    assertThat(checkCount).isZero();
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
