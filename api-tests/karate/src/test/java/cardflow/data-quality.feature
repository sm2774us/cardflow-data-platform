Feature: Data-quality gate — bad events are quarantined, never processed

  Background:
    * url baseUrl
    * def Sign = Java.type('cardflow.SignHelper')

  Scenario: schema-invalid event is accepted at the edge but quarantined downstream
    * def id = Sign.uuid()
    * def body = { eventId: '#(id)', schemaVersion: 1, eventType: 'AUTHORIZATION', cardToken: 'tok_KARATE0000002', merchantName: 'Bad Currency', mcc: '5814', amount: 5.00, currency: 'XXX', country: 'US', occurredAt: '2026-01-01T00:00:00Z' }
    * string raw = body
    * def sig = Sign.sign(raw, secret)
    Given path '/api/v1/webhooks/card-events'
    And header X-Signature = sig
    And request raw
    When method post
    Then status 202

    * configure retry = { count: 20, interval: 500 }
    Given path '/api/v1/dq/quarantine'
    And param limit = 200
    And retry until response.filter(function(q){ return q.eventId == id }).length == 1
    When method get
    Then status 200
    And match response.filter(function(q){ return q.eventId == id })[0].failedRules == '#string'
