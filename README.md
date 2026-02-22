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
- ...

# My suggested improvements
- ...
