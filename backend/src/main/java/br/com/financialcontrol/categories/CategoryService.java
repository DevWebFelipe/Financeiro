package br.com.financialcontrol.categories;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.categories.dto.CreateCategoryRequest;
import br.com.financialcontrol.categories.dto.UpdateCategoryRequest;
import br.com.financialcontrol.config.BusinessRuleException;
import br.com.financialcontrol.config.ConflictException;
import br.com.financialcontrol.config.NotFoundException;
import br.com.financialcontrol.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

  static final String CATEGORY_NOT_FOUND = "Categoria não encontrada.";
  static final String CATEGORY_NAME_TYPE_CONFLICT = "Já existe uma categoria com este nome e tipo.";
  static final String CATEGORY_INACTIVE =
      "Somente categorias ativas devem ser utilizadas em novos lançamentos.";
  static final String CATEGORY_NOT_INCOME = "A categoria deve ser do tipo receita.";
  static final String CATEGORY_NOT_EXPENSE = "A categoria deve ser do tipo despesa.";

  private final CategoryRepository categoryRepository;
  private final Clock clock;

  public CategoryService(CategoryRepository categoryRepository, Clock clock) {
    this.categoryRepository = categoryRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> list(
      AuthenticatedUser authenticatedUser, CategoryType type, Boolean active) {
    UUID userId = authenticatedUser.userId();
    List<Category> categories;
    if (type != null && active != null) {
      categories =
          categoryRepository.findAllByUserIdAndTypeAndActiveOrderByCreatedAtAsc(
              userId, type, active);
    } else if (type != null) {
      categories = categoryRepository.findAllByUserIdAndTypeOrderByCreatedAtAsc(userId, type);
    } else if (active != null) {
      categories = categoryRepository.findAllByUserIdAndActiveOrderByCreatedAtAsc(userId, active);
    } else {
      categories = categoryRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
    }
    return categories.stream().map(CategoryResponse::from).toList();
  }

  @Transactional
  public CategoryResponse create(
      AuthenticatedUser authenticatedUser, CreateCategoryRequest request) {
    UUID userId = authenticatedUser.userId();
    if (categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(
        userId, request.type(), request.name())) {
      throw new ConflictException(CATEGORY_NAME_TYPE_CONFLICT);
    }
    Instant now = Instant.now(clock);
    Category category = new Category();
    category.setId(UuidV7.create());
    category.setUserId(userId);
    category.setName(request.name());
    category.setType(request.type());
    category.setActive(true);
    category.setCreatedAt(now);
    category.setUpdatedAt(now);
    return CategoryResponse.from(saveUnique(category));
  }

  @Transactional
  public CategoryResponse update(
      AuthenticatedUser authenticatedUser, UUID categoryId, UpdateCategoryRequest request) {
    Category category = requireOwnedCategory(authenticatedUser.userId(), categoryId);
    if (categoryRepository.existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(
        authenticatedUser.userId(), request.type(), request.name(), categoryId)) {
      throw new ConflictException(CATEGORY_NAME_TYPE_CONFLICT);
    }
    category.setName(request.name());
    category.setType(request.type());
    category.setUpdatedAt(Instant.now(clock));
    return CategoryResponse.from(saveUnique(category));
  }

  @Transactional
  public CategoryResponse deactivate(AuthenticatedUser authenticatedUser, UUID categoryId) {
    Category category = requireOwnedCategory(authenticatedUser.userId(), categoryId);
    category.setActive(false);
    category.setUpdatedAt(Instant.now(clock));
    return CategoryResponse.from(categoryRepository.save(category));
  }

  Category requireOwnedCategory(UUID userId, UUID categoryId) {
    return categoryRepository
        .findByIdAndUserId(categoryId, userId)
        .orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND));
  }

  public Category requireActiveOwnedIncomeCategory(UUID userId, UUID categoryId) {
    Category category = requireOwnedCategory(userId, categoryId);
    if (!category.isActive()) {
      throw new BusinessRuleException(CATEGORY_INACTIVE);
    }
    if (category.getType() != CategoryType.INCOME) {
      throw new BusinessRuleException(CATEGORY_NOT_INCOME);
    }
    return category;
  }

  public Category requireActiveOwnedExpenseCategory(UUID userId, UUID categoryId) {
    Category category = requireOwnedCategory(userId, categoryId);
    if (!category.isActive()) {
      throw new BusinessRuleException(CATEGORY_INACTIVE);
    }
    if (category.getType() != CategoryType.EXPENSE) {
      throw new BusinessRuleException(CATEGORY_NOT_EXPENSE);
    }
    return category;
  }

  private Category saveUnique(Category category) {
    try {
      return categoryRepository.saveAndFlush(category);
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException(CATEGORY_NAME_TYPE_CONFLICT);
    }
  }
}
