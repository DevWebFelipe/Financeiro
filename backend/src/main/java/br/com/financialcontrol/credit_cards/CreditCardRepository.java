package br.com.financialcontrol.credit_cards;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {}
