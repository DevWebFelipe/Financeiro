package br.com.financialcontrol.expenses;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseInstallmentAdjustmentRepository
    extends JpaRepository<ExpenseInstallmentAdjustment, UUID> {

  Optional<ExpenseInstallmentAdjustment> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT a FROM ExpenseInstallmentAdjustment a
      WHERE a.id = :id
        AND a.userId = :userId
      """)
  Optional<ExpenseInstallmentAdjustment> findByIdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("userId") UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT a FROM ExpenseInstallmentAdjustment a
      WHERE a.id = :id
        AND a.installment.id = :installmentId
        AND a.userId = :userId
      """)
  Optional<ExpenseInstallmentAdjustment> findByIdAndInstallment_IdAndUserIdForUpdate(
      @Param("id") UUID id,
      @Param("installmentId") UUID installmentId,
      @Param("userId") UUID userId);

  /** Full history for an installment (ACTIVE and REVERSED). */
  List<ExpenseInstallmentAdjustment> findAllByInstallment_IdAndUserIdOrderByCreatedAtAscIdAsc(
      UUID installmentId, UUID userId);

  List<ExpenseInstallmentAdjustment>
      findAllByInstallment_IdAndUserIdAndStatusOrderByCreatedAtAscIdAsc(
          UUID installmentId, UUID userId, AdjustmentStatus status);

  Optional<ExpenseInstallmentAdjustment> findByIdAndInstallment_IdAndUserId(
      UUID id, UUID installmentId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM ExpenseInstallmentAdjustment a
      WHERE a.installment.id = :installmentId
        AND a.userId = :userId
        AND a.type = br.com.financialcontrol.expenses.AdjustmentType.DISCOUNT
        AND a.status = br.com.financialcontrol.expenses.AdjustmentStatus.ACTIVE
      """)
  BigDecimal sumActiveDiscountAmountByInstallmentIdAndUserId(
      @Param("installmentId") UUID installmentId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(a.amount), 0)
      FROM ExpenseInstallmentAdjustment a
      WHERE a.installment.id = :installmentId
        AND a.userId = :userId
        AND a.type = br.com.financialcontrol.expenses.AdjustmentType.SURCHARGE
        AND a.status = br.com.financialcontrol.expenses.AdjustmentStatus.ACTIVE
      """)
  BigDecimal sumActiveSurchargeAmountByInstallmentIdAndUserId(
      @Param("installmentId") UUID installmentId, @Param("userId") UUID userId);
}
