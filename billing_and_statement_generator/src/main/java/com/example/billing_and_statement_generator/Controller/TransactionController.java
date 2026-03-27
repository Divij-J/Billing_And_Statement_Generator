package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Controller", description = "Endpoints for Transaction operations")
public class TransactionController {

    private final TransactionService transactionService;

    // POST: Endpoint for creating a transaction (purchase, cash advance, payment)
    @PostMapping
    @Operation(summary = "Create Transaction",
        description = "Create a Purchase or Cash Advance transaction")
    public ResponseEntity<CreateTransactionResponseDTO> create(@Valid @RequestBody CreateTransactionRequestDTO dto) {
        CreateTransactionResponseDTO response = transactionService.create(dto);
        return ResponseEntity.status(201).body(response);
    }

    // GET: Endpoint for fetching a transaction by its ID
    @GetMapping("/{transactionId}")
    @Operation(summary = "Fetch Transaction",
        description = "Fetches information about a specific transaction by its ID")
    public ResponseEntity<CreateTransactionResponseDTO> getById(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getById(transactionId));
    }

    // GET: Endpoint for fetching list of transactions by its Card ID
    @GetMapping("/card/{cardId}")
    @Operation(summary = "Fetch All Transactions",
        description = "Fetches all transactions from a card ID")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(transactionService.listByCard(cardId));
    }

    // GET: Endpoint for fetching ALL transactions from a certain Billing Cycle ID
    @GetMapping("/billing-cycle/{cycleId}")
    @Operation(summary = "Fetch All Transactions by Cycle ID",
            description = "Fetches all transactions from a Cycle ID")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCycle(@PathVariable UUID cycleId) {
        return ResponseEntity.ok(transactionService.listByCycle(cycleId));
    }

    // GET: Endpoint for fetching transactions based on Card ID and date range
    @GetMapping("/card/{cardId}/range")
    @Operation(summary = "Fetch All Transactions by a Date Range",
            description = "Fetches all transactions with a range of dates")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByDateRange(
            @PathVariable UUID cardId,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        return ResponseEntity.ok(
                transactionService.listByCardAndDateRange(cardId, start, end)
        );
    }

    // GET: Endpoint for fetching transactions by Card ID and Billing Cycle ID
    @Operation(summary = "Fetch All Transactions by Card and Cycle ID",
            description = "Fetches all transactions from a Card and Cycle ID")
    @GetMapping("/card/{cardId}/billing-cycle/{cycleId}")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCardAndCycle(
            @PathVariable UUID cardId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                transactionService.listByCardAndBillingCycle(cardId, cycleId)
        );
    }
}