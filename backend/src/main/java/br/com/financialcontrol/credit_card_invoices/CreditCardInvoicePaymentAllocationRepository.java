package br.com.financialcontrol.credit_card_invoices;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardInvoicePaymentAllocationRepository
    extends JpaRepository<CreditCardInvoicePaymentAllocation, UUID> {

  List<CreditCardInvoicePaymentAllocation> findAllByInvoicePayment_IdAndUserId(
      UUID invoicePaymentId, UUID userId);

  @Query(
      """
      SELECT DISTINCT a FROM CreditCardInvoicePaymentAllocation a
      JOIN FETCH a.invoicePayment p
      JOIN FETCH a.installment
      WHERE p.invoice.id = :invoiceId
        AND a.userId = :userId
      ORDER BY a.createdAt ASC, a.id ASC
      """)
  List<CreditCardInvoicePaymentAllocation> findAllByInvoice_IdAndUserId(
      @Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM CreditCardInvoicePaymentAllocation a
      WHERE a.installment.id = :installmentId
        AND a.userId = :userId
        AND a.invoicePayment.status =
            br.com.financialcontrol.credit_card_invoices.InvoicePaymentStatus.ACTIVE
      """)
  BigDecimal sumActiveAmountByInstallmentIdAndUserId(
      @Param("installmentId") UUID installmentId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM CreditCardInvoicePaymentAllocation a
      WHERE a.installment.expense.id = :expenseId
        AND a.userId = :userId
        AND a.invoicePayment.status =
            br.com.financialcontrol.credit_card_invoices.InvoicePaymentStatus.ACTIVE
      """)
  BigDecimal sumActiveAmountByExpenseIdAndUserId(
      @Param("expenseId") UUID expenseId, @Param("userId") UUID userId);
}
