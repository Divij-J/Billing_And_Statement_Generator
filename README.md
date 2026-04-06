# Credit Card Billing & Statement Generator
<a href="https://capgemini-my.sharepoint.com/:p:/r/personal/stephen_harayo_capgemini_com/Documents/Credit%20Card%20Billing%20and%20Statement%20Generator.pptx?d=wfb546ca9dff24842be65bd21979a3b85&csf=1&web=1&e=78Mxjo" target="_blank">
  <img src="https://img.shields.io/badge/View%20Slides-Click%20Here-blue?style=for-the-badge" />
</a>

A Spring Boot application designed to simulate the full lifecycle of a credit card billing system from card issuance and transaction tracking through billing cycle generation, payment processing, and statement creation. This project demonstrates the implementation of financial domain modelling, standard security protocols, strict data validation, and scalable architectural patterns.

## Use Case
The **Credit Card Billing & Statement Generator** serves as a centralized backend for financial services to handle the complete lifecycle of credit card billing operations. It solves the need for accurate financial calculation, secure data storage, identity verification through JWT, and automated billing rule enforcement (e.g. interest calculation, minimum due, cash advance fees, late fees).

## Key Accomplishments
* **Secure Authentication Pipeline**: Successfully implemented a dual-layered authentication system using Basic Auth (for login) and JWT Bearer Auth (for stateless session management). Tokens are signed with HS256 and expire after 1 hour.
* **Financial Calculation Engine**: Implemented accurate financial calculations including daily interest, cash advance fees (2%), late fees ($50), and minimum due calculation ($100).
* **Server-Determined Payment Classification**: Payments are automatically classified as FULL, MINIMUM, or PARTIAL by the server based on the amount relative to the outstanding balance and minimum due, removing the need for clients to specify payment type.
* **Real-Time Balance Reconciliation**: When a payment is processed, the system simultaneously updates the card balance, billing cycle outstanding, statement remaining balance, and statement status, ensuring all data is consistent at all times.
* **Automated API Documentation**: Integrated Swagger UI providing a living, interactive documentation portal that simplifies manual testing and API exploration.
* **Stateless Security Architecture**: Built a security filter chain that decodes and validates JWTs, ensuring the application remains stateless and horizontally scalable.

## Feature & Technical Implementation
* **Identity Exchange**: Implemented an 'AuthController' that trades Basic Auth credentials for a JSON Web Token (JWT). The token contains the user's granted authorities as a 'scope' claim.
* **Bearer Authorization**: Secured all data endpoints using Spring Security's OAuth2 resource server that validates the JWT signature against a server-side secret.
* **Transaction Management**: The 'TransactionService' handles PURCHASE and CASHADVANCE transactions with credit limit enforcement. DECLINED transactions are still saved to the database for audit purposes but do not affect the card balance. Cash advance fees are automatically generated as FEE transactions.
* **Billing Cycle Generation**: The 'BillingService' collects all unbilled transactions, calculates interest (daily compound method), applies late fees if the previous minimum due was missed, and assigns transactions to the new cycle.
* **Payment Processing**: The 'PaymentService' applies payments first to 'cashAdvanceBalance' (higher interest rate) then to 'cardBalance', rejects overpayments, and propagates the balance change to the billing cycle and statement in a single transaction.
* **Statement Generation**: The 'StatementService' generates a full billing statement including all transactions for the cycle, calculates the statement balance, remaining balance, and carry-forward balance, and tracks the statement lifecycle (GENERATED -> UNPAID -> PAID).

## Design Patterns & Architecture
* **Controller -> Service -> Repository**: Followed the classic layered architecture to ensure separation of concerns and maintainability. Each layer has a single responsibility and communicates only with the adjacent layer.
* **DTO Pattern**: Utilized Data Transfer Objects to decouple the API contract from the database layer. Request DTOs handle validation with '@Valid' annotations. Response DTOs expose only the fields needed by the consumer.
* **Mapper Pattern**: Implemented dedicated mapper components to handle transformation logic between entities and DTOs. Server-controlled fields like IDs, dates, and calculated values are never mapped from the request.
* **Repository Pattern**: Used Spring Data JPA repositories extending 'JpaRepository' to abstract all database access. Custom '@Query' annotations are used for complex joins and aggregations (e.g. total paid by cycle, payments within date range).
* **Service Exception Pattern**: Custom exception classes ('NotFoundException', 'ConflictException', 'ValidationException', 'LimitExceededException') are defined within each service to provide meaningful error context while keeping the exception hierarchy clean.

### High Level System Architecture & Request Lifecycle
![HLSA](https://github.com/user-attachments/assets/22338dac-73cd-4798-8554-a7d9f0cba4d4)

## Logging & Error Handling
* **SLF4J/Logback**: Integrated the SLF4J logging framework using Lombok's '@Slf4j' annotation across all service and controller classes. Logs capture authentication events, balance changes, payment processing, and transaction outcomes.
* **Validation**: Input validation is enforced at the DTO level using Jakarta Bean Validation ('@NotNull', '@NotBlank', '@Valid', '@DecimalMin', '@Pattern'). The '@CreditCardNumber' annotation on the Card entity enforces Luhn algorithm validation on card numbers.

## Tech Stack
* Java 17
* Springboot 4.0.2
* Gradle
* PostgreSQL
* Open API Swagger 3.0.0
* SLF4J 1.7.7
* JDK: Eclipse Temurin 25
* Spring Data JPA / Hibernate
* Spring Security + OAuth2 Resource Server
* JUnit 5

## Database Model
![ER](https://github.com/user-attachments/assets/9c82d716-2c0f-4fc2-8e0a-5646ae7bbb50)
