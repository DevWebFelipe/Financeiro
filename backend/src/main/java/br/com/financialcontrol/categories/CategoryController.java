package br.com.financialcontrol.categories;

import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.categories.dto.CreateCategoryRequest;
import br.com.financialcontrol.categories.dto.UpdateCategoryRequest;
import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  @Operation(summary = "Listar as categorias do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Categorias do usuário autenticado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public List<CategoryResponse> list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(required = false) CategoryType type,
      @RequestParam(required = false) Boolean active) {
    return categoryService.list(authenticatedUser, type, active);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar categoria")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Categoria criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "409", description = "Nome e tipo já existem para o usuário")
  })
  public CategoryResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody CreateCategoryRequest request) {
    return categoryService.create(authenticatedUser, request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar nome e tipo da categoria")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada"),
    @ApiResponse(responseCode = "409", description = "Nome e tipo já existem para o usuário")
  })
  public CategoryResponse update(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCategoryRequest request) {
    return categoryService.update(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Desativar categoria (desativação lógica)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Categoria desativada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
  })
  public CategoryResponse deactivate(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return categoryService.deactivate(authenticatedUser, id);
  }
}
