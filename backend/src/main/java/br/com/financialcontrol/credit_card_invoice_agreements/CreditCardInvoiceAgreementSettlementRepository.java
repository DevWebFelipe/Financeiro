package br.com.financialcontrol.credit_card_invoice_agreements;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardInvoiceAgreementSettlementRepository
    extends JpaRepository<CreditCardInvoiceAgreementSettlement, UUID> {}
