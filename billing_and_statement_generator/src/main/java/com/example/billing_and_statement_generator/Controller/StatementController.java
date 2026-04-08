package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.services.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/statements")
@RequiredArgsConstructor
@Tag(name = "Statement Controller", description = "Endpoints for generating and retrieving billing statements")
public class StatementController {

    private final StatementService statementService;

    @PostMapping("/v1/generate")
    @Operation(
            summary = "API Statement - Generate a statement (V1)",
            description = "Generate a billing statement for a specific card and billing cycle. " +
                    "Returns full statement details including transactions and available credit."
    )
    public ResponseEntity<RetrieveStatementResponseDTO> generateStatement(
            @Valid @RequestBody GenerateStatementRequestDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statementService.generateStatement(dto));
    }
}