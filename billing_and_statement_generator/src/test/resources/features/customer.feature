Feature: Customer Management
  As a billing system
  I want to manage customers
  So that customers can be created, retrieved, and validated correctly

  Background:
    Given a customer exists with valid details

  Scenario: Create a customer successfully
    When a customer is created with valid details
    Then the customer should be saved successfully
    And the customer email should be returned

  Scenario: Reject customer creation with missing required fields
    When a customer is created with missing required fields
    Then the customer creation should fail with an error

  Scenario: Retrieve an existing customer
    Given a customer exists in the system
    When the customer is retrieved by id
    Then the customer details should be returned