package com.example.billing_and_statement_generator.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class BillingCycleResponseDTO{
    private UUID cycleId;
    private UUID cardId;

    //Dates
    private LocalDate cycle_start_date;
    private LocalDate cycle_end_date;
    private LocalDate due_date;

    private BigDecimal credit_limit;
    private BigDecimal previous_balance;
    private BigDecimal total_purchases;
    private BigDecimal total_cash_advance;
    private BigDecimal total_interest;
    private BigDecimal total_outstanding;
    private BigDecimal minimum_due;
    private BigDecimal creditLimit;
    //Status
    private String cycle_status;

    // Transaction List
    private List<CreateTransactionResponseDTO> transactions;
}