package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.services.PaymentService;
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
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private com.example.billing_and_statement_generator.Controller.PaymentController paymentController;

    private UUID cardId;
    private UUID cycleId;
    private UUID paymentId;
    private PaymentRequestDTO paymentRequestDTO;
    private PaymentResponseDTO paymentResponseDTO;
    private RetrievePaymentHistoryDTO retrievePaymentHistoryDTO;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        paymentRequestDTO = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .paymentMethod("ONLINE")
                .build();

        paymentResponseDTO = PaymentResponseDTO.builder()
                .paymentId(paymentId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .paymentStatus("SUCCESS")
                .paymentMethod("ONLINE")
                .build();

        retrievePaymentHistoryDTO = RetrievePaymentHistoryDTO.builder()
                .paymentId(paymentId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .paymentStatus("SUCCESS")
                .paymentMethod("ONLINE")
                .build();
    }

    //processPayment() tests

    @Test
    void givenValidPaymentRequest_whenProcessPaymentCalled_thenReturns201() {
        when(paymentService.processPayment(any(PaymentRequestDTO.class)))
                .thenReturn(paymentResponseDTO);

        ResponseEntity<PaymentResponseDTO> response =
                paymentController.processPayment(paymentRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCardId()).isEqualTo(cardId.toString());
        assertThat(response.getBody().getAmountPaid()).isEqualTo("500.00");
        assertThat(response.getBody().getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(response.getBody().getPaymentMethod()).isEqualTo("ONLINE");

        verify(paymentService).processPayment(any(PaymentRequestDTO.class));
    }

    @Test
    void givenValidPaymentRequest_whenProcessPaymentCalled_thenReturnsCorrectPaymentType() {
        when(paymentService.processPayment(any(PaymentRequestDTO.class)))
                .thenReturn(paymentResponseDTO);

        ResponseEntity<PaymentResponseDTO> response =
                paymentController.processPayment(paymentRequestDTO);

        assertThat(response.getBody().getPaymentType()).isEqualTo("FULL");
        assertThat(response.getBody().getPaymentMethod()).isEqualTo("ONLINE");
        verify(paymentService, times(1))
                .processPayment(any(PaymentRequestDTO.class));
    }

    @Test
    void givenValidPaymentRequest_whenProcessPaymentCalled_thenReturnsCorrectPaymentMethod() {
        when(paymentService.processPayment(any(PaymentRequestDTO.class)))
                .thenReturn(paymentResponseDTO);

        ResponseEntity<PaymentResponseDTO> response =
                paymentController.processPayment(paymentRequestDTO);

        assertThat(response.getBody().getPaymentMethod()).isEqualTo("ONLINE");
        verify(paymentService, times(1))
                .processPayment(any(PaymentRequestDTO.class));
    }

    //getPaymentHistory() tests

    @Test
    void givenValidCardId_whenGetPaymentHistoryCalled_thenReturns200() {
        when(paymentService.getPaymentHistory(cardId))
                .thenReturn(List.of(retrievePaymentHistoryDTO));

        ResponseEntity<List<RetrievePaymentHistoryDTO>> response =
                paymentController.getPaymentHistory(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCardId())
                .isEqualTo(cardId.toString());
        assertThat(response.getBody().get(0).getPaymentMethod())
                .isEqualTo("ONLINE");

        verify(paymentService).getPaymentHistory(cardId);
    }

    @Test
    void givenCardWithNoPayments_whenGetPaymentHistoryCalled_thenReturnsEmptyList() {
        when(paymentService.getPaymentHistory(cardId))
                .thenReturn(List.of());

        ResponseEntity<List<RetrievePaymentHistoryDTO>> response =
                paymentController.getPaymentHistory(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();

        verify(paymentService).getPaymentHistory(cardId);
    }
}
