Feature: Real-time authorization decisions

  Background:
    * url baseUrl
    * path '/api/v1/authorizations'

  Scenario: ordinary purchase is approved
    Given request { cardToken: 'tok_KARATE_AUTH1', mcc: '5411', amount: 25.00, country: 'US' }
    When method post
    Then status 200
    And match response == { approved: true, reasons: '#array', riskScore: '#number', degraded: '#boolean' }
    And assert response.riskScore >= 0 && response.riskScore <= 1

  Scenario: sanctioned country is declined
    Given request { cardToken: 'tok_KARATE_AUTH2', mcc: '5411', amount: 25.00, country: 'KP' }
    When method post
    Then status 200
    And match response.approved == false
    And match response.reasons == '#[_ > 0]'

  Scenario Outline: request validation returns 422 problem details
    Given request { cardToken: '<token>', mcc: '<mcc>', amount: <amount>, country: '<country>' }
    When method post
    Then status 422

    Examples:
      | token | mcc  | amount | country |
      |       | 5411 | 10.00  | US      |
      | tok_X | 54   | 10.00  | US      |
      | tok_X | 5411 | 0      | US      |
      | tok_X | 5411 | 10.00  | usa     |
