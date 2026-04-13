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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentServiceBDDTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    CardRepository cardRepository;

    @Mock
    BillingCycleRepository billingCycleRepository;

    @Spy
    PaymentMapper paymentMapper = new PaymentMapper();

    @Mock
    CardService cardService;

    @InjectMocks
    PaymentService paymentService;

    private UUID cardId;
    private UUID cycleId;
    private Card card;
    private BillingCycle openCycle;

// GIVEN
// WHEN
// THEN

    @BeforeEach
    void setup() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();

        card = Card.builder()
                .cardId(cardId)
                .cardBalance(new BigDecimal("1000.00"))
                .cashAdvanceBalance(BigDecimal.ZERO)
                .minimumDue(new BigDecimal("100.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .availableCredit(new BigDecimal("4000.00"))
                .annualInterestRate(new BigDecimal("0.20"))
                .cashAdvanceAPR(new BigDecimal("0.24"))
                .cashAdvanceFeeRate(new BigDecimal("0.02"))
                .lateFeeAmount(new BigDecimal("50.00"))
                .securityCode("123")
                .build();

        openCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(card)
                .cycleStatus("OPEN")
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalOutstanding(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void shouldProcessPartialPaymentSuccessfully() {
        // GIVEN
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("500.00")
                .paymentMethod("ONLINE")
                .build();

        Payment savedPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(card)
                .billingCycle(openCycle)
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();

        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.of(card));
        given(cardService.applyPayment(any(UUID.class), any(BigDecimal.class))).willReturn(new BigDecimal("500.00"));
        given(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(any(UUID.class)))
                .willReturn(Optional.of(openCycle));
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // WHEN
        PaymentResponseDTO result = paymentService.processPayment(dto);

        // THEN
        assertEquals("PARTIAL", result.getPaymentType());
        assertEquals("SUCCESS", result.getPaymentStatus());
        then(paymentRepository).should().save(any(Payment.class));
    }

    @Test
    void shouldProcessMinimumPaymentSuccessfully() {
        // GIVEN
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("100.00")
                .paymentMethod("ONLINE")
                .build();

        Payment savedPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(card)
                .billingCycle(openCycle)
                .amountPaid(new BigDecimal("100.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.MINIMUM)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();

        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.of(card));
        given(cardService.applyPayment(any(UUID.class), any(BigDecimal.class))).willReturn(new BigDecimal("900.00"));
        given(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(any(UUID.class)))
                .willReturn(Optional.of(openCycle));
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // WHEN
        PaymentResponseDTO result = paymentService.processPayment(dto);

        // THEN
        assertEquals("MINIMUM", result.getPaymentType());
        assertEquals("SUCCESS", result.getPaymentStatus());
        then(paymentRepository).should().save(any(Payment.class));
    }

    @Test
    void shouldProcessFullPaymentSuccessfully() {
        // GIVEN
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("1000.00")
                .paymentMethod("ONLINE")
                .build();

        Payment savedPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(card)
                .billingCycle(openCycle)
                .amountPaid(new BigDecimal("1000.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.FULL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();

        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.of(card));
        given(cardService.applyPayment(any(UUID.class), any(BigDecimal.class))).willReturn(BigDecimal.ZERO);
        given(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(any(UUID.class)))
                .willReturn(Optional.of(openCycle));
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // WHEN
        PaymentResponseDTO result = paymentService.processPayment(dto);

        // THEN
        assertEquals("FULL", result.getPaymentType());
        assertEquals("SUCCESS", result.getPaymentStatus());
        then(paymentRepository).should().save(any(Payment.class));
    }

    @Test
    void shouldSavePaymentWithoutCycleWhenNoCycleExists() {
        // GIVEN
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("500.00")
                .paymentMethod("ONLINE")
                .build();

        Payment savedPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(card)
                .billingCycle(null)
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();

        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.of(card));
        given(cardService.applyPayment(any(UUID.class), any(BigDecimal.class))).willReturn(new BigDecimal("500.00"));
        given(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(any(UUID.class)))
                .willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // WHEN
        PaymentResponseDTO result = paymentService.processPayment(dto);

        // THEN
        assertNull(result.getCycleId());
        then(paymentRepository).should().save(any(Payment.class));
    }

    @Test
    void shouldRejectOverpaymentAndNotSavePayment() {
        // GIVEN
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("9999.00")
                .paymentMethod("ONLINE")
                .build();

        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.of(card));
        given(cardService.applyPayment(any(UUID.class), any(BigDecimal.class)))
                .willThrow(new CardService.LimitExceededException("Payment amount exceeds current balance"));

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> paymentService.processPayment(dto));

        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    void shouldThrowExceptionWhenCardNotFoundForPayment() {
        // GIVEN
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(cardId.toString())
                .amountPaid("500.00")
                .paymentMethod("ONLINE")
                .build();

        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> paymentService.processPayment(dto));

        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    void shouldReturnPaymentHistoryForCard() {
        // GIVEN
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(card)
                .billingCycle(openCycle)
                .amountPaid(new BigDecimal("500.00"))
                .paymentDate(LocalDateTime.now())
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .build();

        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.of(card));
        given(paymentRepository.findByCardId(any(UUID.class))).willReturn(List.of(payment));

        // WHEN
        List<RetrievePaymentHistoryDTO> result = paymentService.getPaymentHistory(cardId);

        // THEN
        assertEquals(1, result.size());
        assertEquals("500.00", result.get(0).getAmountPaid());
        assertEquals("PARTIAL", result.get(0).getPaymentType());
        then(paymentRepository).should().findByCardId(any(UUID.class));
    }

    @Test
    void shouldThrowExceptionWhenCardNotFoundForHistory() {
        // GIVEN
        given(cardRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> paymentService.getPaymentHistory(cardId));

        then(paymentRepository).should(never()).findByCardId(any());
    }

    /*
     * Other tests to consider:
     * - shouldApplyPaymentToCashAdvanceFirst
     * - shouldHandleNullMinimumDue
     * - shouldReturnEmptyHistoryForCardWithNoPayments
     */

}

