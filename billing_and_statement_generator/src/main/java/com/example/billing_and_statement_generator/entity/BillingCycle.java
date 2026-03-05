package com.example.billing_and_statement_generator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.Bigdecimal;
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
    private UUID cycle_id;
    @ManyToOne
    @Joincolumn(name = "card_id")
    private Card card;       //FK to cardid

    @Column(name = "cycle_start_date")
    private LocalDate cycle_start_date;

    @Column(name = "cycle_end_date")
    private LocalDate cycle_end_date;

    @Column(name = "due_date")
    private LocalDate due_date;

    @Column(name = "credit_limit", precision= 15, scale = 2)
    private BigDecimal credit_limit;

    @Column(name = "previous_balance", precision= 15, scale = 2)
    private BigDecimal previous_balance;

    @Column(name = "total_purchases", precision= 15, scale = 2)
    private BigDecimal total_purchases;

    @Column(name = "total_cash_advance", precision= 15, scale = 2)
    private BigDecimal total_cash_advance;

    @Column(name = "total_interest", precision= 15, scale = 2)
    private BigDecimal total_interest;

    @Column(name = "total_outstanding", precision= 15, scale = 2)
    private BigDecimal total_outstanding;

    @Column(name = "minimum_due", precision= 15, scale = 2)
    private BigDecimal minimum_due;

    @Column(name = "cycle_status")
    private String cycle_status;

    @OneToMany(mappedBy = "BillingCycle")
    private  List<Transaction> transactions;
}