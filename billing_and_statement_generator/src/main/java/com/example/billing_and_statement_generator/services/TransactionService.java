package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;

import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Transaction;

import com.example.billing_and_statement_generator.mapper.TransactionMapper;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final TransactionMapper transactionMapper;
    private final CardService cardService;

    /** Transaction DTO Creation
     * Create a transaction and apply its effect to the card balance.
     * PURCHASE / CASHADVANCE: increases balance (limit checks in CardService)
     * DECLINED transactions will still be saved to DB but will not affect balance
     * status is server-controlled (PENDING -> SENT on success)
     */
    @Transactional(
            noRollbackFor = {
                    CardService.LimitExceededException.class,
                    CardService.ValidationException.class
            }) // ensures that DECLINED transactions are still saved to the DB
    public CreateTransactionResponseDTO create(CreateTransactionRequestDTO dto) {
        // Validate & load managed Card
        UUID cardId = dto.getCardId();
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));

        // Map DTO to Entity (mapper creates a shallow Card; replace with managed)
        Transaction tx = transactionMapper.toEntity(dto);
        tx.setCard(card);

        // Set server-managed fields
        tx.setTransactionId(UUID.randomUUID());
        tx.setStatus(Transaction.Status.PENDING); // set as PENDING until it is confirmed
        if(dto.getTransactionDate() == null)
            tx.setTransactionDate(LocalDate.now());

        // Apply the financial impact using CardService
        BigDecimal amount = dto.getAmount();
        try {
            switch (dto.getTransactionType()) {
                // PURCHASES and INTEREST act in the same manner when adjusting the card balance
                case PURCHASE -> {
                    BigDecimal newBalance = cardService.applyPurchase(cardId, amount);
                    log.debug("Transaction PURCHASE applied: cardId={}, amount={}, newBalance={}", cardId, amount, newBalance);
                }
                case CASHADVANCE -> {
                    BigDecimal newBalance = cardService.applyCashAdvance(cardId, amount);
                    BigDecimal fee = amount.multiply(card.getCashAdvanceFeeRate()).setScale(2, java.math.RoundingMode.HALF_UP);
                    createFee(cardId, fee, dto.getTransactionDate(), Transaction.transactionType.CASHADVANCEFEE);
                    log.debug("Transaction CASHADVANCE applied: cardId={}, amount={}, fee={}, newCashAdvanceBalance={}", cardId, amount, fee, newBalance);
                }
                default -> throw new ValidationException("Unsupported transaction type: " + dto.getTransactionType());
            }

            // Mark SENT after successful balance application
            tx.setStatus(Transaction.Status.SENT);
        }
        // Handle declined transactions and save to DB
        catch (CardService.LimitExceededException e) {
            log.warn("TX_LIMIT_EXCEEDED cardId={} type={} amount={} msg={}",
                    cardId, dto.getTransactionType(), dto.getAmount(), e.getMessage());
            tx.setStatus(Transaction.Status.DECLINED);
            Transaction saved = transactionRepository.save(tx);
            throw e;
        } catch (CardService.ValidationException e) {
            log.warn("TX_VALIDATION_FAILED cardId={} type={} amount={} msg={}",
                    cardId, dto.getTransactionType(), dto.getAmount(), e.getMessage());
            tx.setStatus(Transaction.Status.DECLINED);
            Transaction saved = transactionRepository.save(tx);
            throw e;
        }

        // Save successful transaction to DB
        Transaction saved = transactionRepository.save(tx);
        return transactionMapper.toResponse(saved);
    }

    /** Interest Creation
     * When interest are calculated within Billing Service,
     * it calls this method which creates an INTEREST transaction to add to balance
     * DECLINED interest transactions will NOT be saved if failed
     */
    @Transactional
    public void createInterest(UUID cardId, BigDecimal amount, InterestType type){
        if(amount == null){
            throw new ValidationException("Interest Amount is required");
        }
        if(amount.signum() <= 0){
            throw new ValidationException("Interest Amount must be > 0");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));

        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setCard(card);
        tx.setAmount(amount);
        tx.setTransactionDate(LocalDate.now());
        tx.setMerchantName(type.toString());
        tx.setTransactionType(Transaction.transactionType.INTEREST);

        BigDecimal newBalance = cardService.applyInterest(cardId, amount, type);
        tx.setStatus(Transaction.Status.SENT);
        log.debug("Transaction INTEREST created: cardId={}, newBalance={}, amount={}", cardId, newBalance, amount);

        transactionRepository.save(tx);
    }

    /** Fee Creation
     * When fees are calculated within Billing Service,
     * it calls this method which creates a FEE transaction to add to balance
     * DECLINED interest transactions will NOT be saved if failed
     */
    @Transactional
    public void createFee(UUID cardId, BigDecimal amount, LocalDate date, Transaction.transactionType feeType){
        if(amount == null){
            throw new ValidationException("Fee Amount is required");
        }
        if(amount.signum() <= 0){
            throw new ValidationException("Fee Amount must be > 0");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));

        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setCard(card);
        tx.setAmount(amount);
        tx.setTransactionDate(date);
        tx.setMerchantName(feeType.toString());
        tx.setTransactionType(feeType);
        tx.setStatus(Transaction.Status.PENDING);

        BigDecimal newBalance = cardService.applyFee(cardId, amount);
        tx.setStatus(Transaction.Status.SENT);
        log.debug("Transaction FEE created: cardId={}, newBalance={}, amount={}", cardId, newBalance, amount);

        transactionRepository.save(tx);
    }



    // Fetches transaction from the transaction ID
    @Transactional(readOnly = true)
    public CreateTransactionResponseDTO getById(UUID transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
        return transactionMapper.toResponse(tx);
    }

    // Fetches transactions from the Card ID
    @Transactional(readOnly = true)
    public List<CreateTransactionResponseDTO> listByCard(UUID cardId) {
        return transactionRepository.findByCardCardId(cardId).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    // Fetches transaction from its cycleID
    @Transactional(readOnly = true)
    public List<CreateTransactionResponseDTO> listByCycle(UUID cycleId) {
        return transactionRepository.findByBillingCycleCycleId(cycleId).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    // Fetches transactions based on Card ID and start/end dates
    @Transactional(readOnly = true)
    public List<CreateTransactionResponseDTO> listByCardAndDateRange(UUID cardId, LocalDate start, LocalDate end) {
        return transactionRepository.findByCardCardIdAndTransactionDateBetween(cardId, start, end).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    // Fetches transactions based on Card ID and Billing Cycle ID
    @Transactional(readOnly = true)
    public List<CreateTransactionResponseDTO> listByCardAndBillingCycle(UUID cardId, UUID billingCycleId){
        return transactionRepository.findByCardCardIdAndBillingCycleCycleId(cardId, billingCycleId).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    // Exceptions
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String msg) { super(msg); }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String msg) { super(msg); }
    }

    public enum InterestType{
        CARDBALANCE,
        CASHADVANCE
    }
}