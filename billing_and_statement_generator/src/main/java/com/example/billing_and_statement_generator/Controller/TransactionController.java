package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.services.TransactionService;
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
public class TransactionController {

    private final TransactionService transactionService;

    // POST: Endpoint for creating a transaction (purchase, cash advance, payment)
    @PostMapping
    public ResponseEntity<CreateTransactionResponseDTO> create(@Valid @RequestBody CreateTransactionRequestDTO dto) {
        CreateTransactionResponseDTO response = transactionService.create(dto);
        return ResponseEntity.status(201).body(response);
    }

    // GET: Endpoint for fetching a transaction by its ID
    @GetMapping("/{transactionId}")
    public ResponseEntity<CreateTransactionResponseDTO> getById(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getById(transactionId));
    }

    // GET: Endpoint for fetching list of transactions by its Card ID
    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(transactionService.listByCard(cardId));
    }

    // GET: Endpoint for fetching ALL transactions from a certain Billing Cycle ID
    @GetMapping("/billing-cycle/{cycleId}")
    public ResponseEntity<List<CreateTransactionResponseDTO>> listByCycle(@PathVariable UUID cycleId) {
        return ResponseEntity.ok(transactionService.listByCycle(cycleId));
    }

    // GET: Endpoint for fetching transactions based on Card ID and date range
    @GetMapping("/card/{cardId}/range")
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