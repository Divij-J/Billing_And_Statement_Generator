package com.example.billing_and_statement_generator.dto.v1;

import com.example.billing_and_statement_generator.dto.CreateTransactionResponseDTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class BillingCycleResponseV1DTO {
    private UUID cycleId;
    private UUID cardId;

    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;
    private LocalDate dueDate;

    private BigDecimal creditLimit;
    private BigDecimal previousBalance;
    private BigDecimal totalPurchases;
    private BigDecimal totalCashAdvance;
    private BigDecimal totalInterest;
    private BigDecimal totalOutstanding;
    private BigDecimal minimumDue;

    private BigDecimal lateFee;
    private BigDecimal cashAdvanceFee;
    private BigDecimal feesApplied;

    private String cycleStatus;
    private List<CreateTransactionResponseDTO> transaction;

}
