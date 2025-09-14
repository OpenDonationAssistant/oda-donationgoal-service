package io.github.opendonationassistant.donationgoal.repository;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.goal.GoalWidgetCommandSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GoalRepository {

  private final ODALogger log = new ODALogger(this);
  private final GoalDataRepository dataRepository;
  private final GoalWidgetCommandSender commandSender;

  @Inject
  public GoalRepository(
    GoalDataRepository dataRepository,
    GoalWidgetCommandSender commandSender
  ) {
    this.dataRepository = dataRepository;
    this.commandSender = commandSender;
  }

  public Goal create(String recipientId, String widgetId, String id) {
    log.info(
      "Create Goal",
      Map.of("recipientId", recipientId, "widgetId", widgetId, "id", id)
    );
    var data = new GoalData(
      Optional.ofNullable(id).orElseGet(() ->
        Generators.timeBasedEpochGenerator().generate().toString()
      ),
      recipientId,
      widgetId,
      "",
      "",
      new Amount(100, 0, "RUB"),
      new Amount(100, 0, "RUB"),
      true,
      false
    );
    dataRepository.save(data);
    return new Goal(data, commandSender, dataRepository);
  }

  public List<Goal> list(String recipientId) {
    return dataRepository
      .getByRecipientId(recipientId)
      .stream()
      .map(this::convert)
      .toList();
  }

  public Optional<Goal> getById(String id) {
    return dataRepository.getById(id).map(this::convert);
  }

  public List<Goal> listByWidgetId(String recipientId, String widgetId) {
    return dataRepository
      .getByRecipientIdAndWidgetId(recipientId, widgetId)
      .stream()
      .map(this::convert)
      .toList();
  }

  private Goal convert(GoalData data) {
    return new Goal(data, commandSender, dataRepository);
  }
}
