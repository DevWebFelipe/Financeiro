package br.com.financialcontrol.credit_card_invoice_agreements;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoiceAgreementSettlementAllocationRepository
    extends JpaRepository<CreditCardInvoiceAgreementSettlementAllocation, UUID> {

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
