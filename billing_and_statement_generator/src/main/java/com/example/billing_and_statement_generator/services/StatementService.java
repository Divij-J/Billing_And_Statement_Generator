package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.mapper.StatementMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.StatementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class StatementService {

    private final StatementRepository statementRepository;
    private final CardRepository cardRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final PaymentRepository paymentRepository;
    private final StatementMapper statementMapper;

    public StatementService(
            StatementRepository statementRepository,
            CardRepository cardRepository,
            BillingCycleRepository billingCycleRepository,
            PaymentRepository paymentRepository,
            StatementMapper statementMapper
    ) {
        this.statementRepository = statementRepository;
        this.cardRepository = cardRepository;
        this.billingCycleRepository = billingCycleRepository;
        this.paymentRepository = paymentRepository;
        this.statementMapper = statementMapper;
    }

    // POST /billing/generate/{card_id}
    public GenerateStatementResponseDTO generateStatement(GenerateStatementRequestDTO dto) {

        // Validate card exists
        Card card = cardRepository.findById(UUID.fromString(dto.getCardId()))
                .orElseThrow(() -> new RuntimeException("Card not found: " + dto.getCardId()));

        // Validate billing cycle exists
        BillingCycle billingCycle = billingCycleRepository.findById(UUID.fromString(dto.getCycleId()))
                .orElseThrow(() -> new RuntimeException("Billing cycle not found: " + dto.getCycleId()));

        // Validate billing cycle belongs to card
        if (!billingCycle.getCard().getCardId().equals(card.getCardId())) {
            throw new RuntimeException("Billing cycle does not belong to this card");
        }

        // Check if statement already exists for this cycle
        if (statementRepository.existsByCycleId(UUID.fromString(dto.getCycleId()))) {
            throw new RuntimeException("Statement already exists for this billing cycle");
        }

        // Calculate balances
        BigDecimal totalPurchases = billingCycle.getTotalPurchases();
        BigDecimal totalCashAdvance = billingCycle.getTotalCashAdvance();
        BigDecimal totalInterest = billingCycle.getTotalInterest();
        BigDecimal totalOutstanding = billingCycle.getTotalOutstanding();
        BigDecimal minimumDue = billingCycle.getMinimumDue();

        // Calculate fees (null-safety)
        BigDecimal cashAdvanceFeeRate = card.getCashAdvanceFeeRate() == null ? BigDecimal.ZERO : card.getCashAdvanceFeeRate();
        BigDecimal lateFeeAmount = card.getLateFeeAmount() == null ? BigDecimal.ZERO : card.getLateFeeAmount();

        BigDecimal cashAdvanceFee = (totalCashAdvance == null ? BigDecimal.ZERO : totalCashAdvance)
                .multiply(cashAdvanceFeeRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalFeeApplied = cashAdvanceFee
                .add(lateFeeAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // Payments so far (null-safe)
        BigDecimal totalPaid = paymentRepository.findTotalPaidByCycleId(UUID.fromString(dto.getCycleId()));
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        // Compute statement balance
        BigDecimal interest = totalInterest == null ? BigDecimal.ZERO : totalInterest;
        BigDecimal outstanding = totalOutstanding == null ? BigDecimal.ZERO : totalOutstanding;

        // If your domain includes interest in statement balance, keep it
        BigDecimal statementBalance = outstanding
                .add(interest)
                .add(totalFeeApplied)
                .setScale(2, RoundingMode.HALF_UP);

        // Remaining & carry-forward
        BigDecimal remainingStatementBalance = statementBalance
                .subtract(totalPaid)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal carryForwardBalance = remainingStatementBalance;

        // Dates required by mapper
        LocalDate statementDate = LocalDate.now();
        LocalDate dueDate = billingCycle.getDueDate();

        // Create and save statement
        Statement statement = statementMapper.toEntity(
                card,
                billingCycle,
                statementBalance,
                remainingStatementBalance,
                minimumDue == null ? BigDecimal.ZERO : minimumDue,
                interest,
                outstanding,
                totalFeeApplied,
                cashAdvanceFee,
                carryForwardBalance
        );

        Statement savedStatement = statementRepository.save(statement);

        return statementMapper.toGenerateResponseDTO(savedStatement);
    }

    // GET /statements/{card_id}/{cycle_id}
    public RetrieveStatementResponseDTO getStatement(UUID cardId, UUID cycleId) {

        // Validate card exists
        cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

        // Find statement by cycle
        Statement statement = statementRepository.findByCycleId(cycleId)
                .orElseThrow(() -> new RuntimeException("Statement not found for cycle: " + cycleId));

        // Validate statement belongs to card
        if (!statement.getCard().getCardId().equals(cardId)) {
            throw new RuntimeException("Statement does not belong to this card");
        }

        return statementMapper.toRetrieveResponseDTO(statement);
    }
}