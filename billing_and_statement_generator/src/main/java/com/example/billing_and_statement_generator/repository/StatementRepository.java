package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID>{

    //Find statement by billing cycle
    @Query("SELECT s FROM Statement s WHERE s.billingCycle.cycle_id = :cycleId")
    Optional<Statement> findByCycleId(@Param("cycleId") UUID cycleId);

    //Find all statements for a specific card
    @Query("SELECT s FROM Statement s WHERE s.card.card_id = :cardId ORDER BY s.statementDate Desc")
    List<Statement> findByCard_Card_idOrderByStatementDateDesc(UUID cardId);

    //Check if statement already exists before generating
    @Query("SELECT COUNT(s) > 0 FROM Statement s WHERE s.billingCycle.cycle_id = :cycleId")
    boolean existsByCycleId(@Param("cycleId") UUID cycleId);

    //Find statements by status
    List<Statement> findByStatementStatus(Statement.StatementStatus statementStatus);

    //Find all statements for a specific card and status
    @Query("SELECT s FROM Statement s WHERE s.card.card_id = :cardId AND s.statementStatus = :status")
    List<Statement> findByCardIdAndStatementStatus(
            @Param("cardId") UUID cardId,
            @Param("status") Statement.StatementStatus statementStatus
    );
}