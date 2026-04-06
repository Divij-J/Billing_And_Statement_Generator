package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.BillingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingCycleRepository extends JpaRepository<BillingCycle, UUID> {

          List<BillingCycle> findByCardCardId(UUID cardId);

          Optional<BillingCycle> findTopByCardCardIdOrderByCycleEndDateDesc(UUID cardId);

          List<BillingCycle> findByCardCardIdAndCycleStatus(UUID cardId, String cycleStatus);

    Optional<BillingCycle> findTopByCardCardIdOrderByCycleIdDesc(UUID cardId);
}