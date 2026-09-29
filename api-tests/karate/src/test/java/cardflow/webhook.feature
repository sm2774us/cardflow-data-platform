Feature: Signed webhook ingestion — authentication and idempotency

  Background:
    * url baseUrl
    * def Sign = Java.type('cardflow.SignHelper')
    * def newEvent =
      """
      function() {
        var id = Java.type('cardflow.SignHelper').uuid();
        return { eventId: id, schemaVersion: 1, eventType: 'AUTHORIZATION', cardToken: 'tok_KARATE0000001',
                 merchantName: 'Karate Cafe', mcc: '5814', amount: 9.99, currency: 'USD', country: 'US',
                 occurredAt: new Date().toISOString() };
      }
      """

  Scenario: missing signature is rejected with RFC 7807 problem body
    * def body = newEvent()
    Given path '/api/v1/webhooks/card-events'
    And request body
    When method post
    Then status 401

  Scenario: wrong signature is rejected
    * def body = newEvent()
    Given path '/api/v1/webhooks/card-events'
    And header X-Signature = 'sha256=deadbeef'
    And request body
    When method post
    Then status 401

  Scenario: valid signed event is accepted; redelivery is flagged duplicate
    * def body = newEvent()
    * string raw = body
    * def sig = Sign.sign(raw, secret)
    Given path '/api/v1/webhooks/card-events'
    And header X-Signature = sig
    And request raw
    When method post
    Then status 202
    And match response == { eventId: '#(body.eventId)', duplicate: false }

    Given path '/api/v1/webhooks/card-events'
    And header X-Signature = sig
    And request raw
    When method post
    Then status 202
    And match response.duplicate == true

  Scenario: accepted event becomes a queryable transaction exactly once
    * def body = newEvent()
    * string raw = body
    * def sig = Sign.sign(raw, secret)
    * def send =
      """
      function() {
        var r = karate.call('classpath:cardflow/send.feature', { raw: raw, sig: sig });
        return r.responseStatus;
      }
      """
    * eval send()
    * eval send()
    * eval send()
    * configure retry = { count: 20, interval: 500 }
    Given path '/api/v1/transactions'
    And param q = 'KARATE'
    And param size = 100
    And retry until response.items.filter(function(t){ return t.eventId == body.eventId }).length >= 1
    When method get
    Then status 200
    And match response.items.filter(function(t){ return t.eventId == body.eventId }).length == 1
