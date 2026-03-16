package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.service.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateStatementResponseDTO> generateStatement(
            @Valid @RequestBody GenerateStatementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statementService.generateStatement(dto));
    }

    @GetMapping("/{cardId}/{cycleId}")
    public ResponseEntity<RetrieveStatementResponseDTO> getStatement(
            @PathVariable UUID cardId,
            @PathVariable UUID cycleId) {
        return ResponseEntity.ok(statementService.getStatement(cardId, cycleId));
    }
}