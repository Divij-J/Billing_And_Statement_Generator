package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    // fetches Cards for specific customer ID
    List<Card> findByCustomerCustomerId(UUID customerId);
}