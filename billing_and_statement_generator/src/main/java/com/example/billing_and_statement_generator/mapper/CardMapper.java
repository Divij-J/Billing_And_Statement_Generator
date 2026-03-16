package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CardMapper {
    // DTO to Entity (Create Request DTO)
    @Mappings({
            // service will set these (the ignore mappings)
            @Mapping(target = "cardId", ignore = true),
            @Mapping(target = "cardBalance", ignore = true),
            @Mapping(target = "isActive", ignore = true),

            @Mapping(target = "customer.customerId", source = "customerId")
    })
    Card toEntity(CreateCardRequestDTO dto);

    // Entity to DTO (Response DTO)
    @Mappings({
            @Mapping(target = "customerId", source = "customer.customerId")
    })
    CreateCardResponseDTO toResponse(Card card);
}
