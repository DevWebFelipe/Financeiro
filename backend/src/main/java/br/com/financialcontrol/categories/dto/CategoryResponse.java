package br.com.financialcontrol.categories.dto;

import br.com.financialcontrol.categories.Category;
import br.com.financialcontrol.categories.CategoryType;
import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
    UUID id, String name, CategoryType type, boolean active, Instant createdAt, Instant updatedAt) {

  public static CategoryResponse from(Category category) {
    return new CategoryResponse(
        category.getId(),
        category.getName(),
        category.getType(),
        category.isActive(),
        category.getCreatedAt(),
        category.getUpdatedAt());
  }
}
