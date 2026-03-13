package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    // find transactions by Card ID
    List<Transaction> findByCardCardID(UUID cardId);

    // find transactions by Billing Cycle ID
    List<Transaction> findByBillingCycleCycle_id(UUID cycleId);

    // find transactions by Card and Billing Cycle IDs
    List<Transaction> findByCardCardIdAndBillingCycle_id(UUID cardId, UUID cycleId);

    // retrieves transactions between a specific set of dates
    List<Transaction> findByCardCardIdAndTransactionDateBetween(UUID cardId, LocalDate startDate, LocalDate endDate);
}