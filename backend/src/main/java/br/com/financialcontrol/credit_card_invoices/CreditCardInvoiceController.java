package br.com.financialcontrol.credit_card_invoices;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.credit_card_invoice_agreements.CreditCardInvoiceAgreementService;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.CreateAgreementRequest;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.RenegotiateAgreementRequest;
import br.com.financialcontrol.credit_card_invoices.dto.CreateInvoiceAdjustmentRequest;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoiceAdjustmentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoicePaymentResponse;
import br.com.financialcontrol.credit_card_invoices.dto.PayInvoiceRequest;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
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
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class CreditCardInvoiceController {

  private final CreditCardInvoiceService creditCardInvoiceService;
  private final CreditCardInvoiceAgreementService agreementService;

  public CreditCardInvoiceController(
      CreditCardInvoiceService creditCardInvoiceService,
      CreditCardInvoiceAgreementService agreementService) {
    this.creditCardInvoiceService = creditCardInvoiceService;
    this.agreementService = agreementService;
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Fatura encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public CreditCardInvoiceResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardInvoiceService.get(authenticatedUser, id);
  }

  @GetMapping("/{id}/items")
  @Operation(summary = "Listar parcelas da fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Itens da fatura"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public List<ExpenseInstallmentResponse> listItems(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardInvoiceService.listItems(authenticatedUser, id);
  }

  @PostMapping("/{id}/payments")
  @Operation(summary = "Pagar fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento registrado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public InvoicePaymentResponse pay(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody PayInvoiceRequest request) {
    return creditCardInvoiceService.pay(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/payments")
  @Operation(summary = "Listar pagamentos da fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamentos da fatura"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public List<InvoicePaymentResponse> listPayments(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardInvoiceService.listPayments(authenticatedUser, id);
  }

  @PostMapping("/{invoiceId}/payments/{paymentId}/reverse")
  @Operation(summary = "Estornar pagamento ACTIVE da fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento estornado"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura ou pagamento não encontrado")
  })
  public InvoicePaymentResponse reversePayment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID invoiceId,
      @PathVariable UUID paymentId) {
    return creditCardInvoiceService.reversePayment(authenticatedUser, invoiceId, paymentId);
  }

  @PostMapping("/{id}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar ajuste da fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Ajuste criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public InvoiceAdjustmentResponse createAdjustment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody CreateInvoiceAdjustmentRequest request) {
    return creditCardInvoiceService.createAdjustment(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/adjustments")
  @Operation(summary = "Listar ajustes da fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ajustes da fatura"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public List<InvoiceAdjustmentResponse> listAdjustments(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return creditCardInvoiceService.listAdjustments(authenticatedUser, id);
  }

  @PostMapping("/{invoiceId}/adjustments/{adjustmentId}/reverse")
  @Operation(summary = "Estornar ajuste ACTIVE da fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ajuste estornado"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura ou ajuste não encontrado")
  })
  public InvoiceAdjustmentResponse reverseAdjustment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID invoiceId,
      @PathVariable UUID adjustmentId) {
    return creditCardInvoiceService.reverseAdjustment(authenticatedUser, invoiceId, adjustmentId);
  }

  @PostMapping("/{id}/agreements")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Nova negociação de fatura fechada")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Negociação criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public AgreementResponse createAgreement(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody CreateAgreementRequest request) {
    return agreementService.createAgreement(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/renegotiations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Renegociação de fatura fechada")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Renegociação criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public AgreementResponse renegotiate(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody RenegotiateAgreementRequest request) {
    return agreementService.renegotiate(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/agreements")
  @Operation(summary = "Listar negociações da fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Negociações da fatura"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public List<AgreementResponse> listAgreements(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return agreementService.listByInvoice(authenticatedUser, id);
  }
}
