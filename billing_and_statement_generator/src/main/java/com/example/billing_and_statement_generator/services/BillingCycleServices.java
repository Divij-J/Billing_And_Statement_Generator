package com.example.billing_and_statement_generator.service;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.dto.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

  private final CardRepository cardRepository;
  private final BillingCycleRepository billingCycleRepository;
  private final TransactionRepository transactionRepository;

  @Transactional
  public BillingCycleResponseDTO generateBillingCycle(UUID cardId) {
    log.info("/api/billing/generate/{} - generating billing cycle", cardId);
    try {
      // Verify card exists
      Card card = cardRepository.findById(cardId)
          .orElseThrow(() -> new EntityNotFoundException(
                                "Card not found with ID: " + cardId));

      // Set cycle dates
      // cycleEndDate = today; cycleStartDate = day after last cycle or 30 days ago
      LocalDate cycleEndDate = LocalDate.now();
      Optional<BillingCycle> lastCycleOpt =
        billingCycleRepository
          .findTopByCardCardIdOrderByCycleEndDateDesc(cardId);

      LocalDate cycleStartDate = lastCycleOpt
          .map(c -> c.getCycleEndDate().plusDays(1))
          .orElse(cycleEndDate.minusDays(30));

      // Due date = cycle end + 21 days
      LocalDate dueDate = BillingUtils.calculateDueDate(cycleEndDate);

      // Previous balance carried forward
      BigDecimal previousBalance = lastCycleOpt
          .filter(c -> "OPEN".equals(c.getCycleStatus()))
          .map(BillingCycle::getTotalOutstanding)
          .orElse(BigDecimal.ZERO);

      // Fetch unbilled transactions
      List<Transaction> unbilledTxns =
        transactionRepository
          .findByCardCardIdAndBillingCycleIsNull(cardId);

      // Sum PURCHASE and CASHADVANCE types separately
      BigDecimal totalPurchases = unbilledTxns.stream()
          .filter(t -> Transaction.transactionType.PURCHASE
                                .equals(t.getTransactionType()))
          .map(Transaction::getAmount)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal totalCashAdvance = unbilledTxns.stream()
          .filter(t -> Transaction.transactionType.CASHADVANCE
                                .equals(t.getTransactionType()))
          .map(Transaction::getAmount)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      // Daily interest: outstanding × (annualRate ÷ 365) × days
      BigDecimal outstanding =
        previousBalance.add(totalPurchases).add(totalCashAdvance);
      BigDecimal annualRate = card.getAnnualInterestRate() != null
          ? card.getAnnualInterestRate()
          : new BigDecimal("0.24");
      BigDecimal interest = BillingUtils.calculateInterest(
                            outstanding, annualRate, cycleStartDate, cycleEndDate);

      // Late fee if previous cycle still unpaid (OPEN status)
      BigDecimal lateFee = BigDecimal.ZERO;
      if (lastCycleOpt.isPresent() &&
        "OPEN".equals(lastCycleOpt.get().getCycleStatus())) {
        lateFee = card.getLateFeeAmount() != null
            ? card.getLateFeeAmount()
            : new BigDecimal("50.00");
        log.info("Late fee {} applied for card {}", lateFee, cardId);
      }

      // Cash advance fee = feeRate × total cash advance
      BigDecimal cashAdvanceFeeRate = card.getCashAdvanceFeeRate() != null
          ? card.getCashAdvanceFeeRate()
          : new BigDecimal("0.02");
      BigDecimal cashAdvanceFee = BillingUtils.calculateCashAdvanceFee(
                            totalCashAdvance, cashAdvanceFeeRate);
      BigDecimal totalFees = lateFee.add(cashAdvanceFee);

      // Grand total = previous + purchases + cash advance + interest + fees
      BigDecimal totalOutstanding =
        outstanding.add(interest).add(totalFees);

      // Minimum due = max(5% of totalOutstanding, $100)
      BigDecimal minimumDue =
        BillingUtils.calculateMinimumDue(totalOutstanding);

      // Persist billing cycle to DB
      BillingCycle cycle = BillingCycle.builder()
          .cycleId(UUID.randomUUID())
          .card(card)
          .cycleStartDate(cycleStartDate)
          .cycleEndDate(cycleEndDate)
          .dueDate(dueDate)
          .creditLimit(card.getCreditLimit())
          .previousBalance(previousBalance)
          .totalPurchases(totalPurchases)
          .totalCashAdvance(totalCashAdvance)
          .totalInterest(interest)
          .totalOutstanding(totalOutstanding)
          .minimumDue(minimumDue)
          .cycleStatus("OPEN")
          .build();

      BillingCycle saved = billingCycleRepository.save(cycle);

      // Link all unbilled transactions to this cycle
      unbilledTxns.forEach(t -> t.setBillingCycle(saved));
      transactionRepository.saveAll(unbilledTxns);

      log.info("/api/billing/generate/{} - cycle {} generated",
                            cardId, saved.getCycleId());
      return toResponseDTO(saved, unbilledTxns, totalFees);

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

    // Verify the cycle belongs to the requested card — prevents data leaks
    if (!cycle.getCard().getCardId().equals(cardId)) {
      throw new EntityNotFoundException(
                            "Cycle " + cycleId + " not found for card " + cardId);
    }

    List<Transaction> txns = cycle.getTransactions() != null
        ? cycle.getTransactions() : List.of();

    // Recalculate fees from stored values for display in response
    BigDecimal fees = cycle.getTotalOutstanding()
        .subtract(cycle.getPreviousBalance())
        .subtract(cycle.getTotalPurchases())
        .subtract(cycle.getTotalCashAdvance())
        .subtract(cycle.getTotalInterest());

    log.info("/api/billing/{}/{} - successfully retrieved", cardId, cycleId);
    return toResponseDTO(cycle, txns, fees);
  }


          private BillingCycleResponseDTO toResponseDTO(BillingCycle cycle,
                         List<Transaction> txns,
                         BigDecimal feesApplied) {
    List<CreateTransactionResponseDTO> txnDTOs = txns.stream()
        .map(t -> CreateTransactionResponseDTO.builder()
                            .transactionId(t.getTransactionId())
            .cardId(t.getCard().getCardId())
            .cycleId(cycle.getCycleId())
            .transactionDate(t.getTransactionDate())
            .type(t.getTransactionType())
            .amount(t.getAmount())
            .merchantName(t.getMerchantName())
            .status(t.getStatus())
            .build())
        .collect(Collectors.toList());

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
        .transaction(txnDTOs)
        .build();
  }
}