package com.example.billing_and_statement_generator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.validator.constraints.CreditCardNumber;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "card")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@Builder
public class Card {
    /*  Columns - varnames
        Card ID (PK) - card_id
        Customer ID (FK) - customer_id
        Card Number - card_number
        Card Type - card_type
        Card Holder Name - card_holder_name
        Card Issue Date - card_issue_date
        Expiry Date - expiry_date
        isActive - is_active
        Credit Limit - credit_limit
        Annual Interest Rate - annual_interest_rate
        Billing Cycle Date - billing_cycle_date
        Late Fee Amount - late_fee_amount
        Cash Advance Fee Rate - cash_advance_fee_rate
        Security Code - security_code
        Annual Membership Fee - annual_membership_fee
        Cash Advance Limit - cash_advance_limit
    */

    @Id
    @Column(name = "card_id", nullable = false, unique = true)
    private UUID card_id;

//    @ManyToOne
//    @JoinColumn(name = "customer_id")
//    private Customer customer;

    @NotBlank
    @CreditCardNumber
    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String card_number;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType card_type;

    @NotBlank
    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @NotNull
    @Column(name = "card_issue_date", nullable = false)
    private LocalDate card_issue_date;

    @NotNull
    @Column(name = "expiry_date", nullable = false, length = 6)
    private LocalDate expiry_date; //YYYYMM

    @NotNull
    @Column(name = "isActive", nullable = false)
    private boolean is_active;

    @NotNull
    @PositiveOrZero
    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "annual_interest_rate")
    private BigDecimal annual_interest_rate;

    @Column(name = "billing_cycle_date")
    private LocalDate billing_cycle_date;

    @Column(name = "late_fee_amount")
    @PositiveOrZero
    private BigDecimal late_fee_amount;

    @Column(name = "cash_advance_fee_rate")
    @PositiveOrZero
    private BigDecimal cash_advance_fee_rate;

    @NotBlank(message = "CVV must be 3 digits")
    @Column(name = "security_code", nullable = false, length = 3)
    private String securityCode;

    @Column(name = "annual_membership_fee")
    @PositiveOrZero
    private BigDecimal annual_membership_fee;

    @Column(name = "cash_advance_limit")
    @PositiveOrZero
    private BigDecimal cash_advance_limit;

    //CARD TYPE ENUM
    public enum CardType {
        CREDIT,
        DEBIT,
        LOYALTY,
        PREPAID
    }
}