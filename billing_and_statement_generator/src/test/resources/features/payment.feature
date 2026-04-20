Feature: Payment Processing
 As a credit card system
 I want to process payments correctly
 So that card balances are updated accurately and payment types are determined by the server

 Background:
  Given a payment customer exists in the system
  And a payment card exists with a credit limit of 5000.00
  And the payment card has a balance of 1000.00 and cash advance balance of 0.00
  And the payment card has a minimum due of 100.00

 Scenario: Process a partial payment successfully
  When a payment of 500.00 is made using ONLINE method
  Then the payment should be saved successfully
  And the payment type should be PARTIAL
  And the payment status should be SUCCESS
  And the card total balance should be 500.00

 Scenario: Process a full payment successfully
  When a payment of 1000.00 is made using BANK_TRANSFER method
  Then the payment should be saved successfully
  And the payment type should be FULL
  And the payment status should be SUCCESS
  And the card total balance should be 0.00

 Scenario: Process a minimum payment successfully
  When a payment of 100.00 is made using CHECK method
  Then the payment should be saved successfully
  And the payment type should be MINIMUM
  And the payment status should be SUCCESS
  And the card total balance should be 900.00

 Scenario: Reject an overpayment
  When a payment of 9999.00 is made using ONLINE method
  Then the payment should be rejected with an error

 Scenario: Payment is applied to cash advance balance first
  Given the payment card has a balance of 500.00 and cash advance balance of 500.00
  When a payment of 600.00 is made using ONLINE method
  Then the payment should be saved successfully
  And the cash advance balance should be 0.00
  And the card balance should be 400.00

 Scenario: Retrieve payment history for a card
  When a payment of 300.00 is made using ONLINE method
  Then the payment history for the card should contain 1 payment
  And the payment history should show amount of 300.0