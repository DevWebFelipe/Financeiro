package br.com.financialcontrol.categories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.categories.dto.CreateCategoryRequest;
import br.com.financialcontrol.categories.dto.UpdateCategoryRequest;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.ConflictException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
  private static final UUID USER_A = UUID.fromString("01800000-0000-7000-8000-00000000000a");
  private static final UUID USER_B = UUID.fromString("01800000-0000-7000-8000-00000000000b");
  private static final UUID CATEGORY_ID = UUID.fromString("01800000-0000-7000-8000-0000000000ca");

  @Mock private CategoryRepository categoryRepository;

  private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    categoryService = new CategoryService(categoryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldCreateExpenseCategoryOwnedByAuthenticatedUser() {
    when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(
            USER_A, CategoryType.EXPENSE, "Mercado"))
        .thenReturn(false);
    when(categoryRepository.saveAndFlush(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        categoryService.create(
            new AuthenticatedUser(USER_A),
            new CreateCategoryRequest("Mercado", CategoryType.EXPENSE));

    ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
    verify(categoryRepository).saveAndFlush(captor.capture());
    Category saved = captor.getValue();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getId().version()).isEqualTo(7);
    assertThat(saved.getUserId()).isEqualTo(USER_A).isNotEqualTo(USER_B);
    assertThat(saved.getName()).isEqualTo("Mercado");
    assertThat(saved.getType()).isEqualTo(CategoryType.EXPENSE);
    assertThat(saved.isActive()).isTrue();
    assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
    assertThat(response.id()).isEqualTo(saved.getId());
    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldCreateIncomeCategory() {
    when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(
            USER_A, CategoryType.INCOME, "Salário"))
        .thenReturn(false);
    when(categoryRepository.saveAndFlush(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        categoryService.create(
            new AuthenticatedUser(USER_A),
            new CreateCategoryRequest("Salário", CategoryType.INCOME));

    assertThat(response.type()).isEqualTo(CategoryType.INCOME);
    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldRejectDuplicateNameAndTypeIgnoringCase() {
    when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(
            USER_A, CategoryType.EXPENSE, "Mercado"))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                categoryService.create(
                    new AuthenticatedUser(USER_A),
                    new CreateCategoryRequest("Mercado", CategoryType.EXPENSE)))
        .isInstanceOf(ConflictException.class)
        .hasMessage(CategoryService.CATEGORY_NAME_TYPE_CONFLICT);
    verify(categoryRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldMapPersistenceUniqueViolationToConflict() {
    when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(
            USER_A, CategoryType.EXPENSE, "Mercado"))
        .thenReturn(false);
    when(categoryRepository.saveAndFlush(any(Category.class)))
        .thenThrow(new DataIntegrityViolationException("unique"));

    assertThatThrownBy(
            () ->
                categoryService.create(
                    new AuthenticatedUser(USER_A),
                    new CreateCategoryRequest("Mercado", CategoryType.EXPENSE)))
        .isInstanceOf(ConflictException.class)
        .hasMessage(CategoryService.CATEGORY_NAME_TYPE_CONFLICT);
  }

  @Test
  void shouldAllowSameNameForDifferentTypes() {
    when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(
            USER_A, CategoryType.INCOME, "Mercado"))
        .thenReturn(false);
    when(categoryRepository.saveAndFlush(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        categoryService.create(
            new AuthenticatedUser(USER_A),
            new CreateCategoryRequest("Mercado", CategoryType.INCOME));

    assertThat(response.type()).isEqualTo(CategoryType.INCOME);
    assertThat(response.name()).isEqualTo("Mercado");
  }

  @Test
  void shouldListOnlyCategoriesOfAuthenticatedUser() {
    when(categoryRepository.findAllByUserIdOrderByCreatedAtAsc(USER_A))
        .thenReturn(List.of(ownedCategory(true, CategoryType.EXPENSE, "Mercado")));

    List<CategoryResponse> result = categoryService.list(new AuthenticatedUser(USER_A), null, null);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(CATEGORY_ID);
    verify(categoryRepository, never()).findAll();
  }

  @Test
  void shouldFilterListByTypeAndActive() {
    when(categoryRepository.findAllByUserIdAndTypeAndActiveOrderByCreatedAtAsc(
            USER_A, CategoryType.EXPENSE, true))
        .thenReturn(List.of(ownedCategory(true, CategoryType.EXPENSE, "Mercado")));

    List<CategoryResponse> result =
        categoryService.list(new AuthenticatedUser(USER_A), CategoryType.EXPENSE, true);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().type()).isEqualTo(CategoryType.EXPENSE);
    assertThat(result.getFirst().active()).isTrue();
  }

  @Test
  void shouldUpdateNameAndTypeWithoutChangingOwnerOrActive() {
    Category category = ownedCategory(true, CategoryType.EXPENSE, "Mercado");
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
        .thenReturn(Optional.of(category));
    when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(
            USER_A, CategoryType.INCOME, "Salário", CATEGORY_ID))
        .thenReturn(false);
    when(categoryRepository.saveAndFlush(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        categoryService.update(
            new AuthenticatedUser(USER_A),
            CATEGORY_ID,
            new UpdateCategoryRequest("Salário", CategoryType.INCOME));

    assertThat(response.name()).isEqualTo("Salário");
    assertThat(response.type()).isEqualTo(CategoryType.INCOME);
    assertThat(category.getUserId()).isEqualTo(USER_A);
    assertThat(category.isActive()).isTrue();
    assertThat(category.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void shouldRejectUpdateThatWouldDuplicateNameAndType() {
    Category category = ownedCategory(true, CategoryType.EXPENSE, "Mercado");
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
        .thenReturn(Optional.of(category));
    when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(
            USER_A, CategoryType.EXPENSE, "Moradia", CATEGORY_ID))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                categoryService.update(
                    new AuthenticatedUser(USER_A),
                    CATEGORY_ID,
                    new UpdateCategoryRequest("Moradia", CategoryType.EXPENSE)))
        .isInstanceOf(ConflictException.class)
        .hasMessage(CategoryService.CATEGORY_NAME_TYPE_CONFLICT);
  }

  @Test
  void shouldDeactivateCategoryWithoutRemovingIt() {
    Category category = ownedCategory(true, CategoryType.EXPENSE, "Mercado");
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
        .thenReturn(Optional.of(category));
    when(categoryRepository.save(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        categoryService.deactivate(new AuthenticatedUser(USER_A), CATEGORY_ID);

    assertThat(response.active()).isFalse();
    assertThat(category.isActive()).isFalse();
    verify(categoryRepository, never()).delete(any());
    verify(categoryRepository, never()).deleteById(any());
  }

  @Test
  void shouldDeactivateAlreadyInactiveCategoryIdempotently() {
    Category category = ownedCategory(false, CategoryType.EXPENSE, "Mercado");
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
        .thenReturn(Optional.of(category));
    when(categoryRepository.save(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        categoryService.deactivate(new AuthenticatedUser(USER_A), CATEGORY_ID);

    assertThat(response.active()).isFalse();
    assertThat(category.isActive()).isFalse();
  }

  @Test
  void shouldRejectAccessToCategoryOfAnotherUser() {
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_B)).thenReturn(Optional.empty());

    AuthenticatedUser userB = new AuthenticatedUser(USER_B);
    assertThatThrownBy(
            () ->
                categoryService.update(
                    userB, CATEGORY_ID, new UpdateCategoryRequest("X", CategoryType.EXPENSE)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(CategoryService.CATEGORY_NOT_FOUND);
  }

  @Test
  void shouldAcceptActiveOwnedIncomeCategoryForNewLaunch() {
    Category category = ownedCategory(true, CategoryType.INCOME, "Salário");
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
        .thenReturn(Optional.of(category));

    Category result = categoryService.requireActiveOwnedIncomeCategory(USER_A, CATEGORY_ID);

    assertThat(result.getId()).isEqualTo(CATEGORY_ID);
    assertThat(result.getType()).isEqualTo(CategoryType.INCOME);
  }

  @Test
  void shouldRejectInactiveCategoryForNewLaunch() {
    Category category = ownedCategory(false, CategoryType.INCOME, "Salário");
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
        .thenReturn(Optional.of(category));

    assertThatThrownBy(() -> categoryService.requireActiveOwnedIncomeCategory(USER_A, CATEGORY_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(CategoryService.CATEGORY_INACTIVE);
  }

  @Test
  void shouldRejectExpenseCategoryForIncomeLaunch() {
    Category category = ownedCategory(true, CategoryType.EXPENSE, "Mercado");
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
        .thenReturn(Optional.of(category));

    assertThatThrownBy(() -> categoryService.requireActiveOwnedIncomeCategory(USER_A, CATEGORY_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(CategoryService.CATEGORY_NOT_INCOME);
  }

  @Test
  void shouldRejectIncomeCategoryOfAnotherUser() {
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_B)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> categoryService.requireActiveOwnedIncomeCategory(USER_B, CATEGORY_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(CategoryService.CATEGORY_NOT_FOUND);
  }

  @Test
  void shouldRejectDeactivateOfAnotherUser() {
    when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_B)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> categoryService.deactivate(new AuthenticatedUser(USER_B), CATEGORY_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(CategoryService.CATEGORY_NOT_FOUND);
  }

  private static Category ownedCategory(boolean active, CategoryType type, String name) {
    Category category = new Category();
    category.setId(CATEGORY_ID);
    category.setUserId(USER_A);
    category.setName(name);
    category.setType(type);
    category.setActive(active);
    category.setCreatedAt(NOW);
    category.setUpdatedAt(NOW);
    return category;
  }
}
