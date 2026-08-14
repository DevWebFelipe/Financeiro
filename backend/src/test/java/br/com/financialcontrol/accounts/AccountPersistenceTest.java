package br.com.financialcontrol.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class AccountPersistenceTest {

  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

  @Autowired private AccountRepository accountRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void shouldPersistAccountWithUuidV7OwnershipAndInitialBalance() {
    User owner = persistUser("acc-owner@example.com");
    Account account = new Account();
    account.setId(UuidV7.create());
    account.setUserId(owner.getId());
    account.setName("Nubank");
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal("1500.00"));
    account.setActive(true);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);

    Account saved = accountRepository.saveAndFlush(account);

    Account loaded = accountRepository.findById(saved.getId()).orElseThrow();
    assertThat(loaded.getId().version()).isEqualTo(7);
    assertThat(loaded.getUserId()).isEqualTo(owner.getId());
    assertThat(loaded.getName()).isEqualTo("Nubank");
    assertThat(loaded.getType()).isEqualTo(AccountType.BANK_ACCOUNT);
    assertThat(loaded.getInitialBalance()).isEqualByComparingTo("1500.00");
    assertThat(loaded.isActive()).isTrue();
    assertThat(accountRepository.findByIdAndUserId(saved.getId(), owner.getId())).isPresent();
  }

  @Test
  void shouldPersistCashAccount() {
    User owner = persistUser("cash-owner@example.com");
    Account account = persistAccount(owner.getId(), "Carteira", AccountType.CASH, "0.00", true);

    Account loaded = accountRepository.findById(account.getId()).orElseThrow();
    assertThat(loaded.getType()).isEqualTo(AccountType.CASH);
    assertThat(loaded.getInitialBalance()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldUpdateAccountAndKeepOwnerAndInitialBalance() {
    User owner = persistUser("upd-owner@example.com");
    Account account =
        persistAccount(owner.getId(), "Nubank", AccountType.BANK_ACCOUNT, "1500.00", true);

    account.setName("Nubank PJ");
    account.setType(AccountType.CASH);
    account.setUpdatedAt(NOW.plusSeconds(60));
    accountRepository.saveAndFlush(account);

    Account loaded = accountRepository.findById(account.getId()).orElseThrow();
    assertThat(loaded.getName()).isEqualTo("Nubank PJ");
    assertThat(loaded.getType()).isEqualTo(AccountType.CASH);
    assertThat(loaded.getUserId()).isEqualTo(owner.getId());
    assertThat(loaded.getInitialBalance()).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldKeepDeactivatedAccountPersisted() {
    User owner = persistUser("deact-owner@example.com");
    Account account =
        persistAccount(owner.getId(), "Nubank", AccountType.BANK_ACCOUNT, "1500.00", true);

    account.setActive(false);
    accountRepository.saveAndFlush(account);

    Account loaded = accountRepository.findById(account.getId()).orElseThrow();
    assertThat(loaded).isNotNull();
    assertThat(loaded.isActive()).isFalse();
    assertThat(loaded.getName()).isEqualTo("Nubank");
    assertThat(loaded.getInitialBalance()).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldNotReturnAccountOfAnotherUserByOwnershipQuery() {
    User owner = persistUser("iso-a@example.com");
    User other = persistUser("iso-b@example.com");
    Account account =
        persistAccount(owner.getId(), "Nubank", AccountType.BANK_ACCOUNT, "1500.00", true);

    assertThat(accountRepository.findByIdAndUserId(account.getId(), other.getId())).isEmpty();
    assertThat(accountRepository.findAllByUserIdOrderByCreatedAtAsc(other.getId())).isEmpty();
    assertThat(accountRepository.findAllByUserIdOrderByCreatedAtAsc(owner.getId()))
        .extracting(Account::getId)
        .containsExactly(account.getId());
  }

  @Test
  void shouldRejectAccountWithoutExistingUser() {
    Account account = new Account();
    account.setId(UuidV7.create());
    account.setUserId(UuidV7.create());
    account.setName("Orfã");
    account.setType(AccountType.BANK_ACCOUNT);
    account.setInitialBalance(new BigDecimal("10.00"));
    account.setActive(true);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);

    assertThatThrownBy(() -> accountRepository.saveAndFlush(account))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRejectInvalidAccountTypeAtDatabase() {
    User owner = persistUser("type-check@example.com");
    UUID id = UuidV7.create();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO accounts
                      (id, user_id, name, type, initial_balance, active, created_at, updated_at)
                    VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz))
                    """,
                    id.toString(),
                    owner.getId().toString(),
                    "Inválida",
                    "SAVINGS",
                    new BigDecimal("10.00"),
                    true,
                    NOW.toString(),
                    NOW.toString()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private User persistUser(String email) {
    User user = new User();
    user.setId(UuidV7.create());
    user.setName("User");
    user.setEmail(email);
    user.setPasswordHash("not-a-real-hash");
    user.setActive(true);
    user.setCreatedAt(NOW);
    user.setUpdatedAt(NOW);
    return userRepository.saveAndFlush(user);
  }

  private Account persistAccount(
      UUID userId, String name, AccountType type, String initialBalance, boolean active) {
    Account account = new Account();
    account.setId(UuidV7.create());
    account.setUserId(userId);
    account.setName(name);
    account.setType(type);
    account.setInitialBalance(new BigDecimal(initialBalance));
    account.setActive(active);
    account.setCreatedAt(NOW);
    account.setUpdatedAt(NOW);
    return accountRepository.saveAndFlush(account);
  }
}
