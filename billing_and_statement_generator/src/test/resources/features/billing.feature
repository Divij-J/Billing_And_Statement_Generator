Feature: Billing Cycle Processing
  As a credit card system
  I want to generate billing cycles correctly
  So that balances, fees, and interest are calculated accurately

  Background:
    Given a billing customer exists
    And a billing card exists with credit limit of 10000
    And the billing card has zero balances

  Scenario: Generate first billing cycle with cash advance
    Given the billing card has a cash advance of 10
    When a billing cycle is generated
    Then the billing cycle should be created
    And the total cash advance should be 10
    And a cash advance fee should be applied
    And interest should not be charged

  Scenario: Generate second billing cycle with unpaid balance
    Given a previous billing cycle exists with outstanding balance of 10.20
    And the billing card has a cash advance of 11
    When a billing cycle is generated
    Then interest should be charged
    And the total cash advance should be 11
    And a cash advance fee should be applied

  Scenario: Late fee applied when payment is overdue
    Given a previous billing cycle exists and is past due with balance 100
    When a billing cycle is generated
    Then a late fee should be applied