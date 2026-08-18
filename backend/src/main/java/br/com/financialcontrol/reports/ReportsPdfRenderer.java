package br.com.financialcontrol.reports;

import br.com.financialcontrol.credit_card_invoices.dto.InvoiceAdjustmentResponse;
import br.com.financialcontrol.expenses.dto.AdjustmentResponse;
import br.com.financialcontrol.projections.dto.ProjectionMonthResponse;
import br.com.financialcontrol.projections.dto.ProjectionSummaryResponse;
import br.com.financialcontrol.reports.dto.CardReportCreditApplicationResponse;
import br.com.financialcontrol.reports.dto.CardReportItemResponse;
import br.com.financialcontrol.reports.dto.CardReportPurchaseInstallmentResponse;
import br.com.financialcontrol.reports.dto.CardReportPurchaseResponse;
import br.com.financialcontrol.reports.dto.CardReportResponse;
import br.com.financialcontrol.reports.dto.CashFlowHistoricalResponse;
import br.com.financialcontrol.reports.dto.CashFlowItemResponse;
import br.com.financialcontrol.reports.dto.CashFlowProjectedResponse;
import br.com.financialcontrol.reports.dto.CashFlowResponse;
import br.com.financialcontrol.reports.dto.CategoryReportItemResponse;
import br.com.financialcontrol.reports.dto.CategoryReportResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportInstallmentResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportItemResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportResponse;
import br.com.financialcontrol.reports.dto.ExpenseReportSummaryResponse;
import br.com.financialcontrol.reports.dto.IncomeReportItemResponse;
import br.com.financialcontrol.reports.dto.IncomeReportResponse;
import br.com.financialcontrol.reports.dto.IncomeReportSummaryResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportAllocationResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportCategoryGroupResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportPurchaseResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportResponse;
import br.com.financialcontrol.reports.dto.InvoiceReportResponsibleGroupResponse;
import br.com.financialcontrol.reports.dto.ReportPeriodResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportItemResponse;
import br.com.financialcontrol.reports.dto.ResponsibleReportResponse;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

@Component
class ReportsPdfRenderer {

  private static final ZoneId FINANCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter GENERATED =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final Font TITLE =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1252, true, 14);
  private static final Font SUBTITLE =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1252, true, 11);
  private static final Font BODY =
      FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, true, 9);
  private static final Font SMALL =
      FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, true, 8);
  private static final Font HEADER =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1252, true, 8);

  byte[] expenses(ExpenseReportResponse report, Instant generatedAt) {
    return write(
        "Relatório de despesas",
        report.period(),
        generatedAt,
        document -> {
          addExpenseSummary(document, report.summary());
          addSection(document, "Despesas");
          if (report.items().isEmpty()) {
            addEmpty(document);
            return;
          }
          PdfPTable table =
              table(
                  8,
                  "Descrição",
                  "Data",
                  "Forma",
                  "Status",
                  "Origem",
                  "Obrigação",
                  "Pago",
                  "Restante");
          for (ExpenseReportItemResponse item : report.items()) {
            table.addCell(text(item.description()));
            table.addCell(text(date(item.expenseDate())));
            table.addCell(text(enumName(item.paymentMethod())));
            table.addCell(text(enumName(item.status())));
            table.addCell(text(enumName(item.origin())));
            table.addCell(amount(item.periodObligation()));
            table.addCell(amount(item.periodPaid()));
            table.addCell(amount(item.periodRemaining()));
            for (ExpenseReportInstallmentResponse installment : item.installments()) {
              table.addCell(
                  text(
                      "Parcela "
                          + installment.installmentNumber()
                          + "/"
                          + installment.totalInstallments()));
              table.addCell(text(date(installment.dueDate())));
              table.addCell(text(""));
              table.addCell(text(enumName(installment.status())));
              table.addCell(text(""));
              table.addCell(amount(installment.obligation()));
              table.addCell(amount(installment.paid()));
              table.addCell(amount(installment.remaining()));
            }
          }
          document.add(table);
        });
  }

  byte[] incomes(IncomeReportResponse report, Instant generatedAt) {
    return write(
        "Relatório de receitas",
        report.period(),
        generatedAt,
        document -> {
          document.add(new Paragraph("dateType: " + report.dateType(), BODY));
          addIncomeSummary(document, report.summary());
          addSection(document, "Receitas");
          if (report.items().isEmpty()) {
            addEmpty(document);
            return;
          }
          PdfPTable table =
              table(
                  7,
                  "Descrição",
                  "Status",
                  "Prevista",
                  "Valor",
                  "Acréscimo",
                  "Recebido",
                  "Restante");
          for (IncomeReportItemResponse item : report.items()) {
            table.addCell(text(item.description()));
            table.addCell(text(enumName(item.status())));
            table.addCell(text(date(item.expectedDate())));
            table.addCell(amount(item.amount()));
            table.addCell(amount(item.accruedAmount()));
            table.addCell(
                amount(
                    item.periodReceivedAmount() != null
                        ? item.periodReceivedAmount()
                        : item.receivedAmount()));
            table.addCell(amount(item.remainingAmount()));
          }
          document.add(table);
        });
  }

  byte[] categories(CategoryReportResponse report, Instant generatedAt) {
    return write(
        "Relatório por categorias",
        report.period(),
        generatedAt,
        document -> {
          document.add(new Paragraph("dateType: " + report.dateType(), BODY));
          if (report.summary().expense() != null) {
            addSection(document, "Resumo de despesas");
            addExpenseSummary(document, report.summary().expense());
          }
          if (report.summary().income() != null) {
            addSection(document, "Resumo de receitas");
            addIncomeSummary(document, report.summary().income());
          }
          addSection(document, "Categorias");
          if (report.items().isEmpty()) {
            addEmpty(document);
            return;
          }
          PdfPTable table = table(4, "Categoria", "Tipo", "Ativa", "Valor");
          for (CategoryReportItemResponse item : report.items()) {
            table.addCell(text(item.name()));
            table.addCell(text(enumName(item.type())));
            table.addCell(text(item.active() ? "sim" : "não"));
            BigDecimal value =
                item.periodObligation() != null
                    ? item.periodObligation()
                    : item.periodReceivedAmount() != null
                        ? item.periodReceivedAmount()
                        : item.amount();
            table.addCell(amount(value));
          }
          document.add(table);
        });
  }

  byte[] responsibles(ResponsibleReportResponse report, Instant generatedAt) {
    return write(
        "Relatório por responsáveis",
        report.period(),
        generatedAt,
        document -> {
          document.add(new Paragraph("nature: " + report.nature(), BODY));
          if (report.dateType() != null) {
            document.add(new Paragraph("dateType: " + report.dateType(), BODY));
          }
          if (report.summary().expense() != null) {
            addSection(document, "Resumo de despesas");
            addExpenseSummary(document, report.summary().expense());
          }
          if (report.summary().income() != null) {
            addSection(document, "Resumo de receitas");
            addIncomeSummary(document, report.summary().income());
          }
          addSection(document, "Responsáveis");
          if (report.items().isEmpty()) {
            addEmpty(document);
            return;
          }
          PdfPTable table = table(3, "Responsável", "Despesas", "Receitas");
          for (ResponsibleReportItemResponse item : report.items()) {
            String name =
                item.responsibleName() == null
                    ? enumName(item.responsibleType())
                    : enumName(item.responsibleType()) + " / " + item.responsibleName();
            table.addCell(text(name));
            table.addCell(
                text(item.expense() == null ? "-" : money(item.expense().periodObligation())));
            table.addCell(
                text(
                    item.income() == null
                        ? "-"
                        : money(
                            item.income().periodReceivedAmount() != null
                                ? item.income().periodReceivedAmount()
                                : item.income().amount())));
          }
          document.add(table);
        });
  }

  byte[] cards(CardReportResponse report, Instant generatedAt) {
    return write(
        "Relatório de cartões",
        report.period(),
        generatedAt,
        document -> {
          document.add(
              new Paragraph(
                  "Compras: "
                      + money(report.summary().purchaseAmount())
                      + " | Faturas: "
                      + money(report.summary().invoiceAmount())
                      + " | Pagos: "
                      + money(report.summary().paidAmount())
                      + " | Créditos: "
                      + money(report.summary().creditAmount()),
                  BODY));
          if (report.items().isEmpty()) {
            addEmpty(document);
            return;
          }
          for (CardReportItemResponse card : report.items()) {
            addSection(
                document,
                card.name()
                    + " — "
                    + card.holderName()
                    + (card.lastFourDigits() == null ? "" : " final " + card.lastFourDigits()));
            document.add(
                new Paragraph(
                    "Compras: "
                        + money(card.summary().purchaseAmount())
                        + " | Faturas: "
                        + money(card.summary().invoiceAmount()),
                    BODY));
            addSection(document, "Compras");
            if (card.purchases().isEmpty()) {
              addEmpty(document);
            } else {
              PdfPTable purchases = table(5, "Descrição", "Data", "Original", "Parcelas", "Status");
              for (CardReportPurchaseResponse purchase : card.purchases()) {
                purchases.addCell(text(purchase.description()));
                purchases.addCell(text(date(purchase.expenseDate())));
                purchases.addCell(amount(purchase.original()));
                purchases.addCell(text(String.valueOf(purchase.totalInstallments())));
                purchases.addCell(text(enumName(purchase.status())));
                for (CardReportPurchaseInstallmentResponse installment : purchase.installments()) {
                  purchases.addCell(text("Parcela " + installment.installmentNumber()));
                  purchases.addCell(text(date(installment.dueDate())));
                  purchases.addCell(amount(installment.amount()));
                  purchases.addCell(text(""));
                  purchases.addCell(text(""));
                }
              }
              document.add(purchases);
            }
            addSection(document, "Faturas");
            PdfPTable invoices =
                table(6, "Referência", "Fechamento", "Vencimento", "Status", "Total", "Restante");
            card.invoices()
                .forEach(
                    invoice -> {
                      invoices.addCell(
                          text(invoice.referenceYear() + "-" + invoice.referenceMonth()));
                      invoices.addCell(text(date(invoice.closingDate())));
                      invoices.addCell(text(date(invoice.dueDate())));
                      invoices.addCell(text(enumName(invoice.status())));
                      invoices.addCell(amount(invoice.totalAmount()));
                      invoices.addCell(amount(invoice.remainingAmount()));
                    });
            if (!card.invoices().isEmpty()) {
              document.add(invoices);
            }
            addLabeledList(
                document,
                "Pagamentos de fatura",
                card.payments().stream()
                    .map(payment -> date(payment.paymentDate()) + " " + money(payment.amount()))
                    .toList());
            addLabeledList(
                document,
                "Créditos",
                card.credits().stream()
                    .map(credit -> instant(credit.createdAt()) + " " + money(credit.amount()))
                    .toList());
            addLabeledList(
                document,
                "Ajustes de parcela",
                card.installmentAdjustments().stream()
                    .map(this::installmentAdjustmentLine)
                    .toList());
            addLabeledList(
                document,
                "Ajustes de fatura",
                card.invoiceAdjustments().stream().map(this::invoiceAdjustmentLine).toList());
          }
        });
  }

  byte[] cashFlow(CashFlowResponse report, Instant generatedAt) {
    return write(
        "Relatório de fluxo de caixa",
        report.period(),
        generatedAt,
        document -> {
          document.add(new Paragraph("flowType: " + report.flowType(), BODY));
          document.add(
              new Paragraph(report.accountId() == null ? "Consolidado" : "Conta filtrada", BODY));
          CashFlowHistoricalResponse historical = report.historical();
          if (historical != null) {
            addSection(document, "Histórico");
            if (historical.openingBalance() != null) {
              document.add(
                  new Paragraph("Saldo inicial: " + money(historical.openingBalance()), BODY));
            }
            if (historical.closingBalance() != null) {
              document.add(
                  new Paragraph("Saldo final: " + money(historical.closingBalance()), BODY));
            }
            if (historical.summary() != null) {
              document.add(
                  new Paragraph(
                      "Entradas: "
                          + money(historical.summary().totalIn())
                          + " | Saídas: "
                          + money(historical.summary().totalOut())
                          + " | Líquido: "
                          + money(historical.summary().net()),
                      BODY));
            }
            if (historical.items().isEmpty()) {
              addEmpty(document);
            } else {
              PdfPTable table = table(4, "Data", "Tipo", "Descrição", "Valor");
              for (CashFlowItemResponse item : historical.items()) {
                table.addCell(text(date(item.date())));
                table.addCell(text(enumName(item.type())));
                table.addCell(text(item.description()));
                table.addCell(amount(item.amount()));
              }
              document.add(table);
            }
          }
          CashFlowProjectedResponse projected = report.projected();
          if (projected != null) {
            addSection(document, "Projeção");
            if (Boolean.TRUE.equals(projected.empty())) {
              document.add(new Paragraph("empty: true", BODY));
            } else {
              ProjectionSummaryResponse summary = projected.summary();
              if (summary != null) {
                document.add(
                    new Paragraph(
                        "Saldo atual: "
                            + money(summary.currentBalance())
                            + " | Receitas: "
                            + money(summary.projectedIncome())
                            + " | Despesas: "
                            + money(summary.projectedExpense())
                            + " | Final: "
                            + money(summary.projectedFinalBalance()),
                        BODY));
              }
              if (projected.months() != null && !projected.months().isEmpty()) {
                PdfPTable months =
                    table(5, "Mês", "Abertura", "Receitas", "Despesas", "Fechamento");
                for (ProjectionMonthResponse month : projected.months()) {
                  months.addCell(text(month.period()));
                  months.addCell(amount(month.openingBalance()));
                  months.addCell(amount(month.totalIncome()));
                  months.addCell(amount(month.totalExpense()));
                  months.addCell(amount(month.closingBalance()));
                }
                document.add(months);
              }
            }
          }
        });
  }

  byte[] invoice(InvoiceReportResponse report, Instant generatedAt) {
    return write(
        "Relatório de fatura",
        null,
        generatedAt,
        document -> {
          document.add(
              new Paragraph(
                  "Cartão: "
                      + report.card().name()
                      + " — "
                      + report.card().holderName()
                      + (report.card().lastFourDigits() == null
                          ? ""
                          : " final " + report.card().lastFourDigits()),
                  BODY));
          document.add(
              new Paragraph(
                  "Referência: "
                      + report.invoice().referenceYear()
                      + "-"
                      + report.invoice().referenceMonth()
                      + " | Fechamento: "
                      + date(report.invoice().closingDate())
                      + " | Vencimento: "
                      + date(report.invoice().dueDate())
                      + " | Status: "
                      + enumName(report.invoice().status()),
                  BODY));
          document.add(
              new Paragraph(
                  "Total: "
                      + money(report.invoice().totalAmount())
                      + " | Pago: "
                      + money(report.invoice().paidAmount())
                      + " | Restante: "
                      + money(report.invoice().remainingAmount()),
                  BODY));
          addSection(document, "Compras");
          if (report.purchases().isEmpty()) {
            addEmpty(document);
          } else {
            PdfPTable purchases =
                table(6, "Descrição", "Data", "Original", "Categoria", "Responsável", "Parcela");
            for (InvoiceReportPurchaseResponse purchase : report.purchases()) {
              purchases.addCell(text(purchase.description()));
              purchases.addCell(text(date(purchase.expenseDate())));
              purchases.addCell(amount(purchase.original()));
              purchases.addCell(text(purchase.categoryName()));
              purchases.addCell(
                  text(
                      purchase.responsibleName() == null
                          ? enumName(purchase.responsibleType())
                          : enumName(purchase.responsibleType())
                              + " / "
                              + purchase.responsibleName()));
              purchases.addCell(
                  text(purchase.installmentNumber() + "/" + purchase.totalInstallments()));
            }
            document.add(purchases);
          }
          addSection(document, "Por categoria");
          PdfPTable byCategory = table(2, "Categoria", "Original");
          for (InvoiceReportCategoryGroupResponse group : report.byCategory()) {
            byCategory.addCell(text(group.name()));
            byCategory.addCell(amount(group.original()));
          }
          if (!report.byCategory().isEmpty()) {
            document.add(byCategory);
          }
          addSection(document, "Por responsável");
          PdfPTable byResponsible = table(2, "Responsável", "Original");
          for (InvoiceReportResponsibleGroupResponse group : report.byResponsible()) {
            byResponsible.addCell(
                text(
                    group.responsibleName() == null
                        ? enumName(group.responsibleType())
                        : enumName(group.responsibleType()) + " / " + group.responsibleName()));
            byResponsible.addCell(amount(group.original()));
          }
          if (!report.byResponsible().isEmpty()) {
            document.add(byResponsible);
          }
          addLabeledList(
              document,
              "Ajustes de parcela",
              report.installmentAdjustments().stream()
                  .map(this::installmentAdjustmentLine)
                  .toList());
          addLabeledList(
              document,
              "Ajustes de fatura",
              report.invoiceAdjustments().stream().map(this::invoiceAdjustmentLine).toList());
          addLabeledList(
              document, "Créditos", report.credits().stream().map(this::creditLine).toList());
          addLabeledList(
              document,
              "Pagamentos",
              report.payments().stream()
                  .map(payment -> date(payment.paymentDate()) + " " + money(payment.amount()))
                  .toList());
          addLabeledList(
              document,
              "Alocações",
              report.allocations().stream().map(this::allocationLine).toList());
        });
  }

  private byte[] write(
      String title, ReportPeriodResponse period, Instant generatedAt, PdfBody body) {
    Document document = new Document(PageSize.A4, 36, 36, 48, 42);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      PdfWriter writer = PdfWriter.getInstance(document, output);
      writer.setCompressionLevel(0);
      PageNumbers numbers = new PageNumbers();
      writer.setPageEvent(numbers);
      document.open();
      document.add(new Paragraph("Financial Control", TITLE));
      document.add(new Paragraph(title, SUBTITLE));
      if (period != null) {
        document.add(
            new Paragraph(
                "Período: " + date(period.startDate()) + " a " + date(period.endDate()), BODY));
      }
      document.add(
          new Paragraph(
              "Gerado em: " + GENERATED.format(generatedAt.atZone(FINANCIAL_ZONE)), BODY));
      document.add(new Paragraph(" "));
      body.write(document);
      document.close();
    } catch (DocumentException exception) {
      throw new IllegalStateException("Falha ao gerar PDF.", exception);
    }
    return output.toByteArray();
  }

  private static void addSection(Document document, String title) throws DocumentException {
    Paragraph paragraph = new Paragraph(title, SUBTITLE);
    paragraph.setSpacingBefore(8);
    paragraph.setSpacingAfter(4);
    document.add(paragraph);
  }

  private static void addEmpty(Document document) throws DocumentException {
    document.add(new Paragraph("Sem itens.", BODY));
  }

  private static void addExpenseSummary(Document document, ExpenseReportSummaryResponse summary)
      throws DocumentException {
    document.add(
        new Paragraph(
            "Original: "
                + money(summary.periodOriginal())
                + " | Desconto: "
                + money(summary.periodDiscount())
                + " | Acréscimo: "
                + money(summary.periodSurcharge())
                + " | Obrigação: "
                + money(summary.periodObligation())
                + " | Pago: "
                + money(summary.periodPaid())
                + " | Restante: "
                + money(summary.periodRemaining()),
            BODY));
  }

  private static void addIncomeSummary(Document document, IncomeReportSummaryResponse summary)
      throws DocumentException {
    if (summary.periodReceivedAmount() != null && summary.amount() == null) {
      document.add(
          new Paragraph("Recebido no período: " + money(summary.periodReceivedAmount()), BODY));
      return;
    }
    document.add(
        new Paragraph(
            "Valor: "
                + money(summary.amount())
                + " | Acréscimo: "
                + money(summary.accruedAmount())
                + " | Recebido: "
                + money(summary.receivedAmount())
                + " | Restante: "
                + money(summary.remainingAmount()),
            BODY));
  }

  private void addLabeledList(Document document, String title, List<String> lines)
      throws DocumentException {
    addSection(document, title);
    if (lines.isEmpty()) {
      addEmpty(document);
      return;
    }
    for (String line : lines) {
      document.add(new Paragraph(line, SMALL));
    }
  }

  private String installmentAdjustmentLine(AdjustmentResponse adjustment) {
    return enumName(adjustment.type())
        + " "
        + money(adjustment.amount())
        + " "
        + instant(adjustment.createdAt());
  }

  private String invoiceAdjustmentLine(InvoiceAdjustmentResponse adjustment) {
    return enumName(adjustment.type())
        + " "
        + money(adjustment.amount())
        + " "
        + instant(adjustment.createdAt());
  }

  private String creditLine(CardReportCreditApplicationResponse credit) {
    return instant(credit.createdAt()) + " " + money(credit.amount());
  }

  private String allocationLine(InvoiceReportAllocationResponse allocation) {
    return enumName(allocation.type())
        + " "
        + money(allocation.amount())
        + " "
        + instant(allocation.createdAt());
  }

  private static PdfPTable table(int columns, String... headers) {
    PdfPTable table = new PdfPTable(columns);
    table.setWidthPercentage(100);
    table.setSpacingBefore(4);
    table.setSpacingAfter(6);
    for (String header : headers) {
      PdfPCell cell = new PdfPCell(new Phrase(header, HEADER));
      cell.setPadding(3);
      table.addCell(cell);
    }
    table.setHeaderRows(1);
    return table;
  }

  private static PdfPCell text(String value) {
    PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, SMALL));
    cell.setPadding(3);
    return cell;
  }

  private static PdfPCell amount(BigDecimal value) {
    PdfPCell cell = text(money(value));
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    return cell;
  }

  static String money(BigDecimal value) {
    if (value == null) {
      return "";
    }
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("pt", "BR"));
    symbols.setGroupingSeparator('.');
    symbols.setDecimalSeparator(',');
    DecimalFormat format = new DecimalFormat("'R$' #,##0.00", symbols);
    return format.format(value);
  }

  private static String date(LocalDate value) {
    return value == null ? "" : DATE.format(value);
  }

  private static String instant(Instant value) {
    return value == null ? "" : DATE.format(value.atZone(FINANCIAL_ZONE).toLocalDate());
  }

  private static String enumName(Enum<?> value) {
    return value == null ? "" : value.name();
  }

  @FunctionalInterface
  private interface PdfBody {
    void write(Document document) throws DocumentException;
  }

  private static final class PageNumbers extends PdfPageEventHelper {
    private PdfTemplate total;

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
      total = writer.getDirectContent().createTemplate(30, 12);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfPTable footer = new PdfPTable(1);
      footer.setTotalWidth(document.right() - document.left());
      footer.getDefaultCell().setBorder(0);
      footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
      Phrase phrase = new Phrase("Página " + writer.getPageNumber() + "/", SMALL);
      phrase.add(new org.openpdf.text.Chunk(Image.getInstance(total), 0, 0));
      footer.addCell(phrase);
      footer.writeSelectedRows(
          0, -1, document.left(), document.bottom() - 12, writer.getDirectContent());
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
      ColumnText.showTextAligned(
          total,
          Element.ALIGN_LEFT,
          new Phrase(String.valueOf(Math.max(writer.getPageNumber() - 1, 1)), SMALL),
          0,
          0,
          0);
    }
  }
}
