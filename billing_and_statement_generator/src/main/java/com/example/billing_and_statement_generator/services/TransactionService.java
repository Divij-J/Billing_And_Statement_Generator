package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateTransactionResponseDTO;

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

    /** Transaction Creation
     * Create a transaction and apply its effect to the card balance.
     * PURCHASE / CASHADVANCE: increases balance (limit checks in CardService)
     * PAYMENT: decreases balance (overpayment rejected in CardService)
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

        // Apply the financial impact using CardService
        BigDecimal amount = dto.getAmount();
        try {
            switch (dto.getType()) {
                case PURCHASE -> {
                    BigDecimal newBal = cardService.applyPurchase(cardId, amount);
                    log.debug("Transaction PURCHASE applied: cardId={}, amount={}, newBalance={}", cardId, amount, newBal);
                }
                case CASHADVANCE -> {
                    BigDecimal newBal = cardService.applyCashAdvance(cardId, amount);
                    log.debug("Transaction CASHADVANCE applied: cardId={}, amount={}, newBalance={}", cardId, amount, newBal);
                }
                case PAYMENT -> {
                    BigDecimal newBal = cardService.applyPayment(cardId, amount);
                    log.debug("Transaction PAYMENT applied: cardId={}, amount={}, newBalance={}", cardId, amount, newBal);
                }
                default -> throw new ValidationException("Unsupported transaction type: " + dto.getType());
            }

            // Mark SENT after successful balance application
            tx.setStatus(Transaction.Status.SENT);
        }
        // Handle declined transactions and save to DB
        catch (CardService.LimitExceededException e) {
            log.warn("TX_LIMIT_EXCEEDED cardId={} type={} amount={} msg={}",
                    cardId, dto.getType(), dto.getAmount(), e.getMessage());
            tx.setStatus(Transaction.Status.DECLINED);
            Transaction saved = transactionRepository.save(tx);
            throw e;
        } catch (CardService.ValidationException e) {
            log.warn("TX_VALIDATION_FAILED cardId={} type={} amount={} msg={}",
                    cardId, dto.getType(), dto.getAmount(), e.getMessage());
            tx.setStatus(Transaction.Status.DECLINED);
            Transaction saved = transactionRepository.save(tx);
            throw e;
        }

        // Save successful transaction to DB
        Transaction saved = transactionRepository.save(tx);
        return transactionMapper.toResponse(saved);
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
}