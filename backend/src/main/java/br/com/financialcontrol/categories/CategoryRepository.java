package br.com.financialcontrol.categories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

  List<Category> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

  List<Category> findAllByUserIdAndTypeOrderByCreatedAtAsc(UUID userId, CategoryType type);

  List<Category> findAllByUserIdAndActiveOrderByCreatedAtAsc(UUID userId, boolean active);

  List<Category> findAllByUserIdAndTypeAndActiveOrderByCreatedAtAsc(
      UUID userId, CategoryType type, boolean active);

  Optional<Category> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByUserIdAndTypeAndNameIgnoreCase(UUID userId, CategoryType type, String name);

  boolean existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(
      UUID userId, CategoryType type, String name, UUID id);
}
