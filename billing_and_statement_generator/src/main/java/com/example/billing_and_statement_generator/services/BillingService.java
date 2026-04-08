package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import com.example.billing_and_statement_generator.util.BillingUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.billing_and_statement_generator.util.BillingUtils.calculateTotalFees;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {
    private final CardRepository cardRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final CardService cardService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public BillingCycleResponseDTO generateBillingCycle(UUID cardId) {
        log.info("/api/billing/generate/{} - generating billing cycle", cardId);
        try {
            // 1. Verify card exists
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Card not found with ID: " + cardId));

            // 2. Determine cycle dates
            LocalDate cycleEndDate = LocalDate.now();

            Optional<BillingCycle> lastCycleOpt =
                    billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId);

            // Close previous cycle if it is still open
            if (lastCycleOpt.isPresent()) {
                BillingCycle previousCycle = lastCycleOpt.get();
                if ("OPEN".equals(previousCycle.getCycleStatus())) {
                    previousCycle.setCycleStatus("CLOSED");
                    billingCycleRepository.save(previousCycle);
                }

                log.info("Closed previous cycle {}", previousCycle.getCycleId());
            }

            LocalDate cycleStartDate = lastCycleOpt
                    .map(c -> c.getCycleEndDate().plusDays(1))
                    .orElse(cycleEndDate.minusDays(30));

            LocalDate dueDate = BillingUtils.calculateDueDate(cycleEndDate);

            // 3. Fetch unbilled transactions and total balances
            BigDecimal previousPurchaseBalance = card.getCardBalance(); // carry-over purchase balance
            BigDecimal previousCashAdvanceBalance = card.getCashAdvanceBalance(); // carry-over cash advance balance

            BigDecimal previousTotalBalance = lastCycleOpt
                    .map(BillingCycle::getTotalOutstanding)
                    .orElse(BigDecimal.ZERO);

            log.info("Snapshot balances — purchase: {}, cashAdvance: {}, total: {}",
                    previousPurchaseBalance, previousCashAdvanceBalance, previousTotalBalance);

            List<Transaction> unbilledTxns = transactionRepository
                    .findByCardCardIdAndBillingCycleIsNull(cardId);

            // 4. Retrieve all payments made in this cycle
            List<Payment> payments = paymentRepository
                    .findPaymentsWithinCycle(cardId, cycleStartDate, cycleEndDate);

            BigDecimal totalPayments = payments.stream()
                    .map(Payment::getAmountPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("Payments applied in cycle: {}", totalPayments);

            // 5. Sum purchases and cash advances separately
            BigDecimal totalPurchases = unbilledTxns.stream()
                    .filter(t -> Transaction.transactionType.PURCHASE
                            .equals(t.getTransactionType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("Total purchases in cycle: {}", totalPurchases);

            BigDecimal totalCashAdvance = unbilledTxns.stream()
                    .filter(t -> Transaction.transactionType.CASHADVANCE
                            .equals(t.getTransactionType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("Total cash advance in cycle: {}", totalCashAdvance);

            // 6. INTEREST — calculated on previousBalance only (unpaid balances from last cycle)
            // New purchases/cash advances in THIS cycle haven't been outstanding
            // long enough to accrue a full cycle of interest
            // Interest rates come from Card entity set during card creation
            BigDecimal annualRate = card.getAnnualInterestRate() != null
                    ? card.getAnnualInterestRate()
                    : new BigDecimal("0.20");

            BigDecimal cashAdvanceAPR = card.getCashAdvanceAPR() != null
                    ? card.getCashAdvanceAPR()
                    : new BigDecimal("0.24");

            // Calculate interest on previous card balance using standard APR
            BigDecimal purchaseInterest = BillingUtils.calculateInterest(
                    previousPurchaseBalance, annualRate, cycleStartDate, cycleEndDate);

            // Calculate interest on previous cash advance balance separately (Cash advances use a higher APR than regular purchases)
            BigDecimal cashAdvanceInterest = BillingUtils.calculateInterest(
                    previousCashAdvanceBalance, cashAdvanceAPR,
                    cycleStartDate, cycleEndDate);

            BigDecimal totalInterest = purchaseInterest.add(cashAdvanceInterest);

            // Apply interest to card via TransactionService
            // Interest is NOT stored as a transaction — it's applied directly
            // to balances via CardService
            if (purchaseInterest.compareTo(BigDecimal.ZERO) > 0) {
                transactionService.createInterest(
                        cardId, purchaseInterest,
                        TransactionService.InterestType.CARDBALANCE);
            }
            if (cashAdvanceInterest.compareTo(BigDecimal.ZERO) > 0) {
                transactionService.createInterest(
                        cardId, cashAdvanceInterest,
                        TransactionService.InterestType.CASHADVANCE);
            }

            // 7. LATE FEE — applied if cycle has an unpaid balance AND past due date
            // Late fee amount taken from Card entity
            BigDecimal lateFee = BigDecimal.ZERO;
            if (lastCycleOpt.isPresent() &&
                    lastCycleOpt.get().getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0 &&
                    lastCycleOpt.get().getDueDate().isBefore(LocalDate.now())) {

                lateFee = card.getLateFeeAmount() != null
                        ? card.getLateFeeAmount()
                        : new BigDecimal("50.00");

                // Late fee stored in transactions under FEE type via TransactionService
                transactionService.createFee(cardId, lateFee, cycleEndDate, Transaction.transactionType.LATEFEE);
                log.info("Late fee {} applied for card {}", lateFee, cardId);
            }

            // 8. CASH ADVANCE FEE — rate taken from Card entity
            // Already applied per transaction in TransactionService.create()
            // Sum FEE transactions to get total fees applied this cycle
            BigDecimal cashAdvanceFee = unbilledTxns.stream()
                    .filter(t -> Transaction.transactionType.CASHADVANCEFEE
                            .equals(t.getTransactionType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 9. ANNUAL MEMBERSHIP FEE — check if anniversary year reached
            BigDecimal annualMembershipFee = checkAndApplyAnnualFee(
                    card, cardId, cycleStartDate, cycleEndDate);

            // Summing up the fees
            BigDecimal totalFeesApplied = lateFee
                    .add(cashAdvanceFee)
                    .add(annualMembershipFee);

            // 10. Total outstanding
            BigDecimal totalOutstanding = card.getCardBalance()
                    .add(card.getCashAdvanceBalance());

            // 11. Minimum due = max(5% of totalOutstanding, $100) + overdue amount(if any)
            BigDecimal minimumDue =
                    BillingUtils.calculateMinimumDue(totalOutstanding);

            cardService.setMinimumDue(cardId, minimumDue);

            // 12. Add due date to card
            cardService.setDueDate(cardId, dueDate);

            // 13. Persist billing cycle
            BillingCycle cycle = BillingCycle.builder()
                    .cycleId(UUID.randomUUID())
                    .card(card)
                    .cycleStartDate(cycleStartDate)
                    .cycleEndDate(cycleEndDate)
                    .dueDate(dueDate)
                    .creditLimit(card.getCreditLimit())
                    .previousBalance(previousTotalBalance)
                    .totalPurchases(totalPurchases)
                    .totalCashAdvance(totalCashAdvance)
                    .totalInterest(totalInterest)
                    .totalOutstanding(totalOutstanding)
                    .minimumDue(minimumDue)
                    .cycleStatus("OPEN")
                    .build();

            BillingCycle saved = billingCycleRepository.save(cycle);

            unbilledTxns.forEach(tx -> tx.setBillingCycle(saved));
            transactionRepository.saveAll(unbilledTxns);

            payments.forEach(p -> p.setBillingCycle(saved));
            paymentRepository.saveAll(payments);

            log.info("/api/billing/generate/{} - cycle {} generated",
                    cardId, saved.getCycleId());

            List<Transaction> cycleTxns =
                    transactionRepository.findByBillingCycleCycleId(saved.getCycleId());

            return toResponseDTO(saved, cycleTxns, totalFeesApplied);

        } catch (EntityNotFoundException e) {
            log.error("/api/billing/generate/{} - not found: {}",
                    cardId, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("/api/billing/generate/{} - error: {}",
                    cardId, e.getMessage());
            throw new RuntimeException(
                    "Failed to generate billing cycle: " + e.getMessage());
        }
    }

    public BillingCycleResponseDTO getBillingCycle(UUID cardId, UUID cycleId) {
        log.info("/api/billing/{}/{} - retrieving cycle", cardId, cycleId);

        BillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Billing cycle not found with ID: " + cycleId));

        if (!cycle.getCard().getCardId().equals(cardId)) {
            throw new EntityNotFoundException(
                    "Cycle " + cycleId + " not found for card " + cardId);
        }

//        List<Transaction> txns = cycle.getTransactions() != null
//                ? cycle.getTransactions() : List.of();

        List<Transaction> cycleTxns = cycle.getTransactions() != null
                ? cycle.getTransactions() : List.of();

        // Summing up the fees
        BigDecimal totalFeesApplied = calculateTotalFees(cycleTxns);

        return toResponseDTO(cycle, cycleTxns, totalFeesApplied);
    }

    // Checks if annual membership fee should be applied this cycle
    // Rule: fee is charged once per year on the anniversary of card issue date
    // e.g. card issued 2024-03-15 → fee charged on 2025-03-15, 2026-03-15 etc.
    private BigDecimal checkAndApplyAnnualFee(Card card, UUID cardId,
                                              LocalDate cycleStart,
                                              LocalDate cycleEnd) {
        // No fee configured — skip
        if (card.getAnnualMembershipFee() == null ||
                card.getAnnualMembershipFee().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // No issue date — cannot calculate anniversary
        if (card.getCardIssueDate() == null) {
            return BigDecimal.ZERO;
        }

        LocalDate issueDate = card.getCardIssueDate();

        // Check if any anniversary date falls within this billing cycle
        // Anniversary = same month/day as issue date but in a future year
        int startYear = cycleStart.getYear();
        int endYear = cycleEnd.getYear();

        for (int year = startYear; year <= endYear; year++) {
            // Skip the issue year itself — no fee on first year
            if (year == issueDate.getYear()) continue;

            try {
                LocalDate anniversaryDate = issueDate.withYear(year);
                // Check if anniversary falls within the billing cycle range
                if (!anniversaryDate.isBefore(cycleStart) &&
                        !anniversaryDate.isAfter(cycleEnd)) {
                    BigDecimal fee = card.getAnnualMembershipFee();
                    // Apply fee via TransactionService — stored under FEE type
                    transactionService.createFee(cardId, fee, anniversaryDate, Transaction.transactionType.ANNUALMEMBERSHIPFEE);
                    log.info("Annual membership fee {} applied for card {} on {}",
                            fee, cardId, anniversaryDate);
                    return fee;
                }
            } catch (Exception e) {
                // Handles Feb 29 on non-leap years gracefully
                log.warn("Could not calculate anniversary for year {}: {}",
                        year, e.getMessage());
            }
        }

        return BigDecimal.ZERO;
    }

    // Original mapping — combined feesApplied total
    private BillingCycleResponseDTO toResponseDTO(BillingCycle cycle,
                                                  List<Transaction> txns,
                                                  BigDecimal feesApplied) {
        Card card = cycle.getCard();

        return BillingCycleResponseDTO.builder()
                .cycleId(cycle.getCycleId())
                .cardId(cycle.getCard().getCardId())
                .cycleStartDate(cycle.getCycleStartDate())
                .cycleEndDate(cycle.getCycleEndDate())
                .dueDate(cycle.getDueDate())
                .creditLimit(cycle.getCreditLimit())
                .previousBalance(cycle.getPreviousBalance())
                .totalPurchases(cycle.getTotalPurchases())
                .totalCashAdvance(cycle.getTotalCashAdvance())
                .totalInterest(cycle.getTotalInterest())
                .feesApplied(feesApplied)
                .totalOutstanding(cycle.getTotalOutstanding())
                .minimumDue(cycle.getMinimumDue())
                .cycleStatus(cycle.getCycleStatus())
                .availableCredit(card.getAvailableCredit())
                .transaction(buildTxnDTOs(cycle, txns))
                .build();
    }

    // Shared transaction list builder
    private List<CreateTransactionResponseDTO> buildTxnDTOs(
            BillingCycle cycle, List<Transaction> txns) {
        return txns.stream()
                .map(t -> CreateTransactionResponseDTO.builder()
                        .transactionId(t.getTransactionId())
                        .cardId(t.getCard().getCardId())
                        .cycleId(cycle.getCycleId())
                        .transactionDate(t.getTransactionDate())
                        .transactionType(t.getTransactionType())
                        .amount(t.getAmount())
                        .merchantName(t.getMerchantName())
                        .status(t.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}