Feature: Statement Generation and Retrieval
 As a credit card system
 I want to generate and retrieve billing statements
 So that cardholders have an accurate record of their billing cycle activity

 Background:
  Given a statement customer exists in the system
  And a statement card exists with a credit limit of 5000.00
  And the statement card has a balance of 1020.00 and cash advance balance of 0.00
  And a billing cycle exists for the statement card with total outstanding of 1020.00 and minimum due of 100.00

 Scenario: Generate a statement successfully
  When a statement is generated for the card and billing cycle
  Then the statement should be saved successfully
  And the statement status should be GENERATED
  And the statement balance should be 1020.00
  And the minimum due should be 100.00

 Scenario: Cannot generate a duplicate statement for the same billing cycle
  Given a statement already exists for the billing cycle
  When a statement is generated for the card and billing cycle
  Then the statement generation should fail with an error

 Scenario: Cannot generate a statement for a billing cycle that does not belong to the card
  Given a different statement card exists in the system
  When a statement is generated for the different card using the original billing cycle
  Then the statement generation should fail with an error

 Scenario: Retrieve a generated statement returns the frozen snapshot
  Given a statement has been generated for the card and billing cycle
  When the statement is retrieved by its statement ID
  Then the retrieved statement status should be GENERATED
  And the retrieved statement balance should be 1020.00
  And the retrieved statement should contain the transactions list
  And the retrieved statement should contain the payments list

 Scenario: Retrieve a non-existent statement throws an error
  When a statement is retrieved with a random non-existent ID
  Then the retrieval should fail with an error

 Scenario: Statement carry forward balance is correct when no payments made
  When a statement is generated for the card and billing cycle
  Then the carry forward balance should equal the statement balance of 1020.00

 Scenario: Statement remaining balance is reduced after payment
  Given a payment of 500.00 has been made against the statement card
  And the statement card has a balance of 520.00 and cash advance balance of 0.00
  When a statement is generated for the card and billing cycle
  Then the remaining statement balance should be less than the statement balance