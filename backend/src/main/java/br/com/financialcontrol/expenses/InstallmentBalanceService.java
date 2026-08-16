package br.com.financialcontrol.expenses;

import br.com.financialcontrol.credit_card_invoice_agreements.CreditCardInvoiceAgreementSettlementAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoiceAdjustmentAllocationRepository;
import br.com.financialcontrol.credit_card_invoices.CreditCardInvoicePaymentAllocationRepository;
import br.com.financialcontrol.credit_cards.CreditCardCreditApplicationRepository;
import br.com.financialcontrol.payments.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstallmentBalanceService {

  private final PaymentRepository paymentRepository;
  private final ExpenseInstallmentAdjustmentRepository adjustmentRepository;
  private final CreditCardInvoicePaymentAllocationRepository invoicePaymentAllocationRepository;
  private final CreditCardCreditApplicationRepository creditApplicationRepository;
  private final CreditCardInvoiceAdjustmentAllocationRepository
      invoiceAdjustmentAllocationRepository;
  private final CreditCardInvoiceAgreementSettlementAllocationRepository
      settlementAllocationRepository;

  public InstallmentBalanceService(
      PaymentRepository paymentRepository,
      ExpenseInstallmentAdjustmentRepository adjustmentRepository,
      CreditCardInvoicePaymentAllocationRepository invoicePaymentAllocationRepository,
      CreditCardCreditApplicationRepository creditApplicationRepository,
      CreditCardInvoiceAdjustmentAllocationRepository invoiceAdjustmentAllocationRepository,
      CreditCardInvoiceAgreementSettlementAllocationRepository settlementAllocationRepository) {
    this.paymentRepository = paymentRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.invoicePaymentAllocationRepository = invoicePaymentAllocationRepository;
    this.creditApplicationRepository = creditApplicationRepository;
    this.invoiceAdjustmentAllocationRepository = invoiceAdjustmentAllocationRepository;
    this.settlementAllocationRepository = settlementAllocationRepository;
  }

  public BigDecimal obligation(ExpenseInstallment installment) {
    UUID installmentId = installment.getId();
    UUID userId = installment.getUserId();
    BigDecimal discount =
        zeroIfNull(
            adjustmentRepository.sumActiveDiscountAmountByInstallmentIdAndUserId(
                installmentId, userId));
    BigDecimal surcharge =
        zeroIfNull(
            adjustmentRepository.sumActiveSurchargeAmountByInstallmentIdAndUserId(
                installmentId, userId));
    return normalize(installment.getAmount().add(surcharge).subtract(discount));
  }

  public BigDecimal remaining(ExpenseInstallment installment) {
    UUID installmentId = installment.getId();
    UUID userId = installment.getUserId();
    BigDecimal value =
        obligation(installment)
            .add(
                zeroIfNull(
                    invoiceAdjustmentAllocationRepository
                        .sumActiveAmountByInstallmentIdAndUserIdAndType(
                            installmentId, userId, AdjustmentType.SURCHARGE)))
            .subtract(
                zeroIfNull(
                    invoiceAdjustmentAllocationRepository
                        .sumActiveAmountByInstallmentIdAndUserIdAndType(
                            installmentId, userId, AdjustmentType.DISCOUNT)))
            .subtract(
                zeroIfNull(
                    paymentRepository.sumActiveAmountByInstallmentIdAndUserId(
                        installmentId, userId)))
            .subtract(
                zeroIfNull(
                    invoicePaymentAllocationRepository.sumActiveAmountByInstallmentIdAndUserId(
                        installmentId, userId)))
            .subtract(
                zeroIfNull(
                    creditApplicationRepository.sumAmountByInstallmentIdAndUserId(
                        installmentId, userId)))
            .subtract(
                zeroIfNull(
                    settlementAllocationRepository.sumAmountByInstallmentIdAndUserId(
                        installmentId, userId)));
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return normalize(value);
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
  }

  private static BigDecimal normalize(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
