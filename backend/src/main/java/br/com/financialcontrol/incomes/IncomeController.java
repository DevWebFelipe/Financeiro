package br.com.financialcontrol.incomes;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.incomes.dto.CreateIncomeRequest;
import br.com.financialcontrol.incomes.dto.IncomePageResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import br.com.financialcontrol.incomes.dto.ReceiveIncomeRequest;
import br.com.financialcontrol.incomes.dto.UpdateIncomeRequest;
import br.com.financialcontrol.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/incomes")
@Tag(name = "Incomes")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class IncomeController {

  private final IncomeService incomeService;

  public IncomeController(IncomeService incomeService) {
    this.incomeService = incomeService;
  }

  @GetMapping
  @Operation(summary = "Listar as receitas do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Receitas do usuário autenticado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public IncomePageResponse list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) IncomeStatus status,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return incomeService.list(
        authenticatedUser, startDate, endDate, status, categoryId, accountId, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar uma receita do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Receita encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return incomeService.get(authenticatedUser, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar receita prevista")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Receita criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public IncomeResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody CreateIncomeRequest request) {
    return incomeService.create(authenticatedUser, request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar receita esperada")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Receita atualizada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeResponse update(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateIncomeRequest request) {
    return incomeService.update(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/receive")
  @Operation(summary = "Receber receita esperada")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Receita recebida"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeResponse receive(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody ReceiveIncomeRequest request) {
    return incomeService.receive(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/reverse")
  @Operation(summary = "Estornar recebimento de receita")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Recebimento estornado"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeResponse reverse(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return incomeService.reverse(authenticatedUser, id);
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancelar receita esperada")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Receita cancelada"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeResponse cancel(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return incomeService.cancel(authenticatedUser, id);
  }
}
