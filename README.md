# README

This service's sole responsibility is to keep state of Offers inside the greater system.

## OFFER

An Offer has the following requirements:
 - A shopper friendly description
 - A price
 - A currency
 - An expiry date (the offer should expire at said date)
 - An offer should be cancellable (before its expiry date)
 
### Assumptions

- Will assume user enters a valid currency - simplification.
- Offer's price won't exceed Int.MaxValue.
 
## REST API 

- Create an offer
- Query an offer
- When expired, query should reflect it
- Should be cancellable - before offer expire

Any REST action should check whether offer has expired.

Keep appropriate HTTP Status Codes in mind. 

### Assumptions

- All offers can be created if the Create Offer request is error-free (JSON not malformed for instance)
  - Create ID for offer when Create Offer request is received.
  - Do not check for duplicate offers.
  - Assume expiryDate is in the future (not in the past). Simplification.
- When an offer has expired, it does not get removed/deleted.
- To Cancel an offer, update the expiry date of the Offer. Simplification.
- Only when queried, will we check whether the offer has expired. Simplification.

## GUIDELINES SUMMARY

- Java or Scala
- HTTP REST API
- No restrictions on libraries
- Git
- Simple solution
- Use TDD
- OO Design, Clean Code, SOLID
- Offers might be held in memory (for simplification)
- No need for auth
- Document assumptions in README

## Testing

### Unit tests

`sbt test`

### Local Sanity Testing

#### Setup

1. Install `jq` (https://stedolan.github.io/jq/).
2. `sbt run` in project root

#### Typical Process

1. Create an offer that will not expire soon: 
    ```bash
    $ curl -H "Content-Type: application/json" -s POST -d '{"description": "Shopper friendly description", "price": 50, "currency": "GBP", "expiryDate": "2020-04-09T18:44:02.665"}' http://127.0.0.1:8080/offers/ | jq
    {
      "id": "0d68b975-7129-478a-b43c-e0e0e6c6077c"
    }
    ```
2. Use id returned to query offer created above:
    ```bash
    $ curl -s http://127.0.0.1:8080/offers/0d68b975-7129-478a-b43c-e0e0e6c6077c | jq
    {
      "currency": "GBP",
      "description": "Shopper friendly description",
      "expiryDate": "2020-04-09T18:44:02.665",
      "price": 50
    }
    ```
3. Cancel the offer:
    ```bash
    $ curl -H "Content-Type: application/json" -X POST -s -o /dev/null -w "%{http_code}" -d '{"id": "0d68b975-7129-478a-b43c-e0e0e6c6077c"}' http://127.0.0.1:8080/offers/ | jq
    200
    
    # This curl will only print out the resulting status code. 
    ```
4. Verify offer is canceled:
    ```bash
    $ curl -I -s -o /dev/null -w "%{http_code}"  http://127.0.0.1:8080/offers/0d68b975-7129-478a-b43c-e0e0e6c6077c | jq
    410
    
    # 410 = Gone
    ```
5. Query offer that does not exist:
    ```bash
    $ curl -I -s -o /dev/null -w "%{http_code}"  http://127.0.0.1:8080/offers/8cfb11bf-4fae-4054-bae1-7b084c67b378 | jq
    404
    
    # 404 = Not Found
    ```

## Improvements

1. Would ideally keep REST Request and Response isolated from Actor Commands and Responses. Can then keep implementation
details and API seperate. API doesn't need to change when Actor / Internals of app changes.
2. Tests for Actor.
3. Use akka-persistence for OfferActors.
4. Project structure.
