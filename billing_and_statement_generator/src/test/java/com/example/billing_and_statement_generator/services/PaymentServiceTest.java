package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.v1.PaymentRequestV1DTO;
import com.example.billing_and_statement_generator.dto.v1.PaymentResponseV1DTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import com.example.billing_and_statement_generator.mapper.PaymentMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BillingCycleRepository billingCycleRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private CardService cardService;

    @InjectMocks
    private PaymentService paymentService;

    private UUID cardId;
    private UUID cycleId;
    private UUID paymentId;
    private Card testCard;
    private BillingCycle testBillingCycle;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        testCard = Card.builder()
                .cardId(cardId)
                .build();

        testBillingCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(testCard)
                .build();

        testPayment = Payment.builder()
                .paymentId(paymentId)
                .card(testCard)
                .billingCycle(testBillingCycle)
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.FULL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .build();
    }

    // ── processPayment() tests ──────────────────────────────────────

    @Test
    void givenValidPaymentRequest_whenProcessPaymentCalled_thenReturnsPaymentResponse() {
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .build();

        PaymentResponseDTO expectedResponse = PaymentResponseDTO.builder()
                .paymentId(paymentId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .paymentStatus("SUCCESS")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.of(testBillingCycle));
        when(paymentMapper.toEntity(any(), any(), any())).thenReturn(testPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentMapper.toResponseDTO(testPayment)).thenReturn(expectedResponse);
        when(cardService.applyPayment(any(UUID.class), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);

        PaymentResponseDTO result = paymentService.processPayment(dto);

        assertThat(result).isNotNull();
        assertThat(result.getCardId()).isEqualTo(cardId.toString());
        assertThat(result.getAmountPaid()).isEqualTo("500.00");
        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");

        verify(cardRepository).findById(cardId);
        verify(billingCycleRepository).findById(cycleId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void givenNonExistentCard_whenProcessPaymentCalled_thenThrowsException() {
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(dto));

        verify(cardRepository).findById(cardId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void givenNonExistentCycle_whenProcessPaymentCalled_thenThrowsException() {
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(dto));

        verify(billingCycleRepository).findById(cycleId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void givenCycleNotBelongingToCard_whenProcessPaymentCalled_thenThrowsException() {
        Card differentCard = Card.builder()
                .cardId(UUID.randomUUID())
                .build();

        BillingCycle cycleWithDifferentCard = BillingCycle.builder()
                .cycleId(cycleId)
                .card(differentCard)
                .build();

        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId))
                .thenReturn(Optional.of(cycleWithDifferentCard));

        assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(dto));

        verify(paymentRepository, never()).save(any());
    }

    // ── processPaymentV1() tests ────────────────────────────────────

    @Test
    void givenValidV1PaymentRequest_whenProcessPaymentV1Called_thenReturnsV1Response() {
        PaymentRequestV1DTO dto = PaymentRequestV1DTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .paymentMethod("ONLINE")
                .build();

        PaymentResponseV1DTO expectedResponse = PaymentResponseV1DTO.builder()
                .paymentId(paymentId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .paymentStatus("SUCCESS")
                .paymentMethod("ONLINE")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId))
                .thenReturn(Optional.of(testBillingCycle));
        when(paymentMapper.toEntityV1(any(), any(), any())).thenReturn(testPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentMapper.toResponseV1DTO(testPayment)).thenReturn(expectedResponse);
        when(cardService.applyPayment(any(UUID.class), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);

        PaymentResponseV1DTO result = paymentService.processPaymentV1(dto);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentMethod()).isEqualTo("ONLINE");
        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");

        verify(paymentRepository).save(any(Payment.class));
    }

    // ── getPaymentHistory() tests ───────────────────────────────────

    @Test
    void givenValidCardId_whenGetPaymentHistoryCalled_thenReturnsPaymentList() {
        RetrievePaymentHistoryDTO historyDTO = RetrievePaymentHistoryDTO.builder()
                .paymentId(paymentId.toString())
                .cardId(cardId.toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .paymentStatus("SUCCESS")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(paymentRepository.findByCardId(cardId)).thenReturn(List.of(testPayment));
        when(paymentMapper.toHistoryDTO(testPayment)).thenReturn(historyDTO);

        List<RetrievePaymentHistoryDTO> result =
                paymentService.getPaymentHistory(cardId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCardId()).isEqualTo(cardId.toString());

        verify(cardRepository).findById(cardId);
        verify(paymentRepository).findByCardId(cardId);
    }

    @Test
    void givenNonExistentCard_whenGetPaymentHistoryCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                paymentService.getPaymentHistory(cardId));

        verify(cardRepository).findById(cardId);
        verify(paymentRepository, never()).findByCardId(any());
    }

    @Test
    void givenCardWithNoPayments_whenGetPaymentHistoryCalled_thenReturnsEmptyList() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(paymentRepository.findByCardId(cardId)).thenReturn(List.of());

        List<RetrievePaymentHistoryDTO> result =
                paymentService.getPaymentHistory(cardId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(paymentRepository).findByCardId(cardId);
    }
}