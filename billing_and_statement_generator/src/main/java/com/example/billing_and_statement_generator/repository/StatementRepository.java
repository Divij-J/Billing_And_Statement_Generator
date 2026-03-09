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
    Optional<Statement> findByBillingCycle_CycleId(UUID cycleId);

    //Find all statements for a specific card
    List<Statement> findByCard_CardIdOrderByStatementDateDesc(UUID cardId);

    //Check if statement already exists before generating
    boolean existsByBillingCycle_CycleId(UUID cycleId);

    //Find statements by status
    List<Statement> findByStatementStatus(
            Statement.StatementStatus statementStatus
    );

    //Find all statements for a specific card
    List<Statement> findByCard_CardIdAndStatement(
            UUID cardId,
            Statement.StatementStatus statementStatus
    );
}