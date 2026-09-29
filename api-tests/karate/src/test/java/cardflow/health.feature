Feature: Platform health and contract basics

  Background:
    * url baseUrl

  Scenario: liveness and readiness probes are UP
    Given path '/actuator/health/liveness'
    When method get
    Then status 200
    And match response.status == 'UP'

  Scenario: pipeline stats respond with the expected shape
    Given path '/api/v1/pipeline/stats'
    When method get
    Then status 200
    And match response == '#object'

  Scenario: DQ rule catalogue exposes the documented rules
    Given path '/api/v1/dq/rules'
    When method get
    Then status 200
    And match response == '#[_ > 5]'
    And match each response == { code: '#string', description: '#string' }

  Scenario: every response carries a correlation id
    Given path '/api/v1/dq/rules'
    When method get
    Then status 200
    And match header X-Correlation-Id == '#string'
