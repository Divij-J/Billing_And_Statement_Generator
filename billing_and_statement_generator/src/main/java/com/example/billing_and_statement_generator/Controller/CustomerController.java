package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerIdDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.dto.UpdateCustomerRequestDTO;
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

  @PostMapping("/createCustomer")
  @Operation(
    summary = "Create a new customer",
          description = "Creates a new customer and returns the generated customer details"
  )
          public ResponseEntity<CustomerResponseDTO> createCustomer(
      @Valid @RequestBody CreateCustomerRequestDTO request) {
    log.info("POST /api/customers/createCustomer - request received for email={}",
                      request.getEmail());
    CustomerResponseDTO response = customerService.createCustomer(request);
    log.info("POST /api/customers/createCustomer - customer created id={}",
                      response.getCustomerId());
    return ResponseEntity.status(201).body(response);
  }

    @PostMapping("/getCustomerById")
    @Operation(
            summary = "Get customer by ID",
            description = "Retrieves the customer profile associated with the given customer ID"
    )
    public ResponseEntity<CustomerResponseDTO> getCustomer(
            @Valid @RequestBody CustomerIdDTO customerId) {
        log.info("POST /api/customers/getCustomerById - request received for customerId={}", customerId.getCustomerId());
        CustomerResponseDTO response = customerService.getCustomer(customerId.getCustomerId());
        log.info("POST /api/customers/getCustomerById - successfully retrieved customerId={}", customerId.getCustomerId());
        return ResponseEntity.ok(response);
    }

  @PutMapping("/updateCustomer")
  @Operation(
    summary = "Update customer by ID",
          description = "Updates the customer profile associated with the given customer ID and returns the updated details"
  )
          public ResponseEntity<CustomerResponseDTO> updateCustomer(
          @Valid @RequestBody UpdateCustomerRequestDTO request) {
            UUID customerId = request.getCustomerId();
            CreateCustomerRequestDTO updateData = request.getUpdateData();

      log.info("PUT /api/customers/updateCustomer - request received for customerId{}", customerId);
    CustomerResponseDTO response =
      customerService.updateCustomer(customerId, updateData);
    log.info("PUT /api/customers/updateCustomer - successfully updated customerId={}", customerId);
    return ResponseEntity.ok(response);
  }
}