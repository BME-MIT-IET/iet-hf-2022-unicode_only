Feature: Are they isomorphic?

  Scenario: They are not isomorphic
    Given there is a lot of data
    When I ask whether two graphs from random different data are isomorphic
    Then Should be told they are not isomorphic

  Scenario: They are isomorphic
    Given there is a lot of data
    When I ask whether two graphs from the same date are isomorphic
    Then Should be told they are isomorphic