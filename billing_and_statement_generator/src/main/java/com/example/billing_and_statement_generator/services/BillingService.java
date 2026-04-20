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

            LocalDate cycleStartDate = cycleEndDate.minusDays(30);
            LocalDate dueDate = BillingUtils.calculateDueDate(cycleEndDate);

            // 3. Fetch unbilled transactions
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
                    .filter(t -> Transaction.transactionType.PURCHASE.equals(t.getTransactionType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCashAdvance = unbilledTxns.stream()
                    .filter(t -> Transaction.transactionType.CASHADVANCE.equals(t.getTransactionType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("Total purchases: {}, Total cash advance: {}", totalPurchases, totalCashAdvance);

            // 6. INTEREST — only charged if there was an UNPAID balance from previous cycle
            BigDecimal annualRate = card.getAnnualInterestRate() != null
                    ? card.getAnnualInterestRate()
                    : new BigDecimal("0.20");

            BigDecimal cashAdvanceAPR = card.getCashAdvanceAPR() != null
                    ? card.getCashAdvanceAPR()
                    : new BigDecimal("0.24");

            BigDecimal previousPurchaseBalance = BigDecimal.ZERO;
            BigDecimal previousCashAdvanceBalance = BigDecimal.ZERO;

            if (lastCycleOpt.isPresent()) {
                BillingCycle lastCycle = lastCycleOpt.get();
                BigDecimal previousOutstanding = lastCycle.getTotalOutstanding() != null
                        ? lastCycle.getTotalOutstanding()
                        : BigDecimal.ZERO;

                if (previousOutstanding.compareTo(BigDecimal.ZERO) > 0) {
                    previousPurchaseBalance = card.getCardBalance()
                            .subtract(totalPurchases)
                            .max(BigDecimal.ZERO);
                    previousCashAdvanceBalance = card.getCashAdvanceBalance()
                            .subtract(totalCashAdvance)
                            .max(BigDecimal.ZERO);

                    log.info(
                            "Previous unpaid balance detected — interest will be charged. prevPurchase={}, prevCashAdvance={}",
                            previousPurchaseBalance, previousCashAdvanceBalance);
                } else {
                    log.info("Previous cycle fully paid — no interest charged for card {}", cardId);
                }
            } else {
                log.info("First billing cycle for card {} — no interest charged", cardId);
            }

            BigDecimal purchaseInterest = previousPurchaseBalance.compareTo(BigDecimal.ZERO) > 0
                    ? BillingUtils.calculateInterest(previousPurchaseBalance, annualRate,
                    cycleStartDate, cycleEndDate)
                    : BigDecimal.ZERO;

            BigDecimal cashAdvanceInterest = previousCashAdvanceBalance.compareTo(BigDecimal.ZERO) > 0
                    ? BillingUtils.calculateInterest(previousCashAdvanceBalance, cashAdvanceAPR,
                    cycleStartDate, cycleEndDate)
                    : BigDecimal.ZERO;

            BigDecimal totalInterest = purchaseInterest.add(cashAdvanceInterest);

            if (purchaseInterest.compareTo(BigDecimal.ZERO) > 0) {
                transactionService.createInterest(cardId, purchaseInterest,
                        TransactionService.InterestType.CARDBALANCE);
            }
            if (cashAdvanceInterest.compareTo(BigDecimal.ZERO) > 0) {
                transactionService.createInterest(cardId, cashAdvanceInterest,
                        TransactionService.InterestType.CASHADVANCE);
            }

            // 7. LATE FEE
            BigDecimal lateFee = BigDecimal.ZERO;
            if (lastCycleOpt.isPresent()
                    && lastCycleOpt.get().getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0
                    && lastCycleOpt.get().getDueDate().isBefore(LocalDate.now())) {

                lateFee = card.getLateFeeAmount() != null
                        ? card.getLateFeeAmount()
                        : new BigDecimal("50.00");

                transactionService.createFee(cardId, lateFee, cycleEndDate,
                        Transaction.transactionType.LATEFEE);
                log.info("Late fee {} applied for card {}", lateFee, cardId);
            }

            // Calculate cash advance fee for this billing cycle
            BigDecimal cashAdvanceFee =
                    BillingUtils.calculateCashAdvanceFee(totalCashAdvance);

            if (cashAdvanceFee.compareTo(BigDecimal.ZERO) > 0) {
                transactionService.createFee(
                        cardId,
                        cashAdvanceFee,   // must be 10.00 or higher
                        cycleEndDate,
                        Transaction.transactionType.CASHADVANCEFEE
                );
            }
            BigDecimal annualMembershipFee = checkAndApplyAnnualFee(
                    card, cardId, cycleStartDate, cycleEndDate);

            BigDecimal totalFeesApplied = lateFee.add(cashAdvanceFee).add(annualMembershipFee);

            BigDecimal totalOutstanding = card.getCardBalance()
                    .add(card.getCashAdvanceBalance());

            BigDecimal minimumDue = BillingUtils.calculateMinimumDue(totalOutstanding);
            cardService.setMinimumDue(cardId, minimumDue);

            cardService.setDueDate(cardId, dueDate);

            BillingCycle cycle = BillingCycle.builder()
                    .cycleId(UUID.randomUUID())
                    .card(card)
                    .cycleStartDate(cycleStartDate)
                    .cycleEndDate(cycleEndDate)
                    .dueDate(dueDate)
                    .creditLimit(card.getCreditLimit())
                    .previousBalance(lastCycleOpt.map(BillingCycle::getTotalOutstanding).orElse(BigDecimal.ZERO))
                    .totalPurchases(totalPurchases)
                    .totalCashAdvance(totalCashAdvance)
                    .totalInterest(totalInterest)
                    .totalOutstanding(totalOutstanding)
                    .minimumDue(minimumDue)
                    .cycleStatus("OPEN")
                    .build();

            BillingCycle saved = billingCycleRepository.save(cycle);

            // Attach newly created fee transactions to the billing cycle
            List<Transaction> newlyCreatedFees =
                    transactionRepository.findByCardCardIdAndBillingCycleIsNull(cardId)
                            .stream()
//                            .filter(tx -> tx.getTransactionType() == Transaction.transactionType.CASHADVANCEFEE)
                            .filter(tx ->
                                    tx.getTransactionType() == Transaction.transactionType.CASHADVANCEFEE ||
                                            tx.getTransactionType() == Transaction.transactionType.LATEFEE
                            )
                            .toList();

            newlyCreatedFees.forEach(tx -> tx.setBillingCycle(saved));
            transactionRepository.saveAll(newlyCreatedFees);


            unbilledTxns.forEach(tx -> tx.setBillingCycle(saved));
            transactionRepository.saveAll(unbilledTxns);

            payments.forEach(p -> p.setBillingCycle(saved));
            paymentRepository.saveAll(payments);

            log.info("/api/billing/generate/{} - cycle {} generated", cardId, saved.getCycleId());

            List<Transaction> cycleTxns =
                    transactionRepository.findByBillingCycleCycleId(saved.getCycleId());

            return toResponseDTO(saved, cycleTxns, totalFeesApplied);

        } catch (EntityNotFoundException e) {
            log.error("/api/billing/generate/{} - not found: {}", cardId, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("/api/billing/generate/{} - error: {}", cardId, e.getMessage());
            throw new RuntimeException("Failed to generate billing cycle: " + e.getMessage());
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

        List<Transaction> cycleTxns = cycle.getTransactions() != null
                ? cycle.getTransactions()
                : List.of();

        BigDecimal totalFeesApplied = calculateTotalFees(cycleTxns);

        return toResponseDTO(cycle, cycleTxns, totalFeesApplied);
    }

    private BigDecimal checkAndApplyAnnualFee(
            Card card, UUID cardId, LocalDate cycleStart, LocalDate cycleEnd) {

        if (card.getAnnualMembershipFee() == null
                || card.getAnnualMembershipFee().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (card.getCardIssueDate() == null) {
            return BigDecimal.ZERO;
        }

        LocalDate issueDate = card.getCardIssueDate();
        int startYear = cycleStart.getYear();
        int endYear = cycleEnd.getYear();

        for (int year = startYear; year <= endYear; year++) {
            if (year == issueDate.getYear()) continue;
            try {
                LocalDate anniversaryDate = issueDate.withYear(year);
                if (!anniversaryDate.isBefore(cycleStart)
                        && !anniversaryDate.isAfter(cycleEnd)) {

                    BigDecimal fee = card.getAnnualMembershipFee();
                    transactionService.createFee(
                            cardId, fee, anniversaryDate,
                            Transaction.transactionType.ANNUALMEMBERSHIPFEE);

                    log.info(
                            "Annual membership fee {} applied for card {} on {}",
                            fee, cardId, anniversaryDate);
                    return fee;
                }
            } catch (Exception e) {
                log.warn(
                        "Could not calculate anniversary for year {}: {}",
                        year, e.getMessage());
            }
        }
        return BigDecimal.ZERO;
    }

    private BillingCycleResponseDTO toResponseDTO(
            BillingCycle cycle,
            List<Transaction> txns,
            BigDecimal feesApplied) {

        BigDecimal lateFee = txns.stream()
                .filter(t -> t.getTransactionType() == Transaction.transactionType.LATEFEE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashAdvanceFee = txns.stream()
                .filter(t -> t.getTransactionType() == Transaction.transactionType.CASHADVANCEFEE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                .cashAdvanceFee(cashAdvanceFee)
                .lateFee(lateFee)
                .build();
    }

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