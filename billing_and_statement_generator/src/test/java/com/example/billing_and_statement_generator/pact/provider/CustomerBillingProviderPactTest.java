package com.example.billing_and_statement_generator.pact.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.example.billing_and_statement_generator.config.TestSecurityConfig;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * PROVIDER PACT TEST — CustomerServiceProvider
 *
 * Verifies pacts published by BillingServiceConsumer.
 * Uses TestSecurityConfig to bypass authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(TestSecurityConfig.class)
@Provider("CustomerServiceProvider")
@PactFolder("src/test/resources/pacts")
@IgnoreNoPactsToVerify
public class CustomerBillingProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    private static final UUID CUSTOMER_ID =
            UUID.fromString("a1111111-b222-c333-d444-e55555555555");

    // ---------------------------------------------------------------------
    // JUnit–Pact wiring
    // ---------------------------------------------------------------------

    @BeforeEach
    void setupTarget(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPacts(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // ---------------------------------------------------------------------
    // PROVIDER STATES
    // ---------------------------------------------------------------------

    /**
     * Matches:
     * given("customer exists")
     */
    @State("customer exists")
    public void customerExists() {
        cleanAll();

        Customer customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setEmail("jane.doe@test.com");
        customer.setPhoneNumber("1234567890");
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Main St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");

//        customerRepository.save(customer);
        customerRepository.saveAndFlush(customer);
    }

    @State("customer exists with id")
    public void customerExistsWithId() {
        cleanAll();

        Customer customer = new Customer();
        customer.setCustomerId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setEmail("jane.doe@test.com");
        customer.setPhoneNumber("1234567890");
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Main St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");

        customerRepository.saveAndFlush(customer);

        System.out.println(
                "TEST DEBUG: customer exists? " +
                        customerRepository.findById(customer.getCustomerId()).isPresent()
        );

    }

    /**
     * Matches:
     * given("customer does not exist")
     */
    @State("customer does not exist")
    public void customerDoesNotExist() {
        cleanAll();
        // No-op: repository intentionally empty
    }

    // ---------------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------------

    private void cleanAll() {
        customerRepository.deleteAll();
    }
}
