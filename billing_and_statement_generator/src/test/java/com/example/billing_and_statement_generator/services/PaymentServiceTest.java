package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.payment.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.payment.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
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

    @Mock private PaymentRepository paymentRepository;
    @Mock private CardRepository cardRepository;
    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private CardService cardService;

    @InjectMocks
    private PaymentService paymentService;

    private UUID cardId;
    private UUID cycleId;
    private Card testCard;
    private BillingCycle openCycle;
    private PaymentRequestDTO requestDTO;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();

        testCard = Card.builder()
                .cardId(cardId)
                .cardBalance(new BigDecimal("1000.00"))
                .cashAdvanceBalance(BigDecimal.ZERO)
                .minimumDue(new BigDecimal("100.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .build();

        openCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(testCard)
                .cycleStatus("OPEN")
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalOutstanding(new BigDecimal("1000.00"))
                .build();

        requestDTO = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("500.00")
                .paymentMethod("ONLINE")
                .build();

        testPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(testCard)
                .billingCycle(openCycle)
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();
    }

    // processPayment() tests

    @Test
    void givenValidPayment_withOpenCycle_whenProcessPaymentCalled_thenAssignsCycleId() {
        PaymentResponseDTO expectedResponse = PaymentResponseDTO.builder()
                .paymentId(testPayment.getPaymentId().toString())
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
        when(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .thenReturn(Optional.of(openCycle));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentMapper.toResponseDTO(testPayment)).thenReturn(expectedResponse);

        PaymentResponseDTO result = paymentService.processPayment(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getCycleId()).isEqualTo(cycleId.toString());
        assertThat(result.getPaymentType()).isEqualTo("PARTIAL");
        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");

        verify(billingCycleRepository).findTopByCardCardIdOrderByCycleEndDateDesc(cardId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void givenValidPayment_withNoCycle_whenProcessPaymentCalled_thenSavesWithoutCycleId() {
        Payment paymentNoCycle = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(testCard)
                .billingCycle(null)
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();

        PaymentResponseDTO expectedResponse = PaymentResponseDTO.builder()
                .paymentId(paymentNoCycle.getPaymentId().toString())
                .cardId(cardId.toString())
                .cycleId(null)
                .amountPaid("500.00")
                .paymentType("PARTIAL")
                .paymentStatus("SUCCESS")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardService.applyPayment(cardId, new BigDecimal("500.00")))
                .thenReturn(new BigDecimal("500.00"));
        when(paymentMapper.toEntity(any(), any())).thenReturn(paymentNoCycle);
        when(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(paymentNoCycle);
        when(paymentMapper.toResponseDTO(paymentNoCycle)).thenReturn(expectedResponse);

        PaymentResponseDTO result = paymentService.processPayment(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getCycleId()).isNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void givenPayment_withClosedCycle_whenProcessPaymentCalled_thenSavesWithoutCycleId() {
        BillingCycle closedCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(testCard)
                .cycleStatus("CLOSED")
                .build();

        Payment paymentNoCycle = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(testCard)
                .billingCycle(null)
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .build();

        PaymentResponseDTO expectedResponse = PaymentResponseDTO.builder()
                .paymentId(paymentNoCycle.getPaymentId().toString())
                .cardId(cardId.toString())
                .cycleId(null)
                .amountPaid("500.00")
                .paymentType("PARTIAL")
                .paymentStatus("SUCCESS")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardService.applyPayment(cardId, new BigDecimal("500.00")))
                .thenReturn(new BigDecimal("500.00"));
        when(paymentMapper.toEntity(any(), any())).thenReturn(paymentNoCycle);
        when(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .thenReturn(Optional.of(closedCycle));
        when(paymentRepository.save(any(Payment.class))).thenReturn(paymentNoCycle);
        when(paymentMapper.toResponseDTO(paymentNoCycle)).thenReturn(expectedResponse);

        PaymentResponseDTO result = paymentService.processPayment(requestDTO);

        assertThat(result.getCycleId()).isNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void givenOverpayment_whenProcessPaymentCalled_thenThrowsAndDoesNotSave() {
        PaymentRequestDTO overpaymentDto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("9999.00")
                .paymentMethod("ONLINE")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardService.applyPayment(cardId, new BigDecimal("9999.00")))
                .thenThrow(new CardService.LimitExceededException("Payment amount exceeds current balance"));

        assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(overpaymentDto));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void givenNonExistentCard_whenProcessPaymentCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(requestDTO));

        verify(paymentRepository, never()).save(any());
    }

    // getPaymentHistory() tests

    @Test
    void givenValidCardId_whenGetPaymentHistoryCalled_thenReturnsPayments() {
        RetrievePaymentHistoryDTO historyDTO = RetrievePaymentHistoryDTO.builder()
                .paymentId(testPayment.getPaymentId().toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .amountPaid("500.00")
                .paymentType("PARTIAL")
                .paymentStatus("SUCCESS")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(paymentRepository.findByCardId(cardId)).thenReturn(List.of(testPayment));
        when(paymentMapper.toHistoryDTO(testPayment)).thenReturn(historyDTO);

        List<RetrievePaymentHistoryDTO> result = paymentService.getPaymentHistory(cardId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmountPaid()).isEqualTo("500.00");
        assertThat(result.get(0).getPaymentType()).isEqualTo("PARTIAL");

        verify(paymentRepository).findByCardId(cardId);
    }

    @Test
    void givenNonExistentCard_whenGetPaymentHistoryCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentHistory(cardId));

        verify(paymentRepository, never()).findByCardId(any());
    }
}