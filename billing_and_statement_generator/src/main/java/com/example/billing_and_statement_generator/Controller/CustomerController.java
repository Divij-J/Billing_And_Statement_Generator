package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/customers")
@Tag(name = "Customer", description = "APIs for managing customers")
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  @Operation(
    summary = "Create a new customer",
                security = @SecurityRequirement(name = "bearerAuth"),
          description = "Creates a new customer and returns the generated customer details"
  )
          public ResponseEntity<CustomerResponseDTO> createCustomer(
      @Valid @RequestBody CreateCustomerRequestDTO request) {
    log.info("POST /api/customers - request received for email={}",
                      request.getEmail());
    CustomerResponseDTO response = customerService.createCustomer(request);
    log.info("POST /api/customers - customer created id={}",
                      response.getCustomerId());
    return ResponseEntity.status(201).body(response);
  }

    @PostMapping("/{customerId}")
    @Operation(
            summary = "Get customer by ID",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Retrieves the customer profile associated with the given customer ID"
    )
    public ResponseEntity<CustomerResponseDTO> getCustomer(
            @PathVariable UUID customerId) {
        log.info("POST /api/customers/{} - request received", customerId);
        CustomerResponseDTO response = customerService.getCustomer(customerId);
        log.info("POST /api/customers/{} - successfully retrieved", customerId);
        return ResponseEntity.ok(response);
    }

  @PutMapping("/{customerId}")
  @Operation(
    summary = "Update customer by ID",
                security = @SecurityRequirement(name = "bearerAuth"),
          description = "Updates the customer profile associated with the given customer ID and returns the updated details"
  )
          public ResponseEntity<CustomerResponseDTO> updateCustomer(
      @PathVariable UUID customerId,
      @Valid @RequestBody CreateCustomerRequestDTO request) {
    log.info("PUT /api/customers/{} - request received", customerId);
    CustomerResponseDTO response =
      customerService.updateCustomer(customerId, request);
    log.info("PUT /api/customers/{} - successfully updated", customerId);
    return ResponseEntity.ok(response);
  }
}