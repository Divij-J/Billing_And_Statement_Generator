package com.example.billing_and_statement_generator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.validator.constraints.CreditCardNumber;
import org.springframework.cglib.core.Local;

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

    @ManyToOne
    @JoinColumn(name = "cycle_id", nullable = true)
    private BillingCycle billingCycle;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "transaction_type")
    @NotNull
    private transactionType transactionType;

    @Column(name = "amount")
    @PositiveOrZero
    @NotNull
    private BigDecimal amount;

    @Column(name = "merchant_name")
    @NotBlank
    private String merchantName;

    @Column(name = "status")
    @NotNull
    private Status status;

    public enum transactionType{
        PURCHASE,
        CASHADVANCE,
        PAYMENT
    }

    public enum Status{
        SENT,
        PENDING,
        DECLINED,
        REVERTED
    }
}