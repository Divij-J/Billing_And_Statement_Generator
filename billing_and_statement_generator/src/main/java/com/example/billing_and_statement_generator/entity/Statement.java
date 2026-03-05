package com.example.billing_and_statement_generator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table (name = "statements")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@AllArgsConstructor
@Builder
public class Statement {

    @Id
    @Column(name = "statement_id", nullable = false, updatable = false)
    private UUID statementId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false, unique = true)
    private BillingCycle billingCycle;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotNull
    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @PositiveOrZero
    @Column(name = "previous_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal previousBalance;

    @NotNull
    @PositiveOrZero
    @Column(name = "new_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal newBalance;

    @NotNull
    @PositiveOrZero
    @Column(name = "minimum_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumDue;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_outstanding", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalOutstanding;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_fee_applied", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalFeeApplied;

    @NotNull
    @PositiveOrZero
    @Column(name = "cash_advance_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal cashAdvanceFee;

    @NotNull
    @PositiveOrZero
    @Column(name = "carry_forward_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal carryForwardBalance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statement_status", nullable = false)
    private StatementStatus statementStatus;

    //STATEMENT STATUS ENUM
    public enum StatementStatus {
        GENERATED,
        PAID,
        UNPAID
    }
}
