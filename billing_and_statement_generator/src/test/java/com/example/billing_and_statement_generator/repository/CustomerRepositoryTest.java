package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private Customer customer;

    @BeforeEach
    void setup() {
        customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setMiddleInitial("A");
        customer.setEmail("john@example.com");
        customer.setPhoneNumber("1234567890");
        customer.setPhoneType(Customer.PhoneType.valueOf("MOBILE"));
        customer.setAddress1("123 Test St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");

        customerRepository.save(customer);
    }

    // ---------------------------------------------------------
    // TEST: findByEmail
    // ---------------------------------------------------------
    @Test
    void findByEmail_shouldReturnCustomer() {
        Optional<Customer> result = customerRepository.findByEmail("john@example.com");

        assertTrue(result.isPresent());
        assertEquals(customer.getEmail(), result.get().getEmail());
    }

    @Test
    void findByEmail_shouldReturnEmptyForUnknownEmail() {
        Optional<Customer> result = customerRepository.findByEmail("unknown@example.com");
        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------
    // TEST: existsByEmail
    // ---------------------------------------------------------
    @Test
    void existsByEmail_shouldReturnTrueWhenExists() {
        assertTrue(customerRepository.existsByEmail("john@example.com"));
    }

    @Test
    void existsByEmail_shouldReturnFalseWhenNotExists() {
        assertFalse(customerRepository.existsByEmail("nobody@example.com"));
    }

    // ---------------------------------------------------------
    // TEST: existsByPhoneNumber
    // ---------------------------------------------------------
    @Test
    void existsByPhoneNumber_shouldReturnTrueWhenExists() {
        assertTrue(customerRepository.existsByPhoneNumber("1234567890"));
    }

    @Test
    void existsByPhoneNumber_shouldReturnFalseWhenNotExists() {
        assertFalse(customerRepository.existsByPhoneNumber("0000000000"));
    }
}
