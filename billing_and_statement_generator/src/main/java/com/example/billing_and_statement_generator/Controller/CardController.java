package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.v1.CreateCardRequestV1DTO;
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

    // POST: Endpoint for creating new card
    @PostMapping
    @Operation(summary = "Create a card",
            description = "Create a card with given information for a customer")
    public ResponseEntity<CreateCardResponseDTO> createCard(@Valid @RequestBody CreateCardRequestDTO dto) {
        CreateCardResponseDTO response = cardService.create(dto);
        return ResponseEntity.status(201).body(response);
    }

    // POST: Endpoint for creating new card (v1)
    @PostMapping("/v1")
    @Operation(summary = "Create a card (v1)",
            description = "Create a card with given information for a customer")
    public ResponseEntity<CreateCardResponseDTO> createCardV1(@Valid @RequestBody CreateCardRequestV1DTO dto) {
        CreateCardResponseDTO response = cardService.createV1(dto);
        return ResponseEntity.status(201).body(response);
    }

    // GET: Endpoint for finding information about a specific card
    @GetMapping("/{cardId}")
    @Operation(summary = "Card Information",
            description = "Fetch card information from a card ID")
    public ResponseEntity<CreateCardResponseDTO> getCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getById(cardId));
    }

    // GET: Endpoint for fetching all cards to a customer
    @GetMapping("/customer/{customerId}/cards")
    @Operation(summary = "Fetch All Cards",
            description = "Fetches all cards from a customer ID")
    public ResponseEntity<List<CreateCardResponseDTO>> getCardsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(cardService.getByCustomer(customerId));
    }

    // GET: Endpoint for fetching card balance for a specific card
    @GetMapping("/{cardId}/balance")
    @Operation(summary = "Fetch Card Balance",
            description = "Fetches card balance for a specific card")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getTotalBalance(cardId));
    }
}