package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import com.example.billing_and_statement_generator.mapper.PaymentMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.dto.v1.PaymentRequestV1DTO;
import com.example.billing_and_statement_generator.dto.v1.PaymentResponseV1DTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            CardRepository cardRepository,
            BillingCycleRepository billingCycleRepository,
            PaymentMapper paymentMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.cardRepository = cardRepository;
        this.billingCycleRepository = billingCycleRepository;
        this.paymentMapper = paymentMapper;
    }

    // POST /payments
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {
        Card card = cardRepository.findById(UUID.fromString(dto.getCardId()))
                .orElseThrow(() -> new RuntimeException("Card not found: " + dto.getCardId()));

        BillingCycle billingCycle = billingCycleRepository.findById(UUID.fromString(dto.getCycleId()))
                .orElseThrow(() -> new RuntimeException("Billing cycle not found: " + dto.getCycleId()));

        if (!billingCycle.getCard().getCardId().equals(card.getCardId())) {
            throw new RuntimeException("Billing cycle does not belong to this card");
        }

        Payment payment = paymentMapper.toEntity(dto, card, billingCycle);
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDTO(savedPayment);
    }

    // GET /payments/{card_id}
    public List<RetrievePaymentHistoryDTO> getPaymentHistory(UUID cardId) {
        cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

        return paymentRepository.findByCardId(cardId)
                .stream()
                .map(paymentMapper::toHistoryDTO)
                .collect(Collectors.toList());
    }

    public PaymentResponseV1DTO processPaymentV1(PaymentRequestV1DTO dto) {
        Card card = cardRepository.findById(UUID.fromString(dto.getCardId()))
                .orElseThrow(() -> new RuntimeException("Card not found: " + dto.getCardId()));

        BillingCycle billingCycle = billingCycleRepository.findById(UUID.fromString(dto.getCycleId()))
                .orElseThrow(() -> new RuntimeException("Billing cycle not found: " + dto.getCycleId()));

        if (!billingCycle.getCard().getCardId().equals(card.getCardId())) {
            throw new RuntimeException("Billing cycle does not belong to this card");
        }

        Payment payment = paymentMapper.toEntityV1(dto, card, billingCycle);
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseV1DTO(savedPayment);
    }
}