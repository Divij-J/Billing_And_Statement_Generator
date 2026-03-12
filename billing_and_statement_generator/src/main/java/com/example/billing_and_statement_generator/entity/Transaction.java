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
    /*
        Columns - varnames
        Transaction ID - transaction_id
        Card ID - card_id
        Cycle ID - cycle_id
        Transaction Date - transaction_date
        Transaction Type (Purchase/Cash Advance/Payment) - transaction_type
        Amount - amount
        Merchant Name - merchant_name
        Status (Sent/Pending/Declined/Reverted) - status
    * */
    @Id
    @Column(name = "transaction_id")
    private UUID transaction_id;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne
    @JoinColumn(name = "cycle_id", nullable = true)
    private BillingCycle billingCycle;

    @Column(name = "transaction_date")
    private LocalDate transaction_date;

    @Column(name = "transaction_type")
    @NotNull
    private transactionType transaction_type;

    @Column(name = "amount")
    @PositiveOrZero
    @NotNull
    private BigDecimal amount;

    @Column(name = "merchant_name")
    @NotBlank
    private String merchant_name;

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