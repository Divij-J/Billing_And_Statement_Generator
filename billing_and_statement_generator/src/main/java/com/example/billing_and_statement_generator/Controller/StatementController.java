package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.services.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/statements")
@RequiredArgsConstructor
@Tag(name = "Statement Controller", description = "Endpoints for generating and retrieving billing statements")
public class StatementController {

    private final StatementService statementService;

    @PostMapping("/v1/generate")
    @Operation(summary = "Generate a statement (V1)",
            description = "Generate a billing statement for a specific card and billing cycle")
    public ResponseEntity<GenerateStatementResponseDTO> generateStatement(
            @Valid @RequestBody GenerateStatementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statementService.generateStatement(dto));
    }

//    @GetMapping("/v1/{cardId}/{cycleId}")
//    @Operation(summary = "Get a statement (V1)",
//            description = "Retrieve a billing statement for a specific card and billing cycle")
//    public ResponseEntity<RetrieveStatementResponseDTO> getStatement(
//            @PathVariable UUID cardId,
//            @PathVariable UUID cycleId) {
//        return ResponseEntity.ok(statementService.getStatement(cardId, cycleId));
//    }

    @PostMapping("/v1/get")
    @Operation(summary = "Get a statement (V1)",
            description = "Retrieve a billing statement for a specific card and billing cycle")
    public ResponseEntity<RetrieveStatementResponseDTO> getStatement(
            @Valid @RequestBody GenerateStatementRequestDTO dto) {
        return ResponseEntity.ok(statementService.getStatement(
                UUID.fromString(dto.getCardId()),
                UUID.fromString(dto.getCycleId())));
    }
}