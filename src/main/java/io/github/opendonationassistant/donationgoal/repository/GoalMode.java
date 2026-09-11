package io.github.opendonationassistant.donationgoal.repository;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum GoalMode {
  CHOOSE,
  DEFAULT,
  ALL;

  public static GoalMode fromConfig(Object value) {
    if (value instanceof String s) {
      try {
        return GoalMode.valueOf(s.toUpperCase());
      } catch (IllegalArgumentException e) {
        return CHOOSE;
      }
    }
    // Legacy boolean support: true → DEFAULT, false → CHOOSE
    if (value instanceof Boolean b) {
      return b ? DEFAULT : CHOOSE;
    }
    return CHOOSE;
  }
}
