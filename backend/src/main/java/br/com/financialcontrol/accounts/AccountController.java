package br.com.financialcontrol.accounts;

import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.accounts.dto.CreateAccountRequest;
import br.com.financialcontrol.accounts.dto.UpdateAccountRequest;
import br.com.financialcontrol.accounts.dto.UpdateInitialBalanceRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  @Operation(summary = "Listar as contas do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contas do usuário autenticado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public List<AccountResponse> list(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return accountService.list(authenticatedUser);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar uma conta do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conta encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public AccountResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return accountService.get(authenticatedUser, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar conta financeira")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Conta criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public AccountResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody CreateAccountRequest request) {
    return accountService.create(authenticatedUser, request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar nome e tipo da conta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conta atualizada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public AccountResponse update(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateAccountRequest request) {
    return accountService.update(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Desativar conta (desativação lógica)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conta desativada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public AccountResponse deactivate(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return accountService.deactivate(authenticatedUser, id);
  }

  @PostMapping("/{id}/activate")
  @Operation(summary = "Reativar conta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conta reativada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public AccountResponse activate(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return accountService.activate(authenticatedUser, id);
  }

  @PutMapping("/{id}/initial-balance")
  @Operation(summary = "Definir ou alterar o saldo inicial (somente sem movimentação)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Saldo inicial atualizado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou conta já movimentada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public AccountResponse updateInitialBalance(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateInitialBalanceRequest request) {
    return accountService.updateInitialBalance(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/balance")
  @Operation(summary = "Consultar o saldo derivado da conta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Saldo da conta"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  })
  public AccountBalanceResponse balance(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return accountService.getBalance(authenticatedUser, id);
  }
}
