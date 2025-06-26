package io.github.opendonationassistant.donationgoal.repository;

import io.github.opendonationassistant.commons.Amount;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;

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
  @MappedProperty("isdefault") Boolean isDefault
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
      isDefault
    );
  }
}
