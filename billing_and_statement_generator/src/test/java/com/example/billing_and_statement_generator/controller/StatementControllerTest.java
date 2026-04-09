package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.GetStatementRequestDTO;
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
    private GetStatementRequestDTO getStatementRequestDTO;
    private GenerateStatementResponseDTO generateResponseDTO;
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

        getStatementRequestDTO = GetStatementRequestDTO.builder()
                .statementId(statementId.toString())
                .build();

        generateResponseDTO = GenerateStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementStatus("GENERATED")
                .message("Statement generated successfully")
                .build();

        retrieveResponseDTO = RetrieveStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementDate(java.time.LocalDate.now().toString())
                .dueDate(java.time.LocalDate.now().plusDays(21).toString())
                .statementBalance("1020.00")
                .remainingStatementBalance("520.00")
                .minimumDue("100.00")
                .totalInterest("20.00")
                .totalOutstanding("1020.00")
                .totalFeeApplied("50.00")
                .cashAdvanceFee("20.00")
                .carryForwardBalance("520.00")
                .statementStatus("GENERATED")
                .availableCredit("4000.00")
                .amountPaid("500.00")
                .transactions(List.of())
                .payments(List.of())
                .build();
    }

// ── generateStatement() tests ───────────────────────────────────────────

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturns201() {
        when(statementService.generateStatement(any(GenerateStatementRequestDTO.class)))
                .thenReturn(generateResponseDTO);

        ResponseEntity<GenerateStatementResponseDTO> response =
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
                .thenReturn(generateResponseDTO);

        ResponseEntity<GenerateStatementResponseDTO> response =
                statementController.generateStatement(generateRequestDTO);

        assertThat(response.getBody().getCardId()).isEqualTo(cardId.toString());
        assertThat(response.getBody().getCycleId()).isEqualTo(cycleId.toString());
        assertThat(response.getBody().getStatementId()).isEqualTo(statementId.toString());

        verify(statementService, times(1)).generateStatement(any(GenerateStatementRequestDTO.class));
    }

// ── getStatement() tests ────────────────────────────────────────────────

    @Test
    void givenValidStatementId_whenGetStatementCalled_thenReturns200() {
        when(statementService.getStatement(statementId)).thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.getStatement(getStatementRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatementId()).isEqualTo(statementId.toString());

        verify(statementService).getStatement(statementId);
    }

    @Test
    void givenValidStatementId_whenGetStatementCalled_thenReturnsFullDetails() {
        when(statementService.getStatement(statementId)).thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.getStatement(getStatementRequestDTO);

        assertThat(response.getBody().getStatementBalance()).isEqualTo("1020.00");
        assertThat(response.getBody().getRemainingStatementBalance()).isEqualTo("520.00");
        assertThat(response.getBody().getAmountPaid()).isEqualTo("500.00");
        assertThat(response.getBody().getAvailableCredit()).isEqualTo("4000.00");
        assertThat(response.getBody().getTransactions()).isNotNull();
        assertThat(response.getBody().getPayments()).isNotNull();

        verify(statementService, times(1)).getStatement(statementId);
    }

    @Test
    void givenValidStatementId_whenGetStatementCalled_thenReturnsCorrectStatus() {
        when(statementService.getStatement(statementId)).thenReturn(retrieveResponseDTO);

        ResponseEntity<RetrieveStatementResponseDTO> response =
                statementController.getStatement(getStatementRequestDTO);

        assertThat(response.getBody().getStatementStatus()).isEqualTo("GENERATED");

        verify(statementService, times(1)).getStatement(statementId);
    }

}