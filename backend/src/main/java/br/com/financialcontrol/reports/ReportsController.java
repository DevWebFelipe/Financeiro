package br.com.financialcontrol.reports;

import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.expenses.ExpenseStatus;
import br.com.financialcontrol.expenses.PaymentMethod;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.reports.dto.CardReportResponse;
import br.com.financialcontrol.reports.dto.CashFlowResponse;
import br.com.financialcontrol.reports.dto.CategoryReportResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportResponse;
import br.com.financialcontrol.reports.dto.IncomeReportResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class ReportsController {

  private static final Set<String> EXPENSE_PARAMS =
      Set.of(
          "startDate",
          "endDate",
          "status",
          "categoryId",
          "accountId",
          "creditCardId",
          "responsibleType",
          "responsibleName",
          "paymentMethod",
          "sort",
          "direction",
          "page",
          "size");

  private static final Set<String> INCOME_PARAMS =
      Set.of(
          "dateType",
          "startDate",
          "endDate",
          "status",
          "categoryId",
          "accountId",
          "responsibleType",
          "responsibleName",
          "sort",
          "direction",
          "page",
          "size");

  private static final Set<String> CATEGORY_PARAMS =
      Set.of("dateType", "startDate", "endDate", "sort", "direction", "page", "size");

  private static final Set<String> RESPONSIBLE_PARAMS =
      Set.of("nature", "dateType", "startDate", "endDate", "sort", "direction", "page", "size");

  private static final Set<String> CARD_PARAMS =
      Set.of("startDate", "endDate", "creditCardId", "sort", "direction", "page", "size");

  private static final Set<String> INVOICE_PARAMS = Set.of("responsibleType", "responsibleName");

  private static final Set<String> INVOICE_PDF_PARAMS =
      Set.of("responsibleType", "responsibleName", "page", "size");

  private static final Set<String> CASH_FLOW_PARAMS =
      Set.of("flowType", "startDate", "endDate", "accountId", "sort", "direction", "page", "size");

  private final ReportsService reportsService;

  public ReportsController(ReportsService reportsService) {
    this.reportsService = reportsService;
  }

  @GetMapping("/expenses")
  @Operation(summary = "Relatório de despesas do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada de despesas"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public ExpenseReportResponse listExpenses(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) ExpenseStatus status,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) UUID creditCardId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String responsibleName,
      @RequestParam(required = false) PaymentMethod paymentMethod,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request, EXPENSE_PARAMS);
    return reportsService.listExpenses(
        authenticatedUser,
        startDate,
        endDate,
        status,
        categoryId,
        accountId,
        creditCardId,
        responsibleType,
        responsibleName,
        paymentMethod,
        sort,
        direction,
        page,
        size);
  }

  @GetMapping("/incomes")
  @Operation(summary = "Relatório de receitas do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada de receitas"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public IncomeReportResponse listIncomes(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String dateType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String responsibleName,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request, INCOME_PARAMS);
    return reportsService.listIncomes(
        authenticatedUser,
        dateType,
        startDate,
        endDate,
        status,
        categoryId,
        accountId,
        responsibleType,
        responsibleName,
        sort,
        direction,
        page,
        size);
  }

  @GetMapping("/categories")
  @Operation(summary = "Relatório por categorias do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada por categorias"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public CategoryReportResponse listCategories(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String dateType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request, CATEGORY_PARAMS);
    return reportsService.listCategories(
        authenticatedUser, dateType, startDate, endDate, sort, direction, page, size);
  }

  @GetMapping("/responsibles")
  @Operation(summary = "Relatório por responsáveis do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada por responsáveis"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public ResponsibleReportResponse listResponsibles(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String nature,
      @RequestParam(required = false) String dateType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request, RESPONSIBLE_PARAMS);
    return reportsService.listResponsibles(
        authenticatedUser, nature, dateType, startDate, endDate, sort, direction, page, size);
  }

  @GetMapping("/cards")
  @Operation(summary = "Relatório de cartões do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada de cartões"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public CardReportResponse listCards(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) UUID creditCardId,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request, CARD_PARAMS);
    return reportsService.listCards(
        authenticatedUser, startDate, endDate, creditCardId, sort, direction, page, size);
  }

  @GetMapping("/invoices/{invoiceId}")
  @Operation(summary = "Relatório detalhado de uma fatura do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada da fatura"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
  })
  public InvoiceReportResponse getInvoice(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @PathVariable UUID invoiceId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String responsibleName) {
    rejectUnknownParams(request, INVOICE_PARAMS);
    return reportsService.getInvoice(
        authenticatedUser, invoiceId, responsibleType, responsibleName);
  }

  @GetMapping("/cash-flow")
  @Operation(summary = "Relatório de fluxo de caixa do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada de fluxo de caixa"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public CashFlowResponse listCashFlow(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String flowType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request, CASH_FLOW_PARAMS);
    return reportsService.listCashFlow(
        authenticatedUser, flowType, startDate, endDate, accountId, sort, direction, page, size);
  }

  @GetMapping(value = "/expenses/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "PDF do relatório de despesas")
  public ResponseEntity<byte[]> expensesPdf(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) ExpenseStatus status,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) UUID creditCardId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String responsibleName,
      @RequestParam(required = false) PaymentMethod paymentMethod,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction) {
    rejectUnknownParams(request, EXPENSE_PARAMS);
    return pdf(
        reportsService.expensesPdf(
            authenticatedUser,
            startDate,
            endDate,
            status,
            categoryId,
            accountId,
            creditCardId,
            responsibleType,
            responsibleName,
            paymentMethod,
            sort,
            direction));
  }

  @GetMapping(value = "/incomes/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "PDF do relatório de receitas")
  public ResponseEntity<byte[]> incomesPdf(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String dateType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String responsibleName,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction) {
    rejectUnknownParams(request, INCOME_PARAMS);
    return pdf(
        reportsService.incomesPdf(
            authenticatedUser,
            dateType,
            startDate,
            endDate,
            status,
            categoryId,
            accountId,
            responsibleType,
            responsibleName,
            sort,
            direction));
  }

  @GetMapping(value = "/categories/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "PDF do relatório por categorias")
  public ResponseEntity<byte[]> categoriesPdf(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String dateType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction) {
    rejectUnknownParams(request, CATEGORY_PARAMS);
    return pdf(
        reportsService.categoriesPdf(
            authenticatedUser, dateType, startDate, endDate, sort, direction));
  }

  @GetMapping(value = "/responsibles/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "PDF do relatório por responsáveis")
  public ResponseEntity<byte[]> responsiblesPdf(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String nature,
      @RequestParam(required = false) String dateType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction) {
    rejectUnknownParams(request, RESPONSIBLE_PARAMS);
    return pdf(
        reportsService.responsiblesPdf(
            authenticatedUser, nature, dateType, startDate, endDate, sort, direction));
  }

  @GetMapping(value = "/cards/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "PDF do relatório de cartões")
  public ResponseEntity<byte[]> cardsPdf(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) UUID creditCardId,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction) {
    rejectUnknownParams(request, CARD_PARAMS);
    return pdf(
        reportsService.cardsPdf(
            authenticatedUser, startDate, endDate, creditCardId, sort, direction));
  }

  @GetMapping(value = "/cash-flow/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "PDF do relatório de fluxo de caixa")
  public ResponseEntity<byte[]> cashFlowPdf(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) String flowType,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction) {
    rejectUnknownParams(request, CASH_FLOW_PARAMS);
    return pdf(
        reportsService.cashFlowPdf(
            authenticatedUser, flowType, startDate, endDate, accountId, sort, direction));
  }

  @GetMapping(value = "/invoices/{invoiceId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "PDF do relatório de fatura")
  public ResponseEntity<byte[]> invoicePdf(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @PathVariable UUID invoiceId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String responsibleName) {
    rejectUnknownParams(request, INVOICE_PDF_PARAMS);
    return pdf(
        reportsService.invoicePdf(authenticatedUser, invoiceId, responsibleType, responsibleName));
  }

  private static ResponseEntity<byte[]> pdf(ReportPdf report) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
        .body(report.content());
  }

  private static void rejectUnknownParams(HttpServletRequest request, Set<String> allowed) {
    for (String name : request.getParameterMap().keySet()) {
      if (!allowed.contains(name)) {
        throw new InvalidRequestException(ReportsService.INVALID_DATA);
      }
    }
  }
}
