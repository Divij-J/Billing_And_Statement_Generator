package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CustomerIdDTO;
import com.example.billing_and_statement_generator.dto.card.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.card.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.dto.card.CardIdDTO;
import com.example.billing_and_statement_generator.dto.card.GetCardBalanceResponseDTO;
import com.example.billing_and_statement_generator.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/cards")
@RequiredArgsConstructor
@Tag(name = "Card Controller", description = "Endpoints for handling card operations")
public class CardController {

    private final CardService cardService;

    // POST: Endpoint for creating new card (v1)
    @PostMapping("/v1/createCard")
    @Operation(summary = "Create a card (V1)",
            description = "Create a card with given information for a customer")
    public ResponseEntity<CreateCardResponseDTO> createCardV1(@Valid @RequestBody CreateCardRequestDTO dto) {
        CreateCardResponseDTO response = cardService.create(dto);
        return ResponseEntity.status(201).body(response);
    }

    // POST: Endpoint for finding information about a specific card
    @PostMapping("/v1/getCardInfo")
    @Operation(summary = "Card Information (V1)",
            description = "Fetch card information from a card ID")
    public ResponseEntity<CreateCardResponseDTO> getCard(@Valid @RequestBody CardIdDTO dto) {
        CreateCardResponseDTO response = cardService.getById(dto.getCardId());
        return ResponseEntity.ok(response);
    }

    // POST: Endpoint for fetching all cards to a customer
    @PostMapping("/v1/getCardsByCustomerId")
    @Operation(summary = "Fetch All Cards By Customer ID (V1)",
            description = "Fetches all cards from a customer ID")
    public ResponseEntity<List<CreateCardResponseDTO>> getCardsByCustomer(@Valid @RequestBody CustomerIdDTO dto) {
        return ResponseEntity.ok(cardService.getByCustomer(dto.getCustomerId()));
    }

    // POST: Endpoint for fetching card balance for a specific card
    @PostMapping("/v1/getCardBalanceByCardId")
    @Operation(summary = "Fetch Card Balance by Card ID (V1)",
            description = "Fetches card balance for a specific card")
    public ResponseEntity<GetCardBalanceResponseDTO> getBalance(@Valid @RequestBody CardIdDTO dto) {
        return ResponseEntity.ok(cardService.getBalances(dto.getCardId()));
    }
}