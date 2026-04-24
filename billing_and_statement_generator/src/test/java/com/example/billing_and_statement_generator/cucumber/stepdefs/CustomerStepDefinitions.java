package com.example.billing_and_statement_generator.cucumber.stepdefs;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import com.example.billing_and_statement_generator.services.CustomerService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerStepDefinitions {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    private CreateCustomerRequestDTO request;
    private CustomerResponseDTO response;
    private Customer existingCustomer;
    private Exception thrownException;

    @Before
    public void reset() {
        customerRepository.deleteAll();
        request = null;
        response = null;
        existingCustomer = null;
        thrownException = null;
    }

    @Given("a customer exists with valid details")
    public void aCustomerExistsWithValidDetails() {
        request = new CreateCustomerRequestDTO();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("cust." + UUID.randomUUID() + "@example.com");
        request.setPhoneNumber("3125551234");
        request.setPhoneType("MOBILE");
        request.setAddress1("123 Main St");
        request.setCity("Chicago");
        request.setState("IL");
        request.setZipcode("60601");
    }

    // GIVEN
    @Given("a customer exists in the system")
    public void aCustomerExistsInTheSystem() {
        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("existing." + UUID.randomUUID() + "@example.com");
        customer.setPhoneNumber("3125559999");
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("456 Oak St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60602");

        existingCustomer = customerRepository.save(customer);
    }

    // WHEN
    @When("a customer is created with valid details")
    public void aCustomerIsCreatedWithValidDetails() {
        try {
            response = customerService.createCustomer(request);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("a customer is created with missing required fields")
    public void aCustomerIsCreatedWithMissingRequiredFields() {
        request = new CreateCustomerRequestDTO();
        request.setFirstName("Jane");
        // missing lastName, email, address, etc.
        try {
            response = customerService.createCustomer(request);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the customer is retrieved by id")
    public void theCustomerIsRetrievedById() {
        try {
            response = customerService.getCustomer(existingCustomer.getCustomerId());
        } catch (Exception e) {
            thrownException = e;
        }
    }

    // THEN
    @Then("the customer should be saved successfully")
    public void theCustomerShouldBeSavedSuccessfully() {
        assertNull(thrownException, "Did not expect an exception");
        assertNotNull(response);
        assertNotNull(response.getCustomerId());

        assertTrue(
                customerRepository.findById(response.getCustomerId()).isPresent(),
                "Customer was not persisted"
        );
    }

    @Then("the customer email should be returned")
    public void theCustomerEmailShouldBeReturned() {
        assertNotNull(response.getEmail());
        assertTrue(response.getEmail().contains("@"));
    }

    @Then("the customer creation should fail with an error")
    public void theCustomerCreationShouldFailWithAnError() {
        assertNotNull(thrownException, "Expected validation error");
        assertNull(response, "Response should be null on failure");
    }

    @Then("the customer details should be returned")
    public void theCustomerDetailsShouldBeReturned() {
        assertNull(thrownException);
        assertNotNull(response);
        assertEquals(existingCustomer.getCustomerId(), response.getCustomerId());
        assertEquals(existingCustomer.getEmail(), response.getEmail());
    }
}
