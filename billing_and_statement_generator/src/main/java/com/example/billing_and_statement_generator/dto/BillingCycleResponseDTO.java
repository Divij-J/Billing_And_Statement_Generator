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
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;
    private LocalDate dueDate;

    //Financial Figures
    private BigDecimal creditLimit;
    private BigDecimal previousBalance;
    private BigDecimal totalPurchases;
    private BigDecimal totalCashAdvance;
    private BigDecimal totalInterest;
    private BigDecimal totalOutstanding;
    private BigDecimal minimumDue;
    private BigDecimal creditLimit;
    //Status
    private String cycleStatus;

    // Transaction List
    private List<CreateTransactionResponseDTO> transactions;
}