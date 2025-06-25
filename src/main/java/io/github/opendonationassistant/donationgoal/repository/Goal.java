package io.github.opendonationassistant.donationgoal.repository;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.CompletedPaymentNotification;
import io.github.opendonationassistant.events.goal.GoalCommand;
import io.github.opendonationassistant.events.goal.GoalCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoal;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;
import java.util.Optional;

@Serdeable
public class Goal {

  private final ODALogger log = new ODALogger(this);

  private final GoalCommandSender commandSender;
  private final GoalDataRepository repository;
  private GoalData data;

  public Goal(
    GoalData data,
    GoalCommandSender commandSender,
    GoalDataRepository repository
  ) {
    this.commandSender = commandSender;
    this.repository = repository;
    this.data = data;
  }

  public Goal handlePayment(CompletedPaymentNotification payment) {
    var paid = payment.amount().getMajor();
    var oldAmount = this.data.accumulatedAmount();
    return new Goal(
      this.data.withAccumulatedAmount(
          new Amount(
            oldAmount.getMajor() + paid,
            oldAmount.getMinor(),
            oldAmount.getCurrency()
          )
        ),
      commandSender,
      repository
    );
  }

  public Goal update(Boolean enabled, Map<String, Object> config) {
    var fullDescription = (String) config.getOrDefault("fullDescription", "");
    var briefDescription = (String) config.getOrDefault("briefDescription", "");

    // TODO: use Amount as is
    var amount = Optional.ofNullable(
      (Map<String, Object>) config.get("requiredAmount")
    )
      .map(it -> (Integer) it.get("major"))
      .orElse(0);

    var accumulatedAmount = Optional.ofNullable(
      (Map<String, Object>) config.get("accumulatedAmount")
    )
      .map(it -> (Integer) it.get("major"))
      .orElse(0);

    var isDefault = (Boolean) config.getOrDefault("default", false);
    return new Goal(
      new GoalData(
        this.data.id(),
        this.data.recipientId(),
        this.data.widgetId(),
        briefDescription,
        fullDescription,
        new Amount(accumulatedAmount, 0, "RUB"),
        new Amount(amount, 0, "RUB"),
        enabled,
        isDefault
      ),
      commandSender,
      repository
    );
  }

  public Goal save() {
    log.info("Updating goal", Map.of("goal", this.data));
    repository.update(this.data);
    return this;
  }

  public void delete() {
    log.info("Deleting goal", Map.of("goal", this.data));
    repository.delete(this.data);
  }

  public UpdatedGoal asUpdatedGoal() {
    return new UpdatedGoal(
      this.data.id(),
      this.data.widgetId(),
      this.data.recipientId(),
      this.data.fullDescription(),
      this.data.briefDescription(),
      this.data.requiredAmount(),
      this.data.accumulatedAmount(),
      this.data.isDefault()
    );
  }

  public GoalCommand asGoalCommand() {
    return new GoalCommand(
      "update",
      this.data.id(),
      this.data.fullDescription(),
      this.data.briefDescription(),
      this.data.requiredAmount(),
      this.data.accumulatedAmount()
    );
  }

  public Map<String, Object> asWidgetConfigGoal() {
    return Map.of(
      "id",
      this.data.id(),
      "briefDescription",
      this.data.briefDescription(),
      "fullDescription",
      this.data.fullDescription(),
      "accumulatedAmount",
      this.data.accumulatedAmount(),
      "requiredAmount",
      this.data.requiredAmount(),
      "default",
      this.data.isDefault()
    );
  }

  public String id() {
    return this.data.id();
  }

  public String widgetId() {
    return this.data.widgetId();
  }

  public boolean isEnabled() {
    return this.data.enabled();
  }

  public GoalData data(){
    return this.data;
  }
}
