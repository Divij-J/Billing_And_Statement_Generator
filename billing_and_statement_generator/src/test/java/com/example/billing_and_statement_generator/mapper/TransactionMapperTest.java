package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.Transaction;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionMapperTest {
    private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    // Testing DTO to Entity
    @Test
    void testTransactionDTOtoEntity(){
        CreateTransactionRequestDTO dto = CreateTransactionRequestDTO.builder()
                .cardId(UUID.randomUUID())
                .transactionDate(LocalDate.now())
                .amount(BigDecimal.valueOf(50))
                .merchantName("Test Transaction")
                .transactionType(Transaction.transactionType.PURCHASE)
                .build();

        Transaction entity = mapper.toEntity(dto);

        // Assert server-controlled fields are null
        assertNull(entity.getTransactionId(), "Transaction DTO to Entity: Transaction ID should be null");
        assertNull(entity.getStatus(), "Transaction DTO to Entity: Status should be null");
        assertNull(entity.getBillingCycle(), "Transaction DTO to Entity: Billing Cycle should be null");

        // Assert DTO-controlled fields match
        assertEquals(entity.getCard().getCardId(),dto.getCardId(), " Transaction DTO to Entity: Card ID's don't match");
        assertEquals(entity.getTransactionDate(), dto.getTransactionDate(), "Transaction DTO to Entity: Dates don't match");
        assertEquals(entity.getAmount(), dto.getAmount(), "Transaction DTO to Entity: Amounts don't match");
        assertEquals(entity.getMerchantName(), dto.getMerchantName(), "Transaction DTO to Entity: Merchant Names don't match");
        assertEquals(entity.getTransactionType(), dto.getTransactionType(), "Transaction DTO to Entity: Transaction Types don't match");
    }

    // Testing Entity to DTO
    @Test
    void testTransactionEntityToDto(){
        Transaction entity = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .transactionDate(LocalDate.now())
                .transactionType(Transaction.transactionType.PURCHASE)
                .amount(BigDecimal.valueOf(100))
                .merchantName("Test")
                .status(Transaction.Status.SENT)
                .build();

        CreateTransactionResponseDTO dto = mapper.toResponse(entity);

        // Assert server-controlled fields
        assertNull(dto.getCardId(), "Transaction Entity to DTO: Card ID should be null");
        assertNull(dto.getCycleId(), "Transaction Entity to DTO: Billing Cycle ID should be null");

        // Assert DTO-controlled fields match
        assertEquals(dto.getTransactionId(), entity.getTransactionId(), "Transaction Entity to DTO: Transaction ID don't match");
        assertEquals(dto.getTransactionDate(), entity.getTransactionDate(), "Transaction Entity to DTO: Transaction Dates don't match");
        assertEquals(dto.getTransactionType(), entity.getTransactionType(), "Transaction Entity to DTO: Transaction Types don't match");
        assertEquals(dto.getAmount(), entity.getAmount(), "Transaction Entity to DTO: Transaction Amount don't match");
        assertEquals(dto.getMerchantName(), entity.getMerchantName(),"Transaction Entity to DTO: Transaction Merchant Names don't match");
        assertEquals(dto.getStatus(), entity.getStatus(), "Transaction Entity to DTO: Transaction Status don't match");

    }
}
