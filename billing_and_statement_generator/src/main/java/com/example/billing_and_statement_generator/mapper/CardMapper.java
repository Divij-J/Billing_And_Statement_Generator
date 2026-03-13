package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.entity.*;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CardMapper {
    // DTO to Entity (Create Request DTO)
    @Mappings({
            // service will set these (the ignore mappings)
            @Mapping(target = "cardId", ignore = true),
            @Mapping(target = "cardBalance", ignore = true),
            @Mapping(target = "isActive", ignore = true),

            @Mapping(target = "customer", source = "customerId", qualifiedByName = "toCustomer")
    })
    Card toEntity(CreateCardRequestDTO dto);

    // Entity to DTO (Response DTO)
    @Mappings({
            @Mapping(target = "customer", source = "customer.customerId")
    })
    CreateCardResponseDTO toResponse(Card card);

    // Helper method to make a customer with only its ID, ensures mapper does not interact with DB
    @Named("toCustomer")
    default Customer toCustomer(UUID id){
        if(id == null)
            return null;
        return Customer.builder()
                .customerId(id)
                .build();
    }
}
