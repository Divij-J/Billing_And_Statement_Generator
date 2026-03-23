package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.v1.PaymentRequestV1DTO;
import com.example.billing_and_statement_generator.dto.v1.PaymentResponseV1DTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PaymentMapperTest {

    private PaymentMapper paymentMapper;
    private Card testCard;
    private BillingCycle testBillingCycle;

    @BeforeEach
    void setUp() {
        paymentMapper = new PaymentMapper();

        testCard = Card.builder()
                .cardId(UUID.randomUUID())
                .build();

        testBillingCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(testCard)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .build();
    }

    // ── toEntity() tests ────────────────────────────────────────────

    @Test
    void givenValidPaymentRequestDTO_whenToEntityCalled_thenReturnsPaymentEntity() {
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cycleId(testBillingCycle.getCycleId().toString())
                .cardId(testCard.getCardId().toString())
                .amountPaid("500.00")
                .paymentType("FULL")
                .build();

        Payment result = paymentMapper.toEntity(dto, testCard, testBillingCycle);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isNotNull();
        assertThat(result.getCard()).isEqualTo(testCard);
        assertThat(result.getBillingCycle()).isEqualTo(testBillingCycle);
        assertThat(result.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.getPaymentType()).isEqualTo(Payment.PaymentType.FULL);
        assertThat(result.getPaymentStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(result.getPaymentDate()).isNotNull();
    }

    @Test
    void givenMinimumPaymentType_whenToEntityCalled_thenReturnsCorrectPaymentType() {
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cycleId(testBillingCycle.getCycleId().toString())
                .cardId(testCard.getCardId().toString())
                .amountPaid("100.00")
                .paymentType("MINIMUM")
                .build();

        Payment result = paymentMapper.toEntity(dto, testCard, testBillingCycle);

        assertThat(result.getPaymentType()).isEqualTo(Payment.PaymentType.MINIMUM);
    }

    @Test
    void givenInvalidPaymentType_whenToEntityCalled_thenThrowsException() {
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cycleId(testBillingCycle.getCycleId().toString())
                .cardId(testCard.getCardId().toString())
                .amountPaid("100.00")
                .paymentType("INVALID_TYPE")
                .build();

        assertThrows(RuntimeException.class,
                () -> paymentMapper.toEntity(dto, testCard, testBillingCycle));
    }

    // ── toEntityV1() tests ──────────────────────────────────────────

    @Test
    void givenValidPaymentRequestV1DTO_whenToEntityV1Called_thenReturnsPaymentEntity() {
        PaymentRequestV1DTO dto = PaymentRequestV1DTO.builder()
                .cycleId(testBillingCycle.getCycleId().toString())
                .cardId(testCard.getCardId().toString())
                .amountPaid("750.00")
                .paymentType("PARTIAL")
                .paymentMethod("BANK_TRANSFER")
                .build();

        Payment result = paymentMapper.toEntityV1(dto, testCard, testBillingCycle);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isNotNull();
        assertThat(result.getAmountPaid()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(result.getPaymentType()).isEqualTo(Payment.PaymentType.PARTIAL);
        assertThat(result.getPaymentMethod()).isEqualTo(Payment.PaymentMethod.BANK_TRANSFER);
    }

    @Test
    void givenNullPaymentMethod_whenToEntityV1Called_thenPaymentMethodIsNull() {
        PaymentRequestV1DTO dto = PaymentRequestV1DTO.builder()
                .cycleId(testBillingCycle.getCycleId().toString())
                .cardId(testCard.getCardId().toString())
                .amountPaid("200.00")
                .paymentType("FULL")
                .paymentMethod(null)
                .build();

        Payment result = paymentMapper.toEntityV1(dto, testCard, testBillingCycle);

        assertThat(result.getPaymentMethod()).isNull();
    }

    @Test
    void givenInvalidPaymentMethod_whenToEntityV1Called_thenThrowsException() {
        PaymentRequestV1DTO dto = PaymentRequestV1DTO.builder()
                .cycleId(testBillingCycle.getCycleId().toString())
                .cardId(testCard.getCardId().toString())
                .amountPaid("200.00")
                .paymentType("FULL")
                .paymentMethod("CRYPTO")
                .build();

        assertThrows(RuntimeException.class,
                () -> paymentMapper.toEntityV1(dto, testCard, testBillingCycle));
    }

    // ── toResponseDTO() tests ───────────────────────────────────────

    @Test
    void givenPaymentEntity_whenToResponseDTOCalled_thenReturnsCorrectDTO() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .card(testCard)
                .billingCycle(testBillingCycle)
                .amountPaid(new BigDecimal("500.00"))
                .paymentType(Payment.PaymentType.FULL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentDate(java.time.LocalDateTime.now())
                .build();

        PaymentResponseDTO result = paymentMapper.toResponseDTO(payment);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(paymentId.toString());
        assertThat(result.getCardId()).isEqualTo(testCard.getCardId().toString());
        assertThat(result.getCycleId()).isEqualTo(testBillingCycle.getCycleId().toString());
        assertThat(result.getAmountPaid()).isEqualTo("500.00");
        assertThat(result.getPaymentType()).isEqualTo("FULL");
        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
    }

    // ── toResponseV1DTO() tests ─────────────────────────────────────

    @Test
    void givenPaymentEntityWithPaymentMethod_whenToResponseV1DTOCalled_thenReturnsCorrectDTO() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .card(testCard)
                .billingCycle(testBillingCycle)
                .amountPaid(new BigDecimal("750.00"))
                .paymentType(Payment.PaymentType.PARTIAL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(Payment.PaymentMethod.ONLINE)
                .paymentDate(java.time.LocalDateTime.now())
                .build();

        PaymentResponseV1DTO result = paymentMapper.toResponseV1DTO(payment);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(paymentId.toString());
        assertThat(result.getAmountPaid()).isEqualTo("750.00");
        assertThat(result.getPaymentMethod()).isEqualTo("ONLINE");
    }

    @Test
    void givenPaymentEntityWithNullPaymentMethod_whenToResponseV1DTOCalled_thenPaymentMethodIsNull() {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(testCard)
                .billingCycle(testBillingCycle)
                .amountPaid(new BigDecimal("500.00"))
                .paymentType(Payment.PaymentType.FULL)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentMethod(null)
                .paymentDate(java.time.LocalDateTime.now())
                .build();

        PaymentResponseV1DTO result = paymentMapper.toResponseV1DTO(payment);

        assertThat(result.getPaymentMethod()).isNull();
    }

    // ── toHistoryDTO() tests ────────────────────────────────────────

    @Test
    void givenPaymentEntity_whenToHistoryDTOCalled_thenReturnsCorrectHistoryDTO() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .card(testCard)
                .billingCycle(testBillingCycle)
                .amountPaid(new BigDecimal("300.00"))
                .paymentType(Payment.PaymentType.MINIMUM)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .paymentDate(java.time.LocalDateTime.now())
                .build();

        RetrievePaymentHistoryDTO result = paymentMapper.toHistoryDTO(payment);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(paymentId.toString());
        assertThat(result.getCardId()).isEqualTo(testCard.getCardId().toString());
        assertThat(result.getCycleId()).isEqualTo(testBillingCycle.getCycleId().toString());
        assertThat(result.getAmountPaid()).isEqualTo("300.00");
        assertThat(result.getPaymentType()).isEqualTo("MINIMUM");
        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
    }
}