package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.BillingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository

public interface BillingCycleRepository extends JpaRepository<BillingCycle, UUID> {
    List<BillingCycle> findByCardId(UUID cardId);
    Optional<BillingCycle> findTopByCard_CardIdOrderByCycleEndDateDesc(UUID cardId);
    List<BillingCycle> findbyCard_CardIdAndCycleStatus(UUID cardId, String cycleStatus);
}
