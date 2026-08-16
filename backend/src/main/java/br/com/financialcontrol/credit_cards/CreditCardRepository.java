package br.com.financialcontrol.credit_cards;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

  List<CreditCard> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

  List<CreditCard> findAllByUserIdAndHolderNameIgnoreCaseOrderByCreatedAtAsc(
      UUID userId, String holderName);

  Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM CreditCard c WHERE c.id = :id AND c.userId = :userId")
  Optional<CreditCard> findByIdAndUserIdForUpdate(
      @Param("id") UUID id, @Param("userId") UUID userId);

  boolean existsByIdAndUserId(UUID id, UUID userId);
}
