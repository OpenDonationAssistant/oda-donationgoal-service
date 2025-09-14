package io.github.opendonationassistant.donationgoal.repository;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.CompletedPaymentNotification;
import io.github.opendonationassistant.events.goal.GoalWidgetCommand;
import io.github.opendonationassistant.events.goal.GoalWidgetCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoal;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;
import java.util.Optional;

@Serdeable
public class Goal {

  private final ODALogger log = new ODALogger(this);

  private final GoalWidgetCommandSender commandSender;
  private final GoalDataRepository repository;
  private GoalData data;

  public Goal(
    GoalData data,
    GoalWidgetCommandSender commandSender,
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

  public Goal add(Amount amount) {
    var oldAmount = this.data.accumulatedAmount();
    return new Goal(
      this.data.withAccumulatedAmount(
          new Amount(
            oldAmount.getMajor() + amount.getMajor(),
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
    repository.deleteById(this.data.id());
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

  public GoalWidgetCommand asGoalCommand() {
    return new GoalWidgetCommand(
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
      Optional.ofNullable(this.data.briefDescription()).orElse(""),
      "fullDescription",
      Optional.ofNullable(this.data.fullDescription()).orElse(""),
      "accumulatedAmount",
      this.data.accumulatedAmount(),
      "requiredAmount",
      this.data.requiredAmount(),
      "default",
      Optional.ofNullable(this.data.isDefault()).orElse(false)
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

  @JsonGetter("data")
  public GoalData data() {
    return this.data;
  }
}
