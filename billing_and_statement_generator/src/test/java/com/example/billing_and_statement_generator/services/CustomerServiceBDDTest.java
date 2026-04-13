package com.example.billing_and_statement_generator.services;


import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.mapper.CustomerMapper;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CustomerServiceBDDTest {

    @Mock
    CustomerRepository customerRepository;

    @Mock
    CustomerMapper customerMapper;

    @InjectMocks
    CustomerService customerService;

    private UUID customerId;
    private Customer customer;
    private CreateCustomerRequestDTO request;
    private CustomerResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        customerId = UUID.randomUUID();

        customer = Customer.builder()
                .customerId(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .phoneNumber("1234567890")
                .phoneType(Customer.PhoneType.MOBILE)
                .build();

        request = new CreateCustomerRequestDTO();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@test.com");
        request.setPhoneNumber("1234567890");
        request.setPhoneType("MOBILE");

        responseDTO = new CustomerResponseDTO();
        responseDTO.setCustomerId(customerId);
        responseDTO.setEmail("john.doe@test.com");
    }

    // -------------------- createCustomer --------------------

    @Test
    void shouldCreateCustomerSuccessfully() {
        // GIVEN
        given(customerRepository.existsByEmail(request.getEmail()))
                .willReturn(false);
        given(customerRepository.existsByPhoneNumber(request.getPhoneNumber()))
                .willReturn(false);
        given(customerRepository.save(any(Customer.class)))
                .willReturn(customer);
        given(customerMapper.toDTO(customer))
                .willReturn(responseDTO);

        // WHEN
        CustomerResponseDTO result =
                customerService.createCustomer(request);

        // THEN
        assertEquals(customerId, result.getCustomerId());
        then(customerRepository).should().save(any(Customer.class));
        then(customerMapper).should().toDTO(customer);
    }

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        // GIVEN
        given(customerRepository.existsByEmail(request.getEmail()))
                .willReturn(true);

        // WHEN / THEN
        assertThrows(DataIntegrityViolationException.class,
                () -> customerService.createCustomer(request));

        then(customerRepository).should(never()).save(any());
    }

    @Test
    void shouldFailWhenPhoneNumberAlreadyExists() {
        // GIVEN
        given(customerRepository.existsByEmail(request.getEmail()))
                .willReturn(false);
        given(customerRepository.existsByPhoneNumber(request.getPhoneNumber()))
                .willReturn(true);

        // WHEN / THEN
        assertThrows(DataIntegrityViolationException.class,
                () -> customerService.createCustomer(request));

        then(customerRepository).should(never()).save(any());
    }

    // -------------------- getCustomer --------------------

    @Test
    void shouldReturnCustomerWhenFound() {
        // GIVEN
        given(customerRepository.findById(customerId))
                .willReturn(Optional.of(customer));
        given(customerMapper.toDTO(customer))
                .willReturn(responseDTO);

        // WHEN
        CustomerResponseDTO result =
                customerService.getCustomer(customerId);

        // THEN
        assertEquals(customerId, result.getCustomerId());
        then(customerMapper).should().toDTO(customer);
    }

    @Test
    void shouldFailWhenCustomerNotFound() {
        // GIVEN
        given(customerRepository.findById(customerId))
                .willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class,
                () -> customerService.getCustomer(customerId));

        then(customerMapper).shouldHaveNoInteractions();
    }

    // -------------------- updateCustomer --------------------

    @Test
    void shouldUpdateCustomerSuccessfully() {
        // GIVEN
        given(customerRepository.findById(customerId))
                .willReturn(Optional.of(customer));
        given(customerMapper.updateEntityFromRequest(request, customer))
                .willReturn(customer);
        given(customerRepository.save(customer))
                .willReturn(customer);
        given(customerMapper.toDTO(customer))
                .willReturn(responseDTO);

        // WHEN
        CustomerResponseDTO result =
                customerService.updateCustomer(customerId, request);

        // THEN
        assertEquals(customerId, result.getCustomerId());
        then(customerRepository).should().save(customer);
    }

    @Test
    void shouldFailUpdateWhenCustomerNotFound() {
        // GIVEN
        given(customerRepository.findById(customerId))
                .willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class,
                () -> customerService.updateCustomer(customerId, request));

        then(customerRepository).should(never()).save(any());
    }
}

