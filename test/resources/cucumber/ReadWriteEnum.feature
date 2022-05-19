Feature: Enum read write functions

  Scenario: Create Model with invalid iri enum
    Given ClassWithEnum object with invalid IRI
    When I try to create Model with given enum
    Then Model should be empty

  Scenario: Read enum
    Given Graph read from a file
    When Create with the read object
    Then ClassWithEnum object is created