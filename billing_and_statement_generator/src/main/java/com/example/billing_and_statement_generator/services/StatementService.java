package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.mapper.StatementMapper;
import com.example.billing_and_statement_generator.mapper.TransactionMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.StatementRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static com.example.billing_and_statement_generator.util.BillingUtils.calculateCashAdvanceFees;
import static com.example.billing_and_statement_generator.util.BillingUtils.calculateTotalFees;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final CardRepository cardRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final StatementMapper statementMapper;
    private final TransactionMapper transactionMapper;

    // POST /statements/v1/generate
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
        BigDecimal totalInterest = billingCycle.getTotalInterest();
        BigDecimal totalOutstanding = billingCycle.getTotalOutstanding();
        BigDecimal minimumDue = billingCycle.getMinimumDue();


        List<Transaction> cycleTransactions =
                billingCycle.getTransactions() == null
                        ? List.of()
                        : billingCycle.getTransactions();

        // Use helper method in BillingUtils for calculating cash advance fees
        BigDecimal cashAdvanceFee = calculateCashAdvanceFees(cycleTransactions);

        // Use helper method in BillingUtils for calculating all fees
        BigDecimal totalFeeApplied = calculateTotalFees(cycleTransactions);

        // Payments so far (null-safe)
        BigDecimal totalPaid = paymentRepository.findTotalPaidByCycleId(UUID.fromString(dto.getCycleId()));
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        BigDecimal interest = totalInterest == null ? BigDecimal.ZERO : totalInterest;
        BigDecimal outstanding = totalOutstanding == null ? BigDecimal.ZERO : totalOutstanding;

        // Fetch outstanding balance from the total balance in Billing Cycle
        BigDecimal statementBalance = billingCycle.getTotalOutstanding();

        BigDecimal remainingStatementBalance = statementBalance
                .subtract(totalPaid)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal carryForwardBalance = remainingStatementBalance;

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

    // POST /statements/v1/get
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

        // Fetch transactions for this billing cycle and map to DTOs
        List<CreateTransactionResponseDTO> transactions =
                transactionRepository.findByBillingCycleCycleId(cycleId)
                        .stream()
                        .map(transactionMapper::toResponse)
                        .toList();

        return statementMapper.toRetrieveResponseDTO(statement, transactions);
    }
}