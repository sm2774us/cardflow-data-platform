Feature: Traffic simulator drives the full pipeline

  Scenario: burst with duplicates and invalid events is reflected in pipeline stats
    Given url baseUrl
    And path '/api/v1/simulator/burst'
    And request { count: 50, duplicatePct: 20, invalidPct: 10, poisonCount: 0 }
    When method post
    Then status 200
    And match response == { valid: '#number', invalid: '#number', duplicates: '#number', poison: '#number' }
    And assert response.valid + response.invalid + response.duplicates == 50
