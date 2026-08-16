package br.com.financialcontrol.credit_cards;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardCreditRepository extends JpaRepository<CreditCardCredit, UUID> {

  List<CreditCardCredit> findAllByCreditCard_IdAndUserIdOrderByCreatedAtAscIdAsc(
      UUID creditCardId, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT c FROM CreditCardCredit c
      WHERE c.creditCard.id = :creditCardId AND c.userId = :userId
      ORDER BY c.createdAt ASC, c.id ASC
      """)
  List<CreditCardCredit> findAllByCardForUpdate(
      @Param("creditCardId") UUID creditCardId, @Param("userId") UUID userId);

  Optional<CreditCardCredit> findByIdAndCreditCard_IdAndUserId(
      UUID id, UUID creditCardId, UUID userId);

  @Query(
      """
      SELECT COALESCE(SUM(c.amount), 0)
      FROM CreditCardCredit c
      WHERE c.creditCard.id = :creditCardId AND c.userId = :userId
      """)
  BigDecimal sumAmountByCardIdAndUserId(
      @Param("creditCardId") UUID creditCardId, @Param("userId") UUID userId);
}
