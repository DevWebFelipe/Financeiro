package br.com.financialcontrol.credit_card_invoices;

import br.com.financialcontrol.expenses.AdjustmentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoiceAdjustmentAllocationRepository
    extends JpaRepository<CreditCardInvoiceAdjustmentAllocation, UUID> {

  List<CreditCardInvoiceAdjustmentAllocation> findAllByInvoiceAdjustment_IdAndUserId(
      UUID invoiceAdjustmentId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM CreditCardInvoiceAdjustmentAllocation a
      WHERE a.installment.id = :installmentId
        AND a.userId = :userId
        AND a.invoiceAdjustment.status =
            br.com.financialcontrol.expenses.AdjustmentStatus.ACTIVE
        AND a.invoiceAdjustment.type = :type
      """)
  BigDecimal sumActiveAmountByInstallmentIdAndUserIdAndType(
      @Param("installmentId") UUID installmentId,
      @Param("userId") UUID userId,
      @Param("type") AdjustmentType type);
}
