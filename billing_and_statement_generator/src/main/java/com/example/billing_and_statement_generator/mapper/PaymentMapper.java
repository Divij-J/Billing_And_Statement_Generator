package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PaymentMapper {
    public Payment toEntity(
            PaymentRequestDTO dto,
            Card card,
            BillingCycle billingCycle
    ) {
        return Payment.builder()
                .paymentId(UUID.randomUUID())
                .card(card)
                .billingCycle(billingCycle)
                .amountPaid(new BigDecimal(dto.getAmountPaid()))
                .paymentDate(LocalDateTime.now())
                .paymentType(parsePaymentType(dto.getPaymentType()))
                .paymentStatus(Payment.PaymentStatus.PENDING)
                .build();
    }
    private Payment.PaymentType parsePaymentType(String type) {
        try {
            return Payment.PaymentType.valueOf(type.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid payment type: " + type + ". Valid values are: MINIMUM, FULL, PARTIAL");
        }
    }
    public PaymentResponseDTO toResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getPaymentId().toString())
                .cycleId(payment.getBillingCycle().getCycleId().toString())
                .cardId(payment.getCard().getCardId().toString())
                .amountPaid(payment.getAmountPaid().toString())
                .paymentDate(payment.getPaymentDate().toString())
                .paymentType(payment.getPaymentType().toString())
                .paymentStatus(payment.getPaymentStatus().toString())
                .build();
    }
    public RetrievePaymentHistoryDTO toHistoryDTO(Payment payment) {
        return RetrievePaymentHistoryDTO.builder()
                .paymentId(payment.getPaymentId().toString())
                .cycleId(payment.getBillingCycle().getCycleId().toString())
                .cardId(payment.getCard().getCardId().toString())
                .amountPaid(payment.getAmountPaid().toString())
                .paymentDate(payment.getPaymentDate().toString())
                .paymentType(payment.getPaymentType().toString())
                .paymentStatus(payment.getPaymentStatus().toString())
                .build();
    }
}