package br.com.financialcontrol.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class V30IncomeMovementsBackfillTest {

  private static final UUID USER_ID = UUID.fromString("01800000-0000-7000-8000-00000000d830");
  private static final UUID ACCOUNT_ID = UUID.fromString("01800000-0000-7000-8000-00000000d831");
  private static final UUID CATEGORY_ID = UUID.fromString("01800000-0000-7000-8000-00000000d832");

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanup() {
    jdbcTemplate.update("DELETE FROM income_movements WHERE user_id = ?", USER_ID);
    jdbcTemplate.update("DELETE FROM incomes WHERE user_id = ?", USER_ID);
    jdbcTemplate.update("DELETE FROM categories WHERE id = ?", CATEGORY_ID);
    jdbcTemplate.update("DELETE FROM accounts WHERE id = ?", ACCOUNT_ID);
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
  }

  @Test
  void shouldBackfillReceivedIncomeIntoSingleActiveReceipt() {
    UUID incomeId = UUID.fromString("01800000-0000-7000-8000-00000000d833");
    insertUserCategoryAccount();
    insertIncome(incomeId, "RECEIVED", new BigDecimal("1250.50"), LocalDate.of(2026, 7, 20));

    runBackfillInsert();

    List<Map<String, Object>> movements = movementsFor(incomeId);
    assertThat(movements).hasSize(1);
    assertThat(movements.getFirst().get("type")).isEqualTo("RECEIPT");
    assertThat(movements.getFirst().get("status")).isEqualTo("ACTIVE");
    assertThat(movements.getFirst().get("amount")).isEqualTo(new BigDecimal("1250.50"));
    assertThat(movements.getFirst().get("account_id")).isEqualTo(ACCOUNT_ID);
    assertThat(((java.sql.Date) movements.getFirst().get("movement_date")).toLocalDate())
        .isEqualTo(LocalDate.of(2026, 7, 20));
  }

  @Test
  void shouldNotDuplicateReceiptOnSecondBackfillExecution() {
    UUID incomeId = UUID.fromString("01800000-0000-7000-8000-00000000d834");
    insertUserCategoryAccount();
    insertIncome(incomeId, "RECEIVED", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1));

    runBackfillInsert();
    runBackfillInsert();

    assertThat(movementsFor(incomeId)).hasSize(1);
  }

  @Test
  void shouldNotBackfillExpectedOrCancelledIncomes() {
    UUID expectedId = UUID.fromString("01800000-0000-7000-8000-00000000d835");
    UUID cancelledId = UUID.fromString("01800000-0000-7000-8000-00000000d836");
    insertUserCategoryAccount();
    insertIncome(expectedId, "EXPECTED", new BigDecimal("80.00"), null);
    insertIncome(cancelledId, "CANCELLED", new BigDecimal("90.00"), null);

    runBackfillInsert();

    assertThat(movementsFor(expectedId)).isEmpty();
    assertThat(movementsFor(cancelledId)).isEmpty();
  }

  @Test
  void shouldAbortBackfillWhenReceivedIncomeMissingAccountOrDate() {
    UUID invalidAccountId = UUID.fromString("01800000-0000-7000-8000-00000000d837");
    UUID invalidDateId = UUID.fromString("01800000-0000-7000-8000-00000000d838");
    insertUserCategoryAccount();
    try {
      jdbcTemplate.update(
          """
          INSERT INTO incomes (
            id, user_id, category_id, account_id, description, amount, expected_date,
            received_date, status, created_at, updated_at)
          VALUES (?, ?, ?, NULL, 'Sem conta', 10.00, '2026-08-01', '2026-08-02', 'RECEIVED', NOW(), NOW())
          """,
          invalidAccountId,
          USER_ID,
          CATEGORY_ID);
      jdbcTemplate.update(
          """
          INSERT INTO incomes (
            id, user_id, category_id, account_id, description, amount, expected_date,
            received_date, status, created_at, updated_at)
          VALUES (?, ?, ?, ?, 'Sem data', 10.00, '2026-08-01', NULL, 'RECEIVED', NOW(), NOW())
          """,
          invalidDateId,
          USER_ID,
          CATEGORY_ID,
          ACCOUNT_ID);

      assertThatThrownBy(this::runPreBackfillValidation)
          .hasMessageContaining("D83 backfill aborted");
    } finally {
      jdbcTemplate.update(
          "DELETE FROM incomes WHERE id IN (?, ?)", invalidAccountId, invalidDateId);
    }
  }

  private void insertUserCategoryAccount() {
    jdbcTemplate.update(
        """
        INSERT INTO users (id, name, email, password_hash, active, created_at, updated_at)
        VALUES (?, 'D83', 'd83-backfill@example.com', 'x', true, NOW(), NOW())
        ON CONFLICT (id) DO NOTHING
        """,
        USER_ID);
    jdbcTemplate.update(
        """
        INSERT INTO accounts (
          id, user_id, name, type, initial_balance, initial_balance_locked, active, created_at, updated_at)
        VALUES (?, ?, 'Conta', 'BANK_ACCOUNT', 0, false, true, NOW(), NOW())
        ON CONFLICT (id) DO NOTHING
        """,
        ACCOUNT_ID,
        USER_ID);
    jdbcTemplate.update(
        """
        INSERT INTO categories (id, user_id, name, type, active, created_at, updated_at)
        VALUES (?, ?, 'Salário', 'INCOME', true, NOW(), NOW())
        ON CONFLICT (id) DO NOTHING
        """,
        CATEGORY_ID,
        USER_ID);
  }

  private void insertIncome(
      UUID incomeId, String status, BigDecimal amount, LocalDate receivedDate) {
    jdbcTemplate.update(
        """
        INSERT INTO incomes (
          id, user_id, category_id, account_id, description, amount, expected_date,
          received_date, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, 'Backfill', ?, '2026-08-01', ?, ?, NOW(), NOW())
        ON CONFLICT (id) DO NOTHING
        """,
        incomeId,
        USER_ID,
        CATEGORY_ID,
        "RECEIVED".equals(status) ? ACCOUNT_ID : null,
        amount,
        receivedDate,
        status);
  }

  private void runPreBackfillValidation() {
    jdbcTemplate.execute(
        """
        DO $$
        BEGIN
            IF EXISTS (
                SELECT 1
                FROM incomes
                WHERE status = 'RECEIVED'
                  AND (account_id IS NULL OR received_date IS NULL)
            ) THEN
                RAISE EXCEPTION
                    'D83 backfill aborted: RECEIVED income is missing account_id or received_date';
            END IF;
        END
        $$;
        """);
  }

  private void runBackfillInsert() {
    runPreBackfillValidation();
    jdbcTemplate.update(
        """
        INSERT INTO income_movements (
            id,
            user_id,
            income_id,
            type,
            status,
            amount,
            movement_date,
            account_id,
            created_at,
            updated_at,
            reversed_at
        )
        SELECT
            uuidv7(),
            i.user_id,
            i.id,
            'RECEIPT',
            'ACTIVE',
            i.amount,
            i.received_date,
            i.account_id,
            NOW(),
            NOW(),
            NULL
        FROM incomes i
        WHERE i.status = 'RECEIVED'
          AND NOT EXISTS (
              SELECT 1
              FROM income_movements m
              WHERE m.income_id = i.id
                AND m.type = 'RECEIPT'
          )
        """);
  }

  private List<Map<String, Object>> movementsFor(UUID incomeId) {
    return jdbcTemplate.queryForList(
        """
        SELECT type, status, amount, account_id, movement_date
        FROM income_movements
        WHERE income_id = ?
        ORDER BY created_at
        """,
        incomeId);
  }
}
