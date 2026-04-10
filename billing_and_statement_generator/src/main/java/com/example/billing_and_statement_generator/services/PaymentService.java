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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final PaymentMapper paymentMapper;
    private final CardService cardService;

    // POST /payments/v1
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {

        UUID cardId = UUID.fromString(dto.getCardId());

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + dto.getCardId()));

        BigDecimal amountPaid = new BigDecimal(dto.getAmountPaid());

        // Server determines payment type
        BigDecimal totalOutstanding = card.getCardBalance().add(card.getCashAdvanceBalance());
        BigDecimal minimumDue = card.getMinimumDue() != null
                ? card.getMinimumDue() : BigDecimal.ZERO;

        // Attempt balance update BEFORE saving payment
        // cardService.applyPayment throws LimitExceededException on overpayment
        BigDecimal newTotalBalance;
        try {
            newTotalBalance = cardService.applyPayment(cardId, amountPaid);
        } catch (CardService.LimitExceededException e) {
            log.warn("Payment rejected for cardId={} — overpayment of {}: {}",
                    cardId, amountPaid, e.getMessage());
            throw new RuntimeException("Payment rejected: " + e.getMessage());
        }

        Payment.PaymentType paymentType = determinePaymentType(amountPaid, totalOutstanding, minimumDue);

        // Build payment entity
        Payment payment = paymentMapper.toEntity(dto, card);
        payment.setPaymentType(paymentType);
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);

        // Automatically assign to OPEN billing cycle if one exists for this card
        Optional<BillingCycle> openCycle = billingCycleRepository
                .findTopByCardCardIdOrderByCycleEndDateDesc(cardId);

        if (openCycle.isPresent() && "OPEN".equals(openCycle.get().getCycleStatus())) {
            payment.setBillingCycle(openCycle.get());
            log.info("Payment assigned to cycleId={}", openCycle.get().getCycleId());
        } else {
            log.info("No open billing cycle found for cardId={} — payment saved without cycleId", cardId);
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved: paymentId={}, cardId={}, amount={}, type={}, status={}, cycleId={}",
                savedPayment.getPaymentId(), cardId, amountPaid, paymentType,
                Payment.PaymentStatus.SUCCESS,
                savedPayment.getBillingCycle() != null
                        ? savedPayment.getBillingCycle().getCycleId() : "null");

        return paymentMapper.toResponseDTO(savedPayment);
    }

    /**
     * Determines payment type server-side based on amount vs outstanding/minimum.
     * FULL     - pays off the entire outstanding balance
     * MINIMUM  - pays at or near the minimum due (within $1 tolerance)
     * PARTIAL  - anything else
     */
    private Payment.PaymentType determinePaymentType(
            BigDecimal amountPaid,
            BigDecimal totalOutstanding,
            BigDecimal minimumDue) {

        if (amountPaid.compareTo(totalOutstanding) >= 0) {
            return Payment.PaymentType.FULL;
        }
        if (minimumDue.compareTo(BigDecimal.ZERO) > 0
                && amountPaid.subtract(minimumDue).abs().compareTo(BigDecimal.ONE) <= 0) {
            return Payment.PaymentType.MINIMUM;
        }
        return Payment.PaymentType.PARTIAL;
    }

    // GET /payments/v1/history
    @Transactional(readOnly = true)
    public List<RetrievePaymentHistoryDTO> getPaymentHistory(UUID cardId) {
        cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

        return paymentRepository.findByCardId(cardId)
                .stream()
                .map(paymentMapper::toHistoryDTO)
                .collect(Collectors.toList());
    }
}