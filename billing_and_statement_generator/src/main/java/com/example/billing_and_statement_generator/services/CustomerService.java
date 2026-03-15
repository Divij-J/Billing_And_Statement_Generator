package com.example.billing_and_statement_generator.service;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.mapper.CustomerMapper;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final CustomerMapper customerMapper;

  public CustomerResponseDTO createCustomer(CreateCustomerRequestDTO request) {
    log.info("customers - creating customer with email={}", request.getEmail());
    try {
      if (customerRepository.existsByEmail(request.getEmail())) {
        throw new DataIntegrityViolationException(
                                  "Customer already exists with email: " + request.getEmail());
      }
      if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
        throw new DataIntegrityViolationException(
                                  "Customer already exists with phone: " + request.getPhoneNumber());
      }

      Customer customer = Customer.builder()
          .customerId(UUID.randomUUID())
          .firstName(request.getFirstName())
          .lastName(request.getLastName())
          .middleInitial(request.getMiddleInitial())
          .email(request.getEmail())
          .phoneNumber(request.getPhoneNumber())
          .phoneType(Customer.PhoneType.valueOf(
                                request.getPhoneType().toUpperCase()))
          .address1(request.getAddress1())
          .address2(request.getAddress2())
          .city(request.getCity())
          .state(request.getState())
          .zipcode(request.getZipcode())
          .build();

      Customer saved = customerRepository.save(customer);
      log.info("customers - successfully created customer with id={}",
                            saved.getCustomerId());
      return customerMapper.toDTO(saved);

    } catch (DataIntegrityViolationException e) {
      log.error("customers - conflict: {}", e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      log.error("customers - error creating customer: {}", e.getMessage());
      throw new RuntimeException("Failed to process customer request: " + e.getMessage());
    }
  }

  public CustomerResponseDTO getCustomer(UUID customerId) {
    log.info("customers/{} - retrieving customer", customerId);

    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new EntityNotFoundException(
                          "Customer not found with ID: " + customerId));

    log.info("customers/{} - successfully retrieved customer", customerId);
    return customerMapper.toDTO(customer);
  }

  public CustomerResponseDTO updateCustomer(UUID customerId,
                       CreateCustomerRequestDTO request) {
    log.info("PUT /customers/{} - updating customer", customerId);
    try {
      Customer customer = customerRepository.findById(customerId)
          .orElseThrow(() -> new EntityNotFoundException(
                                "Customer not found with ID: " + customerId));

      // MapStruct updates only non-ignored fields on existing entity
      customerMapper.updateEntityFromRequest(request, customer);

      Customer updated = customerRepository.save(customer);
      log.info("customers/{} - successfully updated customer", customerId);
      return customerMapper.toDTO(updated);

    } catch (EntityNotFoundException e) {
      log.error("customers/{} - not found: {}", customerId, e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      log.error("customers/{} - error updating: {}", customerId, e.getMessage());
      throw new RuntimeException("Failed to update customer: " + e.getMessage());
    }
  }
}