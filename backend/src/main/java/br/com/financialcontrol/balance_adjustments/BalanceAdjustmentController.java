package br.com.financialcontrol.balance_adjustments;

import br.com.financialcontrol.balance_adjustments.dto.BalanceAdjustmentResponse;
import br.com.financialcontrol.balance_adjustments.dto.CreateBalanceAdjustmentRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/balance-adjustments")
@Tag(name = "Balance Adjustments")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class BalanceAdjustmentController {

  private final BalanceAdjustmentService balanceAdjustmentService;

  public BalanceAdjustmentController(BalanceAdjustmentService balanceAdjustmentService) {
    this.balanceAdjustmentService = balanceAdjustmentService;
  }

  @GetMapping
  @Operation(summary = "Listar acertos de saldo da conta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de acertos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public List<BalanceAdjustmentResponse> list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID accountId) {
    return balanceAdjustmentService.list(authenticatedUser, accountId);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar um acerto de saldo")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Acerto encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Acerto ou conta não encontrado")
  })
  public BalanceAdjustmentResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID accountId,
      @PathVariable UUID id) {
    return balanceAdjustmentService.get(authenticatedUser, accountId, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar acerto de saldo")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Acerto criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public BalanceAdjustmentResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID accountId,
      @Valid @RequestBody CreateBalanceAdjustmentRequest request) {
    return balanceAdjustmentService.create(authenticatedUser, accountId, request);
  }

  @PostMapping("/{id}/reverse")
  @Operation(summary = "Estornar acerto de saldo ACTIVE")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Acerto estornado"),
    @ApiResponse(responseCode = "400", description = "Transição inválida ou saldo insuficiente"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Acerto ou conta não encontrado")
  })
  public BalanceAdjustmentResponse reverse(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID accountId,
      @PathVariable UUID id) {
    return balanceAdjustmentService.reverse(authenticatedUser, accountId, id);
  }
}
