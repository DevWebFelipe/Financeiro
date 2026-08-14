package br.com.financialcontrol.categories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
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
class CategoryPersistenceTest {

  private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");

  @Autowired private CategoryRepository categoryRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void shouldPersistCategoryWithUuidV7AndOwnership() {
    User owner = persistUser("cat-owner@example.com");
    Category category = persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true);

    Category loaded = categoryRepository.findById(category.getId()).orElseThrow();
    assertThat(loaded.getId().version()).isEqualTo(7);
    assertThat(loaded.getUserId()).isEqualTo(owner.getId());
    assertThat(loaded.getName()).isEqualTo("Mercado");
    assertThat(loaded.getType()).isEqualTo(CategoryType.EXPENSE);
    assertThat(loaded.isActive()).isTrue();
    assertThat(categoryRepository.findByIdAndUserId(category.getId(), owner.getId())).isPresent();
  }

  @Test
  void shouldRejectDuplicateNameAndTypeForSameUser() {
    User owner = persistUser("dup-owner@example.com");
    persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true);

    assertThatThrownBy(() -> persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldAllowSameNameForDifferentTypes() {
    User owner = persistUser("types-owner@example.com");
    persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true);
    Category income = persistCategory(owner.getId(), "Mercado", CategoryType.INCOME, true);

    assertThat(categoryRepository.findById(income.getId())).isPresent();
  }

  @Test
  void shouldRejectDuplicateUppercaseName() {
    User owner = persistUser("case-upper@example.com");
    persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true);

    assertThatThrownBy(() -> persistCategory(owner.getId(), "MERCADO", CategoryType.EXPENSE, true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRejectDuplicateLowercaseName() {
    User owner = persistUser("case-lower@example.com");
    persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true);

    assertThatThrownBy(() -> persistCategory(owner.getId(), "mercado", CategoryType.EXPENSE, true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRejectDuplicateEvenWhenExistingCategoryIsInactive() {
    User owner = persistUser("inactive-dup@example.com");
    persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, false);

    assertThatThrownBy(() -> persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldKeepDeactivatedCategoryPersisted() {
    User owner = persistUser("deact-owner@example.com");
    Category category = persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true);

    category.setActive(false);
    categoryRepository.saveAndFlush(category);

    Category loaded = categoryRepository.findById(category.getId()).orElseThrow();
    assertThat(loaded).isNotNull();
    assertThat(loaded.isActive()).isFalse();
    assertThat(loaded.getName()).isEqualTo("Mercado");
  }

  @Test
  void shouldNotReturnCategoryOfAnotherUserByOwnershipQuery() {
    User owner = persistUser("iso-a@example.com");
    User other = persistUser("iso-b@example.com");
    Category category = persistCategory(owner.getId(), "Mercado", CategoryType.EXPENSE, true);

    assertThat(categoryRepository.findByIdAndUserId(category.getId(), other.getId())).isEmpty();
    assertThat(categoryRepository.findAllByUserIdOrderByCreatedAtAsc(other.getId())).isEmpty();
    assertThat(categoryRepository.findAllByUserIdOrderByCreatedAtAsc(owner.getId()))
        .extracting(Category::getId)
        .containsExactly(category.getId());
  }

  @Test
  void shouldRejectCategoryWithoutExistingUser() {
    Category category = new Category();
    category.setId(UuidV7.create());
    category.setUserId(UuidV7.create());
    category.setName("Órfã");
    category.setType(CategoryType.EXPENSE);
    category.setActive(true);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);

    assertThatThrownBy(() -> categoryRepository.saveAndFlush(category))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRejectInvalidCategoryTypeAtDatabase() {
    User owner = persistUser("type-check@example.com");
    UUID id = UuidV7.create();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO categories
                      (id, user_id, name, type, active, created_at, updated_at)
                    VALUES (?::uuid, ?::uuid, ?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz))
                    """,
                    id.toString(),
                    owner.getId().toString(),
                    "Inválida",
                    "TRANSFER",
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

  private Category persistCategory(UUID userId, String name, CategoryType type, boolean active) {
    Category category = new Category();
    category.setId(UuidV7.create());
    category.setUserId(userId);
    category.setName(name);
    category.setType(type);
    category.setActive(active);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);
    return categoryRepository.saveAndFlush(category);
  }
}
