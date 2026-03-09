package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    //Find all payments for a specific card
    List<Payment> findByCard_CardIdOrderByPaymentDateDesc(UUID cardId);

    //Find all payments for a specific billing cycle
    List<Payment> findByBillingCycle_CycleId(UUID cycleId);

    //Find all payments for a specific card in a specific billing cycle
    List<Payment> findByCard_CardIdAndBillingCycle_CycleId(
            UUID cardId,
            UUID cycleId
    );

    //Find payments by status
    List<Payment> findByPaymentStatus(Payment.PaymentStatus paymentStatus);

    //Calculate total amount paid for a billing cycle
    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) " +
        "FROM Payment p" +
        "WHERE p.billingCycle.cycleId = :cycleId "  +
        "AND p.paymentStatus = 'SUCCESS'")
    BigDecimal findTotalPaidByCycleId(@Param("cycleId") UUID cycleId);
}