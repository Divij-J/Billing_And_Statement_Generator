package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "phoneType", target = "phoneType", qualifiedByName = "phoneTypeToString")
    CustomerResponseDTO toDTO(Customer customer);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "cards", ignore = true)
    @Mapping(target = "phoneType", expression = "java(Customer.PhoneType.valueOf(request.getPhoneType().toUpperCase()))")
    Customer updateEntityFromRequest(
            CreateCustomerRequestDTO request,
            @MappingTarget Customer customer);

    @Named("phoneTypeToString")
    default String phoneTypeToString(Customer.PhoneType phoneType){
        return phoneType !=null ? phoneType.name() : null;
    }
}