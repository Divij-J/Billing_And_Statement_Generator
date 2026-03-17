package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  public ResponseEntity<CustomerResponseDTO> createCustomer(
      @Valid @RequestBody CreateCustomerRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(customerService.createCustomer(request));
  }

  @GetMapping("/{customerId}")
  public ResponseEntity<CustomerResponseDTO> getCustomer(
      @PathVariable UUID customerId) {
    return ResponseEntity.ok(customerService.getCustomer(customerId));
  }

  @PutMapping("/{customerId}")
  public ResponseEntity<CustomerResponseDTO> updateCustomer(
      @PathVariable UUID customerId,
      @Valid @RequestBody CreateCustomerRequestDTO request) {
    return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
  }
}