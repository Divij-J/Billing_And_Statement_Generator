package com.example.billing_and_statement_generator.service;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final CustomerMapper customerMapper;

  public CustomerResponseDTO createCustomer(CreateCustomerRequestDTO request) {
    log.info("Creating customer with email: {}", request.getEmail());

    if (customerRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Customer with email already exists: " + request.getEmail());
    }
    if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
      throw new IllegalArgumentException("Customer with phone number already exists: " + request.getPhoneNumber());
    }

    Customer customer = Customer.builder()
        .customerId(UUID.randomUUID())
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .middleInitial(request.getMiddleInitial())
        .email(request.getEmail())
        .phoneNumber(request.getPhoneNumber())
        .phoneType(Customer.PhoneType.valueOf(request.getPhoneType().toUpperCase()))
        .address1(request.getAddress1())
        .address2(request.getAddress2())
        .city(request.getCity())
        .state(request.getState())
        .zipcode(request.getZipcode())
        .build();

    Customer saved = customerRepository.save(customer);
    log.info("Customer created with ID: {}", saved.getCustomerId());
    return customerMapper.toDTO(saved);
  }

  public CustomerResponseDTO getCustomer(UUID customerId) {
    log.info("Fetching customer with ID: {}", customerId);
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    return toResponseDTO(customer);
  }

  public CustomerResponseDTO updateCustomer(UUID customerId, CreateCustomerRequestDTO request) {
    log.info("Updating customer with ID: {}", customerId);
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

    customer.setFirstName(request.getFirstName());
    customer.setLastName(request.getLastName());
    customer.setMiddleInitial(request.getMiddleInitial());
    customer.setEmail(request.getEmail());
    customer.setPhoneNumber(request.getPhoneNumber());
    customer.setPhonetype(Customer.PhoneType.valueOf(request.getPhoneType().toUpperCase()));
    customer.setAddress1(request.getAddress1());
    customer.setAddress2(request.getAddress2());
    customer.setCity(request.getCity());
    customer.setState(request.getState());
    customer.setZipcode(request.getZipcode());

    Customer updated = customerRepository.save(customer);
    return toResponseDTO(updated);
  }

  private CustomerResponseDTO toResponseDTO(Customer c) {
    return CustomerResponseDTO.builder()
        .customerId(c.getCustomerId())
        .firstName(c.getFirstName())
        .lastName(c.getLastName())
        .middleInitial(c.getMiddleInitial())
        .email(c.getEmail())
        .phoneNumber(c.getPhoneNumber())
        .phoneType(c.getPhonetype() != null ? c.getPhonetype().name() : null)
        .address1(c.getAddress1())
        .address2(c.getAddress2())
        .city(c.getCity())
        .state(c.getState())
        .zipcode(c.getZipcode())
        .build();
  }
}