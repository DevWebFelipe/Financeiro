package br.com.financialcontrol.credit_cards;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceService;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceStatus;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_cards.dto.CreateCreditCardCreditRequest;
import br.com.financialcontrol.credit_cards.dto.CreateCreditCardRequest;
import br.com.financialcontrol.credit_cards.dto.CreditCardCreditResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardLimitResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import br.com.financialcontrol.credit_cards.dto.UpdateCreditCardRequest;
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
@RequestMapping("/api/v1/credit-cards")
@Tag(name = "Credit cards")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class CreditCardController {

  private final CreditCardService creditCardService;
  private final CreditCardInvoiceService creditCardInvoiceService;

  public CreditCardController(
      CreditCardService creditCardService, CreditCardInvoiceService creditCardInvoiceService) {
    this.creditCardService = creditCardService;
    this.creditCardInvoiceService = creditCardInvoiceService;
  }

  @GetMapping
  @Operation(summary = "Listar os cartões do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cartões do usuário autenticado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public List<CreditCardResponse> list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(required = false) String holderName) {
    return creditCardService.list(authenticatedUser, holderName);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar um cartão do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cartão encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public CreditCardResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardService.get(authenticatedUser, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar cartão de crédito")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Cartão criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public CreditCardResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody CreateCreditCardRequest request) {
    return creditCardService.create(authenticatedUser, request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cartão atualizado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public CreditCardResponse update(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCreditCardRequest request) {
    return creditCardService.update(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Desativar cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cartão desativado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public CreditCardResponse deactivate(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardService.deactivate(authenticatedUser, id);
  }

  @PostMapping("/{id}/activate")
  @Operation(summary = "Reativar cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cartão reativado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public CreditCardResponse activate(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardService.activate(authenticatedUser, id);
  }

  @GetMapping("/{id}/limit")
  @Operation(summary = "Consultar o limite derivado do cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Limite do cartão"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public CreditCardLimitResponse getLimit(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardService.getLimit(authenticatedUser, id);
  }

  @GetMapping("/{id}/credits")
  @Operation(summary = "Listar créditos do cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Créditos do cartão"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public List<CreditCardCreditResponse> listCredits(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardService.listCredits(authenticatedUser, id);
  }

  @PostMapping("/{id}/credits")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Lançar crédito manual no cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Crédito criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public CreditCardCreditResponse createCredit(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody CreateCreditCardCreditRequest request) {
    return creditCardInvoiceService.createManualCredit(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/invoices")
  @Operation(summary = "Listar faturas do cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Faturas do cartão"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
  })
  public List<CreditCardInvoiceResponse> listInvoices(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) CreditCardInvoiceStatus status) {
    return creditCardInvoiceService.listByCard(authenticatedUser, id, year, month, status);
  }

  @GetMapping("/{id}/invoices/current")
  @Operation(summary = "Consultar a fatura OPEN do cartão")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Fatura atual"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public CreditCardInvoiceResponse currentInvoice(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardInvoiceService.current(authenticatedUser, id);
  }
}
