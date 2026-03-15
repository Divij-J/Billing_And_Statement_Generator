package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "phoneType", target = "phoneType")
    CustomerResponseDTO toDTO(Customer customer);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "cards", ignore = true)
    @Mapping(target = "phoneType", expression = "java(Customer.PhoneType.valueOf(request.getPhoneType().toUpperCase()))")
    void updateEntityFromRequest(
            CreateCustomerRequestDTO request,
            @MappingTarget Customer customer);
}