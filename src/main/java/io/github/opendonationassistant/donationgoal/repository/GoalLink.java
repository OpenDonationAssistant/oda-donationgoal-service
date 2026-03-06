package io.github.opendonationassistant.donationgoal.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;

@MappedEntity("goal_link")
public record GoalLink(
  @Id String id,
  String goalId,
  String originId,
  String source
) {}
