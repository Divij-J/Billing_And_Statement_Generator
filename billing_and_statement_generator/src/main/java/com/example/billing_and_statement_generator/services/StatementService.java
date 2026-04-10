package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.mapper.PaymentMapper;
import com.example.billing_and_statement_generator.mapper.StatementMapper;
import com.example.billing_and_statement_generator.mapper.TransactionMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.StatementRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static com.example.billing_and_statement_generator.util.BillingUtils.calculateCashAdvanceFees;
import static com.example.billing_and_statement_generator.util.BillingUtils.calculateTotalFees;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final StatementRepository statementRepository;
    private final CardRepository cardRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final StatementMapper statementMapper;
    private final TransactionMapper transactionMapper;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

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

        BigDecimal cashAdvanceFee = calculateCashAdvanceFees(cycleTransactions);
        BigDecimal totalFeeApplied = calculateTotalFees(cycleTransactions);

        BigDecimal interest = totalInterest == null ? BigDecimal.ZERO : totalInterest;
        BigDecimal outstanding = totalOutstanding == null ? BigDecimal.ZERO : totalOutstanding;

        BigDecimal statementBalance = outstanding;

        // Calculate total paid
        BigDecimal currentCardTotal = card.getCardBalance().add(card.getCashAdvanceBalance());
        BigDecimal totalPaid = outstanding.subtract(currentCardTotal).max(BigDecimal.ZERO);

        BigDecimal remainingStatementBalance = statementBalance
                .subtract(totalPaid)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal carryForwardBalance = remainingStatementBalance;

        // Build statement entity
        Statement statement = statementMapper.toEntity(
                card, billingCycle, statementBalance, remainingStatementBalance,
                minimumDue == null ? BigDecimal.ZERO : minimumDue,
                interest, outstanding, totalFeeApplied, cashAdvanceFee, carryForwardBalance
        );

        // Fetch transactions and payments at time of generation — snapshot
        List<CreateTransactionResponseDTO> transactions =
                transactionRepository.findByBillingCycleCycleId(UUID.fromString(dto.getCycleId()))
                        .stream()
                        .map(transactionMapper::toResponse)
                        .toList();

        List<Payment> paymentEntities =
                paymentRepository.findByCycleId(UUID.fromString(dto.getCycleId()));

        List<RetrievePaymentHistoryDTO> payments = paymentEntities.stream()
                .map(paymentMapper::toHistoryDTO)
                .toList();

        BigDecimal amountPaid = paymentEntities.stream()
                .map(Payment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build retrieve snapshot
        RetrieveStatementResponseDTO snapshot =
                statementMapper.toRetrieveResponseDTO(statement, transactions, payments, amountPaid);

        // Serialize snapshot
        try {
            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            statement.setStatementSnapshot(snapshotJson);
        } catch (Exception e) {
            log.warn("Failed to serialize statement snapshot: {}", e.getMessage());
        }

        Statement savedStatement = statementRepository.save(statement);
        log.info("Statement generated: statementId={}, cardId={}, cycleId={}",
                savedStatement.getStatementId(), card.getCardId(), billingCycle.getCycleId());

        return statementMapper.toGenerateResponseDTO(savedStatement);
    }

    // POST /statements/v1/get
    public RetrieveStatementResponseDTO getStatement(UUID statementId) {

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new RuntimeException("Statement not found: " + statementId));

        // Return frozen snapshot
        if (statement.getStatementSnapshot() != null) {
            try {
                return objectMapper.readValue(
                        statement.getStatementSnapshot(),
                        RetrieveStatementResponseDTO.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize statement snapshot for statementId={}: {}",
                        statementId, e.getMessage());
            }
        }

        throw new RuntimeException("Statement snapshot not found for statementId: " + statementId);
    }
}