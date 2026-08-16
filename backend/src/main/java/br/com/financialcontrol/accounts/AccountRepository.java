package br.com.financialcontrol.accounts;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  List<Account> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

  Optional<Account> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Account a WHERE a.id = :id AND a.userId = :userId")
  Optional<Account> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
