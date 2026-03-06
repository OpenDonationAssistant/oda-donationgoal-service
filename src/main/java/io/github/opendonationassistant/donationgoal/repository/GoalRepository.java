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
import org.jspecify.annotations.Nullable;

@Singleton
public class GoalRepository {

  private final ODALogger log = new ODALogger(this);
  private final GoalDataRepository dataRepository;
  private final GoalWidgetCommandSender commandSender;
  private final GoalLinkRepository linkRepository;

  @Inject
  public GoalRepository(
    GoalDataRepository dataRepository,
    GoalWidgetCommandSender commandSender,
    GoalLinkRepository linkRepository
  ) {
    this.dataRepository = dataRepository;
    this.commandSender = commandSender;
    this.linkRepository = linkRepository;
  }

  public Optional<Goal> getByOriginId(String originId) {
    return Optional.ofNullable(originId)
      .flatMap(linkRepository::getByOriginId)
      .flatMap(link -> dataRepository.getById(link.goalId()))
      .map(this::convert);
  }

  public Goal create(String recipientId, String widgetId, @Nullable String id) {
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
    return new Goal(data, commandSender, dataRepository, linkRepository);
  }

  public List<Goal> list(String recipientId) {
    return dataRepository
      .getByRecipientId(recipientId)
      .stream()
      .map(this::convert)
      .toList();
  }

  public Optional<Goal> getById(@Nullable String id) {
    return Optional.ofNullable(id)
      .flatMap(dataRepository::getById)
      .map(this::convert);
  }

  public List<Goal> listByWidgetId(String recipientId, String widgetId) {
    return dataRepository
      .getByRecipientIdAndWidgetId(recipientId, widgetId)
      .stream()
      .map(this::convert)
      .toList();
  }

  private Goal convert(GoalData data) {
    return new Goal(data, commandSender, dataRepository, linkRepository);
  }
}
