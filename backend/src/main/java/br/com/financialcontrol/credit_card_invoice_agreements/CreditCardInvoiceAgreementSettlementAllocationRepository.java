package br.com.financialcontrol.credit_card_invoice_agreements;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoiceAgreementSettlementAllocationRepository
    extends JpaRepository<CreditCardInvoiceAgreementSettlementAllocation, UUID> {

  @Query(
      """
      SELECT DISTINCT a FROM CreditCardInvoiceAgreementSettlementAllocation a
      JOIN FETCH a.settlement s
      JOIN FETCH a.installment
      WHERE s.invoice.id = :invoiceId
        AND a.userId = :userId
      ORDER BY a.createdAt ASC, a.id ASC
      """)
  List<CreditCardInvoiceAgreementSettlementAllocation> findAllByInvoice_IdAndUserId(
      @Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM CreditCardInvoiceAgreementSettlementAllocation a
      WHERE a.installment.id = :installmentId
        AND a.userId = :userId
      """)
  BigDecimal sumAmountByInstallmentIdAndUserId(
      @Param("installmentId") UUID installmentId, @Param("userId") UUID userId);
}
