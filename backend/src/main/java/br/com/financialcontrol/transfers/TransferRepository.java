package br.com.financialcontrol.transfers;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

  Optional<Transfer> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM Transfer t WHERE t.id = :id AND t.userId = :userId")
  Optional<Transfer> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

  @Query(
      """
      SELECT t FROM Transfer t
      WHERE t.userId = :userId
        AND (CAST(:startDate AS LocalDate) IS NULL OR t.transferDate >= :startDate)
        AND (CAST(:endDate AS LocalDate) IS NULL OR t.transferDate <= :endDate)
        AND (:accountId IS NULL
             OR t.sourceAccount.id = :accountId
             OR t.destinationAccount.id = :accountId)
      ORDER BY t.transferDate ASC, t.createdAt ASC, t.id ASC
      """)
  List<Transfer> searchByUser(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("accountId") UUID accountId);

  @Query(
      """
      SELECT COALESCE(SUM(t.amount), 0)
      FROM Transfer t
      WHERE t.userId = :userId
        AND t.destinationAccount.id = :accountId
        AND t.status = br.com.financialcontrol.transfers.TransferStatus.ACTIVE
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR t.transferDate <= :asOfDate)
      """)
  BigDecimal sumActiveIncomingByAccountIdAndUserId(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  @Query(
      """
      SELECT COALESCE(SUM(t.amount), 0)
      FROM Transfer t
      WHERE t.userId = :userId
        AND t.sourceAccount.id = :accountId
        AND t.status = br.com.financialcontrol.transfers.TransferStatus.ACTIVE
        AND (CAST(:asOfDate AS LocalDate) IS NULL OR t.transferDate <= :asOfDate)
      """)
  BigDecimal sumActiveOutgoingByAccountIdAndUserId(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfDate") LocalDate asOfDate);

  boolean existsBySourceAccount_IdAndUserId(UUID accountId, UUID userId);

  boolean existsByDestinationAccount_IdAndUserId(UUID accountId, UUID userId);
}
