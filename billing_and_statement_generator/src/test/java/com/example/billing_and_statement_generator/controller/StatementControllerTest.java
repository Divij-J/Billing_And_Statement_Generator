package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.services.StatementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementControllerTest {

    @Mock
    private StatementService statementService;

    @InjectMocks
    private com.example.billing_and_statement_generator.Controller.StatementController statementController;

    private UUID cardId;
    private UUID cycleId;
    private UUID statementId;
    private GenerateStatementRequestDTO generateRequestDTO;
    private RetrieveStatementResponseDTO retrieveResponseDTO;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();
        statementId = UUID.randomUUID();

        generateRequestDTO = GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();

        retrieveResponseDTO = RetrieveStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementDate(java.time.LocalDate.now().toString())
                .dueDate(java.time.LocalDate.now().plusDays(21).toString())
                .statementBalance("1020.00")
                .remainingStatementBalance("1020.00")
                .minimumDue("100.00")
                .totalInterest("20.00")
                .totalOutstanding("1020.00")
                .totalFeeApplied("50.00")
                .cashAdvanceFee("20.00")
                .carryForwardBalance("1020.00")
                .availableCredit("4000.00")
                .statementStatus("GENERATED")
                .transactions(List.of())
                .build();
    }

    // ── generateStatement() tests ───────────────────────────────────

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturns201() {
        when(statementService.generateStatement(any(GenerateStatementRequestDTO.class)))
                .thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.generateStatement(generateRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatementStatus()).isEqualTo("GENERATED");
        assertThat(response.getBody().getMessage()).isEqualTo("Statement generated successfully");

        verify(statementService).generateStatement(any(GenerateStatementRequestDTO.class));
    }

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturnsCorrectIds() {
        when(statementService.generateStatement(any(GenerateStatementRequestDTO.class)))
                .thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.generateStatement(generateRequestDTO);

        assertThat(response.getBody().getCardId()).isEqualTo(cardId.toString());
        assertThat(response.getBody().getCycleId()).isEqualTo(cycleId.toString());
        assertThat(response.getBody().getStatementId())
                .isEqualTo(statementId.toString());

        verify(statementService, times(1))
                .generateStatement(any(GenerateStatementRequestDTO.class));
    }

    // ── getStatement() tests ────────────────────────────────────────

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturnsAvailableCredit() {
        when(statementService.generateStatement(any(GenerateStatementRequestDTO.class)))
                .thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.generateStatement(generateRequestDTO);

        assertThat(response.getBody().getAvailableCredit()).isEqualTo("4000.00");

        verify(statementService).generateStatement(any(GenerateStatementRequestDTO.class));
    }

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturnsTransactionList() {
        when(statementService.generateStatement(any(GenerateStatementRequestDTO.class)))
                .thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.generateStatement(generateRequestDTO);

        assertThat(response.getBody().getTransactions()).isNotNull();
        verify(statementService, times(1)).generateStatement(any(GenerateStatementRequestDTO.class));
    }

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturnsCorrectBalance() {
        when(statementService.generateStatement(any(GenerateStatementRequestDTO.class)))
                .thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.generateStatement(generateRequestDTO);

        assertThat(response.getBody().getStatementBalance()).isEqualTo("1020.00");
        assertThat(response.getBody().getMinimumDue()).isEqualTo("100.00");
        assertThat(response.getBody().getTotalOutstanding()).isEqualTo("1020.00");

        verify(statementService, times(1)).generateStatement(any(GenerateStatementRequestDTO.class));
    }
}