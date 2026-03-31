package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.card.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.card.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CardMapper {
    // DTO to Entity (Create Request DTO), ignore server-controlled fields
    @Mappings({
            @Mapping(target = "cardId", ignore = true),
            @Mapping(target = "cardIssueDate", ignore = true),
            @Mapping(target = "expiryDate", ignore = true),
            @Mapping(target = "active", ignore = true),
            @Mapping(target = "cardBalance", ignore = true),
            @Mapping(target = "cashAdvanceBalance", ignore = true),
            @Mapping(target = "creditLimit", ignore = true),
            @Mapping(target = "annualInterestRate", ignore = true),
            @Mapping(target = "billingCycleDate", ignore = true),
            @Mapping(target = "lateFeeAmount", ignore = true),
            @Mapping(target = "cashAdvanceFeeRate", ignore = true),
            @Mapping(target = "cashAdvanceAPR", ignore = true),
            @Mapping(target = "annualMembershipFee", ignore = true),
            @Mapping(target = "cashAdvanceLimit", ignore = true),
            @Mapping(target = "minimumDue", ignore = true),

            @Mapping(target = "customer.customerId", source = "customerId")
    })
    Card toEntity(CreateCardRequestDTO dto);

    // Entity to DTO (Response DTO)
    @Mappings({
            @Mapping(target = "customerId", source = "customer.customerId")
    })
    CreateCardResponseDTO toResponse(Card card);
}
