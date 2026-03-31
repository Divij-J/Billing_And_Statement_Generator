package com.example.billing_and_statement_generator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerRequestDTO {

    @NotNull
    private UUID customerId;

    @Valid
    @NotNull
    private CreateCustomerRequestDTO updateData;

}
