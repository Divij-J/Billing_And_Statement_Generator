package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.BillingCycleIdDTO;
import com.example.billing_and_statement_generator.dto.card.CardIdDTO;
import com.example.billing_and_statement_generator.dto.transaction.*;
import com.example.billing_and_statement_generator.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Controller", description = "Endpoints for Transaction operations")
public class TransactionController {

    private final TransactionService transactionService;

    // POST: Endpoint for creating a transaction (purchase, cash advance, payment)
    @PostMapping("/v1/createTransaction")
    @Operation(summary = "Create Transaction (V1)",
        description = "Create a Purchase or Cash Advance transaction")
    public ResponseEntity<CreateTransactionResponseDTO> create(@Valid @RequestBody CreateTransactionRequestDTO dto) {
        CreateTransactionResponseDTO response = transactionService.create(dto);
        return ResponseEntity.status(201).body(response);
    }

    // POST: Endpoint for fetching a transaction by its ID
    @PostMapping("/v1/getTransactionById")
    @Operation(summary = "Fetch Transaction (V1)",
        description = "Fetches information about a specific transaction by its ID")
    public ResponseEntity<CreateTransactionResponseDTO> getById(@Valid @RequestBody TransactionIdDTO dto) {
        return ResponseEntity.ok(transactionService.getById(dto.getTransactionId()));
    }

    // POST: Endpoint for fetching list of transactions by its Card ID
    @PostMapping("/v1/getTransactionsByCardId")
    @Operation(summary = "Fetch All Transactions (V1)",
        description = "Fetches all transactions from a card ID")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCard(@Valid @RequestBody CardIdDTO dto) {
        return ResponseEntity.ok(transactionService.listByCard(dto.getCardId()));
    }

    // POST: Endpoint for fetching ALL transactions from a certain Billing Cycle ID
    @PostMapping("/v1/getTransactionsByBillingCycle")
    @Operation(summary = "Fetch All Transactions by Cycle ID (V1)",
            description = "Fetches all transactions from a Cycle ID")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCycle(@Valid @RequestBody BillingCycleIdDTO dto) {
        return ResponseEntity.ok(transactionService.listByCycle(dto.getCycleId()));
    }

    // POST: Endpoint for fetching transactions based on Card ID and date range
    @PostMapping("/v1/getTransactionsWithinDateRange")
    @Operation(summary = "Fetch All Transactions by a Date Range (V1)",
            description = "Fetches all transactions with a range of dates")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByDateRange(@Valid @RequestBody GetTransactionsBetweenDatesRequestDTO dto) {
        return ResponseEntity.ok(
                transactionService.listByCardAndDateRange(dto.getCardId(), dto.getStartDate(), dto.getEndDate())
        );
    }

    // POST: Endpoint for fetching transactions by Card ID and Billing Cycle ID
    @PostMapping("/v1/getTransactionsByCardAndCycleID")
    @Operation(summary = "Fetch All Transactions by Card and Cycle ID (V1)",
            description = "Fetches all transactions from a Card and Cycle ID")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCardAndCycle(@Valid @RequestBody GetTransactionsByCardIdCycleIdRequestDTO dto) {
        return ResponseEntity.ok(
                transactionService.listByCardAndBillingCycle(dto.getCardId(), dto.getCycleId())
        );
    }
}