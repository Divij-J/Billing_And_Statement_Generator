package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.payment.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.payment.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.mapper.PaymentMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.StatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private CardRepository cardRepository;
    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private StatementRepository statementRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private CardService cardService;

    @InjectMocks
    private PaymentService paymentService;

    private UUID cardId;
    private UUID cycleId;
    private UUID paymentId;
    private Card testCard;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        testCard = Card.builder()
                .cardId(cardId)
                .cardBalance(BigDecimal.valueOf(1000))
                .cashAdvanceBalance(BigDecimal.valueOf(0))
                .minimumDue(BigDecimal.valueOf(100))
                .build();

        testPayment = Payment.builder()
                .paymentId(paymentId)
                .card(testCard)
                .billingCycle(BillingCycle.builder().cycleId(cycleId).build())
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();

    }

// processPayment() — happy path
    @Test
    void givenValidPartialPayment_whenProcessPaymentCalled_thenReturnsPaymentResponse() {
        PaymentRequestDTO dto = buildDto("500.00");

        PaymentResponseDTO expectedResponse = PaymentResponseDTO.builder()
                .paymentId(paymentId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("PARTIAL")
                .paymentStatus("SUCCESS")
                .paymentMethod("ONLINE")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardService.applyPayment(cardId, new BigDecimal("500.00")))
                .thenReturn(new BigDecimal("500.00"));
        when(paymentMapper.toEntity(any(), any())).thenReturn(testPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentMapper.toResponseDTO(testPayment)).thenReturn(expectedResponse);

        PaymentResponseDTO result = paymentService.processPayment(dto);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(result.getAmountPaid()).isEqualTo("500.00");

        verify(cardService).applyPayment(cardId, new BigDecimal("500.00"));
        verify(paymentRepository).save(any(Payment.class));
    }

    // Server determines payment type
    @Test
    void givenFullPaymentAmount_whenProcessPaymentCalled_thenPaymentTypeIsFull() {
        PaymentRequestDTO dto = buildDto("1000.00");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardService.applyPayment(cardId, new BigDecimal("1000.00")))
                .thenReturn(BigDecimal.ZERO);
        when(paymentMapper.toEntity(any(), any())).thenReturn(testPayment);
        when(paymentMapper.toResponseDTO(any())).thenReturn(
                PaymentResponseDTO.builder().paymentType("FULL").paymentStatus("SUCCESS").build());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(captor.capture())).thenReturn(testPayment);

        paymentService.processPayment(dto);

        assertThat(captor.getValue().getPaymentType()).isEqualTo(Payment.PaymentType.FULL);
    }

    @Test
    void givenMinimumDueAmount_whenProcessPaymentCalled_thenPaymentTypeIsMinimum() {
        PaymentRequestDTO dto = buildDto("100.00");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardService.applyPayment(cardId, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("900.00"));
        when(paymentMapper.toEntity(any(), any())).thenReturn(testPayment);
        when(paymentMapper.toResponseDTO(any())).thenReturn(
                PaymentResponseDTO.builder().paymentType("MINIMUM").paymentStatus("SUCCESS").build());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(captor.capture())).thenReturn(testPayment);

        paymentService.processPayment(dto);

        assertThat(captor.getValue().getPaymentType()).isEqualTo(Payment.PaymentType.MINIMUM);
    }

    //Overpayment - payment must NOT be saved
    @Test
    void givenOverpaymentAmount_whenProcessPaymentCalled_thenThrowsAndPaymentNotSaved() {
        PaymentRequestDTO dto = buildDto("9999.00");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardService.applyPayment(cardId, new BigDecimal("9999.00")))
                .thenThrow(new CardService.LimitExceededException("Payment amount exceeds current balance"));

        assertThrows(RuntimeException.class, () -> paymentService.processPayment(dto));

        verify(paymentRepository, never()).save(any());
    }


    // Error cases
    @Test
    void givenNonExistentCard_whenProcessPaymentCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(buildDto("500.00")));

        verify(paymentRepository, never()).save(any());
    }

    // getPaymentHistory()
    @Test
    void givenValidCardId_whenGetPaymentHistoryCalled_thenReturnsPaymentList() {
        RetrievePaymentHistoryDTO historyDTO = RetrievePaymentHistoryDTO.builder()
                .paymentId(paymentId.toString())
                .cardId(cardId.toString())
                .amountPaid("500.00")
                .paymentType("PARTIAL")
                .paymentStatus("SUCCESS")
                .paymentMethod("ONLINE")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(paymentRepository.findByCardId(cardId)).thenReturn(List.of(testPayment));
        when(paymentMapper.toHistoryDTO(testPayment)).thenReturn(historyDTO);

        List<RetrievePaymentHistoryDTO> result = paymentService.getPaymentHistory(cardId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCardId()).isEqualTo(cardId.toString());

        verify(cardRepository).findById(cardId);
        verify(paymentRepository).findByCardId(cardId);
    }

    @Test
    void givenNonExistentCard_whenGetPaymentHistoryCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentHistory(cardId));

        verify(paymentRepository, never()).findByCardId(any());
    }

    @Test
    void givenCardWithNoPayments_whenGetPaymentHistoryCalled_thenReturnsEmptyList() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(paymentRepository.findByCardId(cardId)).thenReturn(List.of());

        List<RetrievePaymentHistoryDTO> result = paymentService.getPaymentHistory(cardId);

        assertThat(result).isEmpty();
        verify(paymentRepository).findByCardId(cardId);
    }

    // Helpers

    private PaymentRequestDTO buildDto(String amount) {
        return PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid(amount)
                .paymentMethod("ONLINE")
                .build();
    }
}