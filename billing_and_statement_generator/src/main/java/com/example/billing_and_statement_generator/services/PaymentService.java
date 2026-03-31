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
    private final StatementRepository statementRepository;
    private final PaymentMapper paymentMapper;
    private final CardService cardService;

    // POST /payments/v1
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {

        // Validate card and billing cycle
        UUID cardId = UUID.fromString(dto.getCardId());
        UUID cycleId = UUID.fromString(dto.getCycleId());

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + dto.getCardId()));

        BillingCycle billingCycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Billing cycle not found: " + dto.getCycleId()));

        if (!billingCycle.getCard().getCardId().equals(card.getCardId())) {
            throw new RuntimeException("Billing cycle does not belong to this card");
        }

        BigDecimal amountPaid = new BigDecimal(dto.getAmountPaid());

        // Attempt balance update BEFORE saving payment
        // cardService.applyPayment throws LimitExceededException on overpayment.
        // We only save the payment if it succeeds — no ghost FAILED records.
        BigDecimal newTotalBalance;
        try {
            newTotalBalance = cardService.applyPayment(cardId, amountPaid);
        } catch (CardService.LimitExceededException e) {
            // Overpayment rejected - do NOT save a payment record
            log.warn("Payment rejected for cardId={} — overpayment of {}: {}",
                    cardId, amountPaid, e.getMessage());
            throw new RuntimeException("Payment rejected: " + e.getMessage());
        }

        // Server determines payment type
        BigDecimal totalOutstanding = billingCycle.getTotalOutstanding() != null
                ? billingCycle.getTotalOutstanding() : BigDecimal.ZERO;
        BigDecimal minimumDue = billingCycle.getMinimumDue() != null
                ? billingCycle.getMinimumDue() : BigDecimal.ZERO;

        Payment.PaymentType paymentType = determinePaymentType(amountPaid, totalOutstanding, minimumDue);

        // Build and save payment with server-determined type and SUCCESS status
        Payment payment = paymentMapper.toEntity(dto, card, billingCycle);
        payment.setPaymentType(paymentType);
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved: paymentId={}, cardId={}, amount={}, type={}, status={}",
                savedPayment.getPaymentId(), cardId, amountPaid, paymentType,
                Payment.PaymentStatus.SUCCESS);

        // Update BillingCycle totalOutstanding after payment
        BigDecimal updatedOutstanding = totalOutstanding.subtract(amountPaid)
                .max(BigDecimal.ZERO);
        billingCycle.setTotalOutstanding(updatedOutstanding);
        billingCycleRepository.save(billingCycle);
        log.debug("BillingCycle totalOutstanding updated: cycleId={}, old={}, new={}",
                cycleId, totalOutstanding, updatedOutstanding);

        // Update Statement remainingBalance, carryForward, and status if exists
        Optional<Statement> statementOpt = statementRepository.findByCycleId(cycleId);
        if (statementOpt.isPresent()) {
            Statement statement = statementOpt.get();

            BigDecimal newRemaining = statement.getRemainingStatementBalance()
                    .subtract(amountPaid)
                    .max(BigDecimal.ZERO);

            statement.setRemainingStatementBalance(newRemaining);
            statement.setCarryForwardBalance(newRemaining);

            // Flip to PAID if fully paid off
            if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
                statement.setStatementStatus(Statement.StatementStatus.PAID);
                log.info("Statement fully paid: statementId={}, cycleId={}",
                        statement.getStatementId(), cycleId);
            } else {
                statement.setStatementStatus(Statement.StatementStatus.UNPAID);
            }

            statementRepository.save(statement);
            log.debug("Statement updated: statementId={}, remainingBalance={}, status={}",
                    statement.getStatementId(), newRemaining, statement.getStatementStatus());
        }

        return paymentMapper.toResponseDTO(savedPayment);
    }

    /**
     * Determines payment type server-side based on amount vs outstanding/minimum.
     * FULL    - pays off the entire outstanding balance
     * MINIMUM - pays at or near the minimum due (within $1 tolerance)
     * PARTIAL - anything else
     */
    private Payment.PaymentType determinePaymentType(
            BigDecimal amountPaid,
            BigDecimal totalOutstanding,
            BigDecimal minimumDue) {

        if (amountPaid.compareTo(totalOutstanding) >= 0) {
            return Payment.PaymentType.FULL;
        }
        // Within $1.00 of minimum due counts as a minimum payment
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