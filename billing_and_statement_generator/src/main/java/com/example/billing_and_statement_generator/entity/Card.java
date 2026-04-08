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
    @Id
    @Column(name = "card_id", nullable = false, unique = true)
    private UUID cardId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotBlank
    @CreditCardNumber
    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String cardNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @NotBlank
    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @NotNull
    @Column(name = "card_issue_date", nullable = false)
    private LocalDate cardIssueDate;

    @NotNull
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @NotNull
    @PositiveOrZero
    @Column(name = "card_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal cardBalance;

    @NotNull
    @PositiveOrZero
    @Column(name = "cash_advance_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal cashAdvanceBalance;

    @NotNull
    @PositiveOrZero
    @Column(name = "available_credit", nullable = false, precision = 15, scale = 2)
    private BigDecimal availableCredit;

    @NotNull
    @PositiveOrZero
    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "annual_interest_rate", precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(name = "billing_cycle_date")
    private LocalDate billingCycleDate;

    @Column(name = "late_fee_amount", precision = 15, scale = 2)
    @PositiveOrZero
    private BigDecimal lateFeeAmount;

    @Column(name = "cash_advance_fee_rate", precision = 15, scale = 2)
    @PositiveOrZero
    private BigDecimal cashAdvanceFeeRate;

    @Column(name = "cash_advance_APR", precision = 5, scale = 2)
    @PositiveOrZero
    private BigDecimal cashAdvanceAPR;

    @NotBlank(message = "CVV must be 3 digits")
    @Column(name = "security_code", nullable = false, length = 3)
    private String securityCode;

    @Column(name = "annual_membership_fee", precision = 15, scale = 2)
    @PositiveOrZero
    private BigDecimal annualMembershipFee;

    @Column(name = "cash_advance_limit", precision = 15, scale = 2)
    @PositiveOrZero
    private BigDecimal cashAdvanceLimit;

    @Column(name = "minimum_due", precision = 15, scale = 2)
    @PositiveOrZero
    private BigDecimal minimumDue;

    //CARD TYPE ENUM
    public enum CardType {
        CREDIT,
        DEBIT,
        LOYALTY,
        PREPAID
    }
}