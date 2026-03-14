package com.example.billing_and_statement_generator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import org.hibernate.validator.constraints.CreditCardNumber;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
@Entity
@Table(name="billing_cycle")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillingCycle {
    @Id
    @Column(name = "cycle_id", nullable = false)
    private UUID cycleId;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;       //FK to cardid

    @Column(name = "cycle_start_date")
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date")
    private LocalDate cycleEndDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "credit_limit", precision= 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "previous_balance", precision= 15, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "total_purchases", precision= 15, scale = 2)
    private BigDecimal totalPurchases;

    @Column(name = "total_cash_advance", precision= 15, scale = 2)
    private BigDecimal totalCashAdvance;

    @Column(name = "total_interest", precision= 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_outstanding", precision= 15, scale = 2)
    private BigDecimal totalOutstanding;

    @Column(name = "minimum_due", precision= 15, scale = 2)
    private BigDecimal minimumDue;

    @Column(name = "cycle_status")
    private String cycleStatus;

    @OneToMany(mappedBy = "billingCycle")
    private  List<Transaction> transaction;
}