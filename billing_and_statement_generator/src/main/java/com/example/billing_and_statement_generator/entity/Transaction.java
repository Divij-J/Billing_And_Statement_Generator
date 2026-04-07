package com.example.billing_and_statement_generator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@Builder
public class Transaction{
    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cycle_id", nullable = true)
    private BillingCycle billingCycle;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    @NotNull
    private transactionType transactionType;

    @Column(name = "amount", precision = 15, scale = 2)
    @PositiveOrZero
    @NotNull
    private BigDecimal amount;

    @Column(name = "merchant_name")
    @NotBlank
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @NotNull
    private Status status;

    public enum transactionType{
        PURCHASE,
        CASHADVANCE,
        CASHADVANCEFEE,
        ANNUALMEMBERSHIPFEE,
        LATEFEE,
        INTEREST,
        FEE
    }

    public enum Status{
        SENT,
        PENDING,
        DECLINED,
        REVERTED
    }
}