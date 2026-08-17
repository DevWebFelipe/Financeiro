package br.com.financialcontrol.transfers;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.transfers.dto.CreateTransferRequest;
import br.com.financialcontrol.transfers.dto.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class TransferController {

  private final TransferService transferService;

  public TransferController(TransferService transferService) {
    this.transferService = transferService;
  }

  @GetMapping
  @Operation(summary = "Listar transferências do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de transferências"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public List<TransferResponse> list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) UUID accountId) {
    return transferService.list(authenticatedUser, startDate, endDate, accountId);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar uma transferência")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Transferência encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Transferência não encontrada")
  })
  public TransferResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return transferService.get(authenticatedUser, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar transferência")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Transferência criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public TransferResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody CreateTransferRequest request) {
    return transferService.create(authenticatedUser, request);
  }

  @PostMapping("/{id}/reverse")
  @Operation(summary = "Estornar transferência ACTIVE")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Transferência estornada"),
    @ApiResponse(responseCode = "400", description = "Transição inválida ou saldo insuficiente"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Transferência não encontrada")
  })
  public TransferResponse reverse(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return transferService.reverse(authenticatedUser, id);
  }
}
