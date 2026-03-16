package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.services.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // POST: Endpoint for creating new card
    @PostMapping
    public ResponseEntity<CreateCardResponseDTO> createCard(@Valid @RequestBody CreateCardRequestDTO dto) {
        CreateCardResponseDTO response = cardService.create(dto);
        return ResponseEntity.status(201).body(response);
    }

    // GET: Endpoint for finding information about a specific card
    @GetMapping("/{cardId}")
    public ResponseEntity<CreateCardResponseDTO> getCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getById(cardId));
    }

    // GET: Endpoint for fetching all cards to a customer
    @GetMapping("/customer/{customerId}/cards")
    public ResponseEntity<List<CreateCardResponseDTO>> getCardsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(cardService.getByCustomer(customerId));
    }

    // GET: Endpoint for fetching card balance for a specific card
    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getBalance(cardId));
    }
}