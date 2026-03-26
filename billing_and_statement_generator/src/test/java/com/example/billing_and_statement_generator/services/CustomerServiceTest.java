package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.mapper.CustomerMapper;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private CreateCustomerRequestDTO request;
    private Customer customer;
    private CustomerResponseDTO response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        request = new CreateCustomerRequestDTO();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setMiddleInitial("A");
        request.setEmail("john@example.com");
        request.setPhoneNumber("1234567890");
        request.setPhoneType("MOBILE");
        request.setAddress1("123 test st");
        request.setCity("Chicago");
        request.setState("IL");
        request.setZipcode("60601");

        customer = Customer.builder()
                .customerId(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .middleInitial("A")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .phoneType(Customer.PhoneType.MOBILE)
                .city("Chicago")
                .state("IL")
                .zipcode("60601")
                .build();
        response = new CustomerResponseDTO();
        response.setCustomerId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setMiddleInitial(customer.getMiddleInitial());
        response.setEmail(customer.getEmail());
        response.setPhoneNumber(customer.getPhoneNumber());
        response.setPhoneType(customer.getPhoneType().name());
        response.setAddress1(customer.getAddress1());
        response.setAddress2(customer.getAddress2());
        response.setCity(customer.getCity());
        response.setState(customer.getState());
        response.setZipcode(customer.getZipcode());
    }

    // ==========================================================================
    // CREATE CUSTOMER TESTS
    // ==========================================================================
    @Test
    void createCustomer_shouldCreateAndReturnResponse() {

        // Build response DTO using setters
        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setMiddleInitial(customer.getMiddleInitial());
        response.setEmail(customer.getEmail());
        response.setPhoneNumber(customer.getPhoneNumber());
        response.setPhoneType(customer.getPhoneType().name());
        response.setAddress1(customer.getAddress1());
        response.setAddress2(customer.getAddress2());
        response.setCity(customer.getCity());
        response.setState(customer.getState());
        response.setZipcode(customer.getZipcode());

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(customerRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(customerMapper.toDTO(customer)).thenReturn(response);

        CustomerResponseDTO result = customerService.createCustomer(request);

        assertNotNull(result);
        assertEquals(customer.getCustomerId(), result.getCustomerId());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_shouldThrowConflict_whenEmailExists() {
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> customerService.createCustomer(request));
    }

    @Test
    void createCustomer_shouldThrowConflict_whenPhoneExists() {
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(customerRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> customerService.createCustomer(request));
    }

    @Test
    void createCustomer_shouldThrowRuntimeExceptionOnSaveFailure() {
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(customerRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(customerRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> customerService.createCustomer(request));

        assertTrue(ex.getMessage().contains("Failed to process customer request"));
    }

    // ==========================================================================
    // GET CUSTOMER TESTS
    // ==========================================================================

    @Test
    void getCustomer_shouldReturnCustomer() {
        UUID id = customer.getCustomerId();

        // Build response DTO using setters (because your DTO has no constructors)
        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setEmail(customer.getEmail());

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerMapper.toDTO(customer)).thenReturn(response);

        CustomerResponseDTO dto = customerService.getCustomer(id);

        assertNotNull(dto);
        assertEquals(id, dto.getCustomerId());
        assertEquals(customer.getFirstName(), dto.getFirstName());
        assertEquals(customer.getLastName(), dto.getLastName());
        assertEquals(customer.getEmail(), dto.getEmail());
    }

    @Test
    void getCustomer_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> customerService.getCustomer(id));
    }

    // ==========================================================================
    // UPDATE CUSTOMER TESTS
    // ==========================================================================

    @Test
    void updateCustomer_shouldUpdateAndReturnResponse() {
        UUID id = customer.getCustomerId();

        // Build response DTO
        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setMiddleInitial(customer.getMiddleInitial());
        response.setEmail(customer.getEmail());
        response.setPhoneNumber(customer.getPhoneNumber());
        response.setPhoneType(customer.getPhoneType().name());
        response.setAddress1(customer.getAddress1());
        response.setAddress2(customer.getAddress2());
        response.setCity(customer.getCity());
        response.setState(customer.getState());
        response.setZipcode(customer.getZipcode());

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerMapper.updateEntityFromRequest(request, customer)).thenReturn(customer);
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toDTO(customer)).thenReturn(response);

        CustomerResponseDTO result = customerService.updateCustomer(id, request);

        assertNotNull(result);
        assertEquals(id, result.getCustomerId());
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomer_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> customerService.updateCustomer(id, request));
    }

    @Test
    void updateCustomer_shouldThrowRuntimeExceptionOnSaveError() {
        UUID id = customer.getCustomerId();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerMapper.updateEntityFromRequest(request, customer)).thenReturn(customer);
        when(customerRepository.save(customer)).thenThrow(new RuntimeException("Save failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> customerService.updateCustomer(id, request));

        assertTrue(ex.getMessage().contains("Failed to update customer"));
    }
}