package br.com.financialcontrol.incomes;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.incomes.dto.CreateIncomeAccrualRequest;
import br.com.financialcontrol.incomes.dto.CreateIncomeReceiptRequest;
import br.com.financialcontrol.incomes.dto.CreateIncomeRequest;
import br.com.financialcontrol.incomes.dto.IncomeMovementPageResponse;
import br.com.financialcontrol.incomes.dto.IncomeMovementResponse;
import br.com.financialcontrol.incomes.dto.IncomePageResponse;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
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

  @PostMapping("/{id}/accruals")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Lançar acréscimo na receita")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Acréscimo criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeMovementResponse createAccrual(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody CreateIncomeAccrualRequest request) {
    return incomeService.createAccrual(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/receipts")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar recebimento da receita")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Recebimento criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeMovementResponse createReceipt(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody CreateIncomeReceiptRequest request) {
    return incomeService.createReceipt(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/movements")
  @Operation(summary = "Listar movimentações da receita")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Histórico da receita"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita não encontrada")
  })
  public IncomeMovementPageResponse listMovements(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return incomeService.listMovements(authenticatedUser, id, page, size);
  }

  @PostMapping("/{id}/movements/{movementId}/reverse")
  @Operation(summary = "Estornar uma movimentação da receita")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Movimentação estornada"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Receita ou movimentação não encontrada")
  })
  public IncomeMovementResponse reverseMovement(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @PathVariable UUID movementId) {
    return incomeService.reverseMovement(authenticatedUser, id, movementId);
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
