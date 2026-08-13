package br.com.financialcontrol.credit_card_invoices;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardInvoicePaymentRepository
    extends JpaRepository<CreditCardInvoicePayment, UUID> {}
