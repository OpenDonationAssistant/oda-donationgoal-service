package io.github.opendonationassistant.donationgoal.repository;

import io.github.opendonationassistant.commons.Amount;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

@Serdeable
@MappedEntity("goal")
public record GoalData(
  @Id String id,
  String recipientId,
  String widgetId,
  @Nullable String briefDescription,
  @Nullable String fullDescription,
  Amount accumulatedAmount,
  Amount requiredAmount,
  Boolean enabled,
  GoalMode mode
) {
  public GoalData withAccumulatedAmount(Amount amount) {
    return new GoalData(
      id,
      recipientId,
      widgetId,
      briefDescription,
      fullDescription,
      amount,
      requiredAmount,
      enabled,
      mode
    );
  }
}
