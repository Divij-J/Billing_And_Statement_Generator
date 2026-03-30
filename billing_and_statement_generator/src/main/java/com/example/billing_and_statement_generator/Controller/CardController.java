package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/cards")
@RequiredArgsConstructor
@Tag(name = "Card Controller", description = "Endpoints for handling card operations")
public class CardController {

    private final CardService cardService;

    // POST: Endpoint for creating new card (v1)
    @PostMapping("/v1")
    @Operation(summary = "Create a card (V1)",
            description = "Create a card with given information for a customer")
    public ResponseEntity<CreateCardResponseDTO> createCardV1(@Valid @RequestBody CreateCardRequestDTO dto) {
        CreateCardResponseDTO response = cardService.create(dto);
        return ResponseEntity.status(201).body(response);
    }

    // POST: Endpoint for finding information about a specific card
    @PostMapping("/v1/{cardId}")
    @Operation(summary = "Card Information (V1)",
            description = "Fetch card information from a card ID")
    public ResponseEntity<CreateCardResponseDTO> getCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getById(cardId));
    }

    // POST: Endpoint for fetching all cards to a customer
    @PostMapping("/v1/customer/cards/{customerId}")
    @Operation(summary = "Fetch All Cards (V1)",
            description = "Fetches all cards from a customer ID")
    public ResponseEntity<List<CreateCardResponseDTO>> getCardsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(cardService.getByCustomer(customerId));
    }

    // POST: Endpoint for fetching card balance for a specific card
    @PostMapping("/v1/balance/{cardId}")
    @Operation(summary = "Fetch Card Balance",
            description = "Fetches card balance for a specific card")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getTotalBalance(cardId));
    }
}