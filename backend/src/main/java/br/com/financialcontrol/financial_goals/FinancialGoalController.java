package br.com.financialcontrol.financial_goals;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.financial_goals.dto.CreateFinancialGoalRequest;
import br.com.financialcontrol.financial_goals.dto.CreateGoalContributionRequest;
import br.com.financialcontrol.financial_goals.dto.CreateGoalContributionResponse;
import br.com.financialcontrol.financial_goals.dto.CreateGoalRedemptionRequest;
import br.com.financialcontrol.financial_goals.dto.CreateGoalRedemptionResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalPageResponse;
import br.com.financialcontrol.financial_goals.dto.FinancialGoalResponse;
import br.com.financialcontrol.financial_goals.dto.GoalContributionResponse;
import br.com.financialcontrol.financial_goals.dto.GoalRedemptionResponse;
import br.com.financialcontrol.financial_goals.dto.UpdateFinancialGoalRequest;
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
@RequestMapping("/api/v1/financial-goals")
@Tag(name = "Financial Goals")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class FinancialGoalController {

  private final FinancialGoalService financialGoalService;

  public FinancialGoalController(FinancialGoalService financialGoalService) {
    this.financialGoalService = financialGoalService;
  }

  @GetMapping
  @Operation(summary = "Listar as metas do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Metas do usuário autenticado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public FinancialGoalPageResponse list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(required = false) FinancialGoalStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return financialGoalService.list(authenticatedUser, status, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar uma meta do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Meta encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public FinancialGoalResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return financialGoalService.get(authenticatedUser, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar meta financeira")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Meta criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public FinancialGoalResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody CreateFinancialGoalRequest request) {
    return financialGoalService.create(authenticatedUser, request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar meta ativa")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Meta atualizada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public FinancialGoalResponse update(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateFinancialGoalRequest request) {
    return financialGoalService.update(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/contributions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Contribuir para a meta")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Contribuição criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public CreateGoalContributionResponse contribute(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody CreateGoalContributionRequest request) {
    return financialGoalService.contribute(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/contributions")
  @Operation(summary = "Listar contribuições da meta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contribuições da meta"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public List<GoalContributionResponse> listContributions(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return financialGoalService.listContributions(authenticatedUser, id);
  }

  @PostMapping("/{id}/redemptions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Resgatar valor da meta")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Resgate criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public CreateGoalRedemptionResponse redeem(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody CreateGoalRedemptionRequest request) {
    return financialGoalService.redeem(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/redemptions")
  @Operation(summary = "Listar resgates da meta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Resgates da meta"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public List<GoalRedemptionResponse> listRedemptions(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return financialGoalService.listRedemptions(authenticatedUser, id);
  }

  @PostMapping("/{id}/complete")
  @Operation(summary = "Concluir meta ativa")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Meta concluída"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public FinancialGoalResponse complete(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return financialGoalService.complete(authenticatedUser, id);
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancelar meta ativa sem valor reservado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Meta cancelada"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  })
  public FinancialGoalResponse cancel(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return financialGoalService.cancel(authenticatedUser, id);
  }
}
