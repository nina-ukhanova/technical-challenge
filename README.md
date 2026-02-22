# Introduction

This repository gives a (very) simple example of a payment processor which takes an inbound message, processes it and stores it before sending to an external system.

## System requirements

1. Scalability
   - API has to support at least 50 concurrent users 

2. Security
   - sensitive data must not be logged in the clear
   - considered to be sensitive:
     - card holder
     - card number

3. Business rules
   - Amount must be greater 0 and less or equal 199999999
    

# Your tasks


1. Find and reproduce up to 3 bugs you may find and provide fixes by appropriate unit or integration tests. Any other bugs you may find can be noted below.
   - Refactoring is allowed if it simplifies testing (but do not modify unless necessary - this is targetted for production)
   - Explain why that kind of test has been chosen

2. Add relevant logging
   - Add logging to support tracing of the payment processing
  
3. List your changes to the code (including tests) in the section below

3. Describe improvements or questions you have.
   - List your improvements in the section below and explain why they are important (consider how you would make this code production-ready)
   - List any questions you have on the business/technical requirements for this system.

4. Zip up your changes and send it back to littlepay

# Useful scripts

```bash
for i in {1..50}; do
  (curl -X POST http://localhost:8080/api/payments \
        -H "Content-Type: application/json" \
        -d '{"cardHolder": "Alice","amount": 100000001,"currency": "USD","cardNumber": "4111111111111111"}') &
done
```

```bash
curl http://localhost:8080/api/payments
        
```

# My changes

Link to github - https://github.com/nina-ukhanova/technical-challenge 

## 1. Fixed Business Rule Validation
- Added amount validation: must be > 0 and ≤ 199,999,999
- Test: `PaymentServiceTest.testProcessPayment_AmountInvalid()`
- Prevents invalid transactions and ensures compliance with business requirements

## 2. Fixed ID Collision Bug
- Changed from `new Date().getTime()` to database-managed sequence
- Test: `PaymentIdCollisionTest.testID_NoCollisionsWithConcurrentCreation()`
- Prevents primary key violations under concurrent load (multiple payments in same millisecond)

## 3. Added @Transactional to processPayment()
- Ensures atomic operations - rollback if external system fails
- Test: `PaymentServiceTransactionTest.testProcessPaymentTransactionRollback_WhenExternalSystemFails()`
- Prevents orphaned PENDING payments when external system fails

## 4. Changed Amount from int/float to BigDecimal 
- Changed `amount` field from `float` to `BigDecimal` in `Payment` entity and  related DTO with scale of 4.
- BigDecimal ensures exact decimal arithmetic (0.1 + 0.2 = 0.3, not 0.30000001)
- Breaking change - clients must update JSON parsing to handle BigDecimal serialization.
However, the issue is also that controller returns directly the entity (can expose internal entity structure to clients + schema changes can break API contracts)
To fix that it is required to create a separate DTO as response.

## 5. Removed Unnecessary Response Object
- Simplified ExternalSystemMock - removed unused object creation in sendPayment() method
- Improves code clarity

## 6. Configuration for 50 Concurrent Users
- Enabled Virtual Threads for concurrent requests handling and reducing resource consumption
- Removed configuration for tomcat threads as it is not relevant after enabling virtual threads
- Configured HikariCP connection pool (max 50 connections)

## 7. Added Logging with Sensitive Data Protection
- Added SLF4J logging
- Card holder names and full card numbers NEVER logged (only masked: ****1234)

## 8. Idempotency Key Enhancement
- Original implementation used timestamp with only minute-level precision, causing collisions when multiple payments were submitted within the same minute.
- Enhanced to use nanosecond-precision timestamp to drastically reduce collision window from 1 minute to nanosecond - temporary fix (also made this field unique in DB)
- Time-based approach still irrelevant - retrying the same payment generates different keys, defeating idempotency purpose.
- Better solution (client-provided keys) noted in suggestions.

# My suggested improvements
## 1. Use Enumerations for Status and Currency
- Status and currency are stored as strings, allowing invalid values.
- Suggestion: Create enums `PaymentStatus` and `Currency`, store as `@Enumerated(EnumType.STRING)` or `EnumType.ORDINAL`.
That will prevent invalid data entry, improves type safety and ordinal storage can improve query performance and reduce storage space.

## 2. Add Request Payload Validation
- `PaymentRequest` lacks validation.
- Suggestion: Add Bean Validation annotations:
```java
@NotBlank(message = "Card holder is required")
private String cardHolder;

@NotNull @Positive
@DecimalMax(value = "199999999")
private BigDecimal amount;

@NotBlank @Pattern(regexp = "^[A-Z]{3}$")
private String currency;

@NotBlank @Size(min = 13, max = 19)
private String cardNumber;
```
Fails fast with clear error messages, prevents invalid data from reaching business logic.

## 3. Separate Async External System Call from DB Transaction
- `sendPayment()` is a blocking call inside the same transactional method that saves to database, holding DB connection during external I/O.
- Suggestion to be discussed: To use two-phase processing
```java
@Transactional
public Payment processPayment(PaymentRequest request) {
    Payment payment = repository.save(new Payment(...));

    // Instead of calling the method, publish an event
    eventPublisher.publishEvent(new PaymentCreatedEvent(payment.getId()));

    return payment;
}

@Transactional(propagation = REQUIRES_NEW)
public void processExternalPaymentAsync(UUID paymentId) {
    Payment payment = repository.findById(paymentId).orElseThrow();
    try {
        Payment response = externalSystem.sendPayment(payment);
        payment.setStatus(response.getStatus());
    } catch (Exception e) {
        payment.setStatus("FAILED");
    }
    repository.save(payment);
}

@Component
public class PaymentEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        // This ONLY runs after the first transaction is 100% saved in the DB
        processExternalPaymentAsync(event.paymentId());
    }
}
```
Improves scalability by not blocking DB connections during slow external calls.

## 4. Move Repository Calls to Service Layer and Fix Count Performance
- Controller directly calls `repository.findAll().size()` which loads all Payment records into memory just to count them, can be an issue with, for example, 1M of records.
- Suggestion: To extract a method to service layer which provides proper separation of concerns and transaction management
```java
// Service layer
@Transactional(readOnly = true)
public long countPayments() {
    return repository.count();
}

// Controller
@GetMapping("/count")
public ResponseEntity<Long> countPayments() {
    return ResponseEntity.ok(paymentService.countPayments());
}
```
Breaking change - the integration must be updated on the client side. 


## 5. Replace H2 with Production Database
- H2 in-memory database loses all data on restart, no persistence.
- Suggestion: Use production-grade database: PostgreSQL/MySQL/Oracle

## 6. Client-Provided Idempotency Keys
- Server-generated idempotency keys based on payment data can't distinguish between legitimate duplicate payments and retries.
- Suggestion: Accept idempotency key from client in request header or body:
```java
@PostMapping
public ResponseEntity<Payment> processPayment(
    @RequestHeader("X-Idempotency-Key") UUID idempotencyKey,
    @RequestBody PaymentRequest request) {
}
```
Gives clients full control over retry logic. Same key = return cached result, different key = new payment. Prevents accidental duplicate payments.

## 7. Database Indexes
- No explicit indexes defined - queries on `idempotencyKey` will perform full table scans causing performance degradation when the data is increasing.
- Suggestion: Add indexes on frequently queried columns

## 8. Retry Logic for external call + probably add Circuit Breaker
- No retry mechanism for failures (temporary service unavailability).
- Suggestion: Add configurable retry

# Questions on Business/Technical Requirements

## Business Questions:
1. **Payment Status Transitions:** What are all valid status transitions? (PENDING → AUTHORISED, PENDING → FAILED, etc.)
2. **Idempotency:** Should idempotency keys be cached? And how long should idempotency keys be valid? 
3. **Failed Payment Retry:** Should system automatically retry failed payments? How many times?

## Technical Questions:
1. **Timeout Configuration:** Calls to external system should have timeout. What timeout is acceptable for external system calls? (5s? 30s?)
2. **Encryption:** Should card numbers be encrypted in the database? Card holder should be masked in DB?
