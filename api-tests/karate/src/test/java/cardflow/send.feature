@ignore
Feature: reusable signed POST

  Scenario:
    Given url baseUrl
    And path '/api/v1/webhooks/card-events'
    And header X-Signature = sig
    And request raw
    When method post
    Then status 202
