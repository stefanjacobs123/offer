# README

This service's sole responsibility is to keep state of Offers inside the greater system.

## OFFER

An Offer has the following requirements:
 - A shopper friendly description
 - A price
 - A currency
 - An expiry date (the offer should expire at said date)
 - An offer should be cancellable (before its expiry date)
 
## REST API 

- Create an offer
- Query an offer
- When expired, query should reflect it
- Should be cancellable - before offer expire

Any REST action should check whether offer has expired.

Keep appropriate HTTP Status Codes in mind. 

## GUIDELINES SUMMARY

- Java or Scala
- HTTP REST API
- No restrictions on libraries
- Git
- Simple solution, but enterprise deliverable
- Use TDD
- OO Design, Clean Code, SOLID
- Offers might be held in memory (for simplification)
- No need for auth
- Document assumptions in README.
