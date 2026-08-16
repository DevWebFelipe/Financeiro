package br.com.financialcontrol.credit_card_invoice_agreements;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AgreementResponse;
import br.com.financialcontrol.credit_card_invoice_agreements.dto.AnticipateAgreementInstallmentRequest;
import br.com.financialcontrol.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agreements")
@Tag(name = "Agreements")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class CreditCardInvoiceAgreementController {

  private final CreditCardInvoiceAgreementService agreementService;

  public CreditCardInvoiceAgreementController(CreditCardInvoiceAgreementService agreementService) {
    this.agreementService = agreementService;
  }

  @GetMapping("/{agreementId}")
  @Operation(summary = "Consultar negociação de fatura")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Negociação encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Negociação não encontrada")
  })
  public AgreementResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID agreementId) {
    return agreementService.get(authenticatedUser, agreementId);
  }

  @PostMapping("/{agreementId}/installments/{installmentId}/anticipate")
  @Operation(summary = "Antecipar parcela da negociação")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Antecipação registrada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Negociação ou parcela não encontrada")
  })
  public AgreementResponse anticipate(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID agreementId,
      @PathVariable UUID installmentId,
      @Valid @RequestBody AnticipateAgreementInstallmentRequest request) {
    return agreementService.anticipate(authenticatedUser, agreementId, installmentId, request);
  }
}
