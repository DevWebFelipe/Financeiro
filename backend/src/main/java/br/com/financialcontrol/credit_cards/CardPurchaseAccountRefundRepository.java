package br.com.financialcontrol.credit_cards;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardPurchaseAccountRefundRepository
    extends JpaRepository<CardPurchaseAccountRefund, UUID> {

  @Query(
      """
      SELECT COALESCE(SUM(r.amount), 0)
      FROM CardPurchaseAccountRefund r
      WHERE r.account.id = :accountId AND r.userId = :userId
      """)
  BigDecimal sumAmountByAccountIdAndUserId(
      @Param("accountId") UUID accountId, @Param("userId") UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(r.amount), 0)
      FROM CardPurchaseAccountRefund r
      WHERE r.account.id = :accountId AND r.userId = :userId
        AND r.createdAt <= :asOfInstant
      """)
  BigDecimal sumAmountByAccountIdAndUserIdAsOf(
      @Param("accountId") UUID accountId,
      @Param("userId") UUID userId,
      @Param("asOfInstant") Instant asOfInstant);

  boolean existsByAccount_IdAndUserId(UUID accountId, UUID userId);

  @EntityGraph(attributePaths = {"expense", "account"})
  @Query(
      """
      SELECT r FROM CardPurchaseAccountRefund r
      WHERE r.userId = :userId
        AND r.createdAt >= :startInstant
        AND r.createdAt <= :endInstant
      """)
  List<CardPurchaseAccountRefund> findAllByUserIdAndCreatedAtBetween(
      @Param("userId") UUID userId,
      @Param("startInstant") Instant startInstant,
      @Param("endInstant") Instant endInstant);
}
