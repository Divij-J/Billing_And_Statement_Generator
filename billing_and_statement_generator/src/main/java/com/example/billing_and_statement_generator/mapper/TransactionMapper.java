package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Transaction;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    // DTO to Entity (Create Request DTO)
    @Mappings({
        // service will set these (the ignore mappings)
        @Mapping(target = "transactionId", ignore = true),
        @Mapping(target = "billingCycle", ignore = true),
        @Mapping(target = "status", ignore = true),

        @Mapping(target = "card", source = "cardId", qualifiedByName = "toCard"),
        @Mapping(target = "transactionType", source = "type")
    })
    Transaction toEntity(CreateTransactionRequestDTO dto);

    // Entity to DTO (Response DTO)
    @Mappings({
        @Mapping(target = "cardId", source = "card.cardId"),
        @Mapping(target = "cycleId", source = "billingCycle.cycle_id"),
        @Mapping(target = "type", source = "transactionType")
    })
    CreateTransactionResponseDTO toResponse(Transaction transaction);

    // Helper method to make a card with only its card ID, ensures mapper does not interact with DB
    @Named("toCard")
    default Card toCard(UUID cardId) {
        if (cardId == null) return null;
        return Card.builder()
                .cardId(cardId)
                .build();
    }
}
