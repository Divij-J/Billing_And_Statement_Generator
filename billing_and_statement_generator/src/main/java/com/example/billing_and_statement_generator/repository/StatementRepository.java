package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID>{

    //Find statement by billing cycle
    Optional<Statement> findByBillingCycle_Cycle_id(UUID cycle_id);

    //Find all statements for a specific card
    List<Statement> findByCard_Card_idOrderByStatementDateDesc(UUID card_id);

    //Check if statement already exists before generating
    boolean existsByBillingCycle_Cycle_id(UUID cycle_id);

    //Find statements by status
    List<Statement> findByStatementStatus(
            Statement.StatementStatus statementStatus
    );

    //Find all statements for a specific card
    List<Statement> findByCard_Card_idAndStatement(
            UUID card_id,
            Statement.StatementStatus statementStatus
    );
}