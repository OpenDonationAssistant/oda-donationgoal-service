package io.github.opendonationassistant.donationgoal.repository;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.goal.GoalWidgetCommand;
import io.github.opendonationassistant.events.goal.GoalWidgetCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoal;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@Serdeable
public class Goal {

  private final ODALogger log = new ODALogger(this);

  private final GoalWidgetCommandSender commandSender;
  private final GoalDataRepository repository;
  private final GoalLinkRepository linkRepository;
  private GoalData data;

  public Goal(
    GoalData data,
    GoalWidgetCommandSender commandSender,
    GoalDataRepository repository,
    GoalLinkRepository linkRepository
  ) {
    this.commandSender = commandSender;
    this.repository = repository;
    this.data = data;
    this.linkRepository = linkRepository;
  }

  public Goal add(Amount amount, String source, @Nullable String originId) {
    var oldAmount = this.data.accumulatedAmount();
    if (originId != null) {
      linkRepository.save(
        new GoalLink(
          Generators.timeBasedEpochGenerator().generate().toString(),
          data.id(),
          originId,
          source
        )
      );
    }
    return update(
      data.withAccumulatedAmount(
        new Amount(
          oldAmount.getMajor() + amount.getMajor(),
          oldAmount.getMinor(),
          oldAmount.getCurrency()
        )
      )
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
    return update(
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
      )
    );
  }

  private Goal update(GoalData data) {
    return new Goal(data, commandSender, repository, linkRepository);
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
      ofNullable(data.fullDescription()).orElse(""),
      ofNullable(this.data.briefDescription()).orElse(""),
      this.data.requiredAmount(),
      this.data.accumulatedAmount(),
      this.data.isDefault()
    );
  }

  public GoalWidgetCommand asGoalCommand() {
    return new GoalWidgetCommand(
      "update",
      this.data.id(),
      ofNullable(this.data.fullDescription()).orElse(""),
      ofNullable(this.data.briefDescription()).orElse(""),
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

  @JsonGetter("data")
  public GoalData data() {
    return this.data;
  }
}
