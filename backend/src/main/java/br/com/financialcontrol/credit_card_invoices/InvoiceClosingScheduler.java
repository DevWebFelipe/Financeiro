package br.com.financialcontrol.credit_card_invoices;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "financial-control.invoice-scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class InvoiceClosingScheduler {

  private final CreditCardInvoiceService creditCardInvoiceService;

  public InvoiceClosingScheduler(CreditCardInvoiceService creditCardInvoiceService) {
    this.creditCardInvoiceService = creditCardInvoiceService;
  }

  @Scheduled(cron = "0 5 * * * *", zone = "America/Sao_Paulo")
  public void closeDueInvoices() {
    creditCardInvoiceService.closeDueInvoices();
  }
}
