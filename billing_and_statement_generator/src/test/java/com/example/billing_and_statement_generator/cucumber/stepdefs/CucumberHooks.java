package com.example.billing_and_statement_generator.cucumber.stepdefs;

import com.example.billing_and_statement_generator.repository.*;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CucumberHooks — single place for all Cucumber lifecycle hooks.
 * Having @Before in multiple step definition classes causes them to run multiple times per scenario, leading to constraint violations.
 * All cleanup is centralized here instead.
 */
public class CucumberHooks {

    @Autowired private StatementRepository statementRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BillingCycleRepository billingCycleRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private CustomerRepository customerRepository;

    @Before(order = 1)
    public void cleanDatabase() {
        statementRepository.deleteAll();
        paymentRepository.deleteAll();
        transactionRepository.deleteAll();
        billingCycleRepository.deleteAll();
        cardRepository.deleteAll();
        customerRepository.deleteAll();
    }
}