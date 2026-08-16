package br.com.financialcontrol.expenses;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import br.com.financialcontrol.expenses.dto.CreateAdjustmentRequest;
import br.com.financialcontrol.expenses.dto.CreateExpenseRequest;
import br.com.financialcontrol.expenses.dto.ExpenseInstallmentResponse;
import br.com.financialcontrol.expenses.dto.ExpensePageResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.expenses.dto.PayExpenseRequest;
import br.com.financialcontrol.expenses.dto.RefundExpenseRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseInstallmentRequest;
import br.com.financialcontrol.expenses.dto.UpdateExpenseRequest;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
@Tag(name = "Expenses")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class ExpenseController {

  private final ExpenseService expenseService;

  public ExpenseController(ExpenseService expenseService) {
    this.expenseService = expenseService;
  }

  @GetMapping
  @Operation(summary = "Listar as despesas do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Despesas do usuário autenticado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public ExpensePageResponse list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) ExpenseStatus status,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) UUID creditCardId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) PaymentMethod paymentMethod,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return expenseService.list(
        authenticatedUser,
        startDate,
        endDate,
        status,
        categoryId,
        accountId,
        creditCardId,
        responsibleType,
        paymentMethod,
        page,
        size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar uma despesa do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Despesa encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
  })
  public ExpenseResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return expenseService.get(authenticatedUser, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar despesa simples")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Despesa criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public ExpenseResponse create(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody CreateExpenseRequest request) {
    return expenseService.create(authenticatedUser, request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar despesa aberta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Despesa atualizada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
  })
  public ExpenseResponse update(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateExpenseRequest request) {
    return expenseService.update(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/installments")
  @Operation(summary = "Listar parcelas da despesa")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Parcelas da despesa"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
  })
  public List<ExpenseInstallmentResponse> listInstallments(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return expenseService.listInstallments(authenticatedUser, id);
  }

  @GetMapping("/{expenseId}/installments/{installmentId}")
  @Operation(summary = "Consultar parcela da despesa")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Parcela encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa ou parcela não encontrada")
  })
  public ExpenseInstallmentResponse getInstallment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID expenseId,
      @PathVariable UUID installmentId) {
    return expenseService.getInstallment(authenticatedUser, expenseId, installmentId);
  }

  @PutMapping("/{expenseId}/installments/{installmentId}")
  @Operation(summary = "Editar parcela aberta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Parcela atualizada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa ou parcela não encontrada")
  })
  public ExpenseInstallmentResponse updateInstallment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID expenseId,
      @PathVariable UUID installmentId,
      @Valid @RequestBody UpdateExpenseInstallmentRequest request) {
    return expenseService.updateInstallment(authenticatedUser, expenseId, installmentId, request);
  }

  @PostMapping("/{expenseId}/installments/{installmentId}/payments")
  @Operation(summary = "Pagar parcela identificada")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento registrado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa ou parcela não encontrada")
  })
  public ExpenseResponse payInstallment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID expenseId,
      @PathVariable UUID installmentId,
      @Valid @RequestBody PayExpenseRequest request) {
    return expenseService.payInstallment(authenticatedUser, expenseId, installmentId, request);
  }

  @PostMapping("/{expenseId}/installments/{installmentId}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Criar adjustment da parcela")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Adjustment criado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa ou parcela não encontrada")
  })
  public AdjustmentResponse createAdjustment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID expenseId,
      @PathVariable UUID installmentId,
      @Valid @RequestBody CreateAdjustmentRequest request) {
    return expenseService.createAdjustment(authenticatedUser, expenseId, installmentId, request);
  }

  @GetMapping("/{expenseId}/installments/{installmentId}/adjustments")
  @Operation(summary = "Listar adjustments da parcela")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Histórico de adjustments"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa ou parcela não encontrada")
  })
  public List<AdjustmentResponse> listAdjustments(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID expenseId,
      @PathVariable UUID installmentId) {
    return expenseService.listAdjustments(authenticatedUser, expenseId, installmentId);
  }

  @PostMapping("/{expenseId}/installments/{installmentId}/adjustments/{adjustmentId}/reverse")
  @Operation(summary = "Estornar adjustment ACTIVE")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Adjustment estornado"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "404",
        description = "Despesa, parcela ou adjustment não encontrado")
  })
  public AdjustmentResponse reverseAdjustment(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID expenseId,
      @PathVariable UUID installmentId,
      @PathVariable UUID adjustmentId) {
    return expenseService.reverseAdjustment(
        authenticatedUser, expenseId, installmentId, adjustmentId);
  }

  @PostMapping("/{id}/pay")
  @Operation(summary = "Pagar despesa 1/1")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento registrado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
  })
  public ExpenseResponse pay(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @Valid @RequestBody PayExpenseRequest request) {
    return expenseService.pay(authenticatedUser, id, request);
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancelar despesa aberta")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Despesa cancelada"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
  })
  public ExpenseResponse cancel(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return expenseService.cancel(authenticatedUser, id);
  }

  @PostMapping("/{id}/refund")
  @Operation(summary = "Estornar despesa paga ou parcialmente paga")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Despesa estornada"),
    @ApiResponse(responseCode = "400", description = "Transição inválida"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
  })
  public ExpenseResponse refund(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID id,
      @RequestBody(required = false) RefundExpenseRequest request) {
    return expenseService.refund(authenticatedUser, id, request);
  }

  @GetMapping("/{id}/payments")
  @Operation(summary = "Listar pagamentos da despesa")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamentos da despesa"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
  })
  public List<PaymentResponse> listPayments(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return expenseService.listPayments(authenticatedUser, id);
  }
}
