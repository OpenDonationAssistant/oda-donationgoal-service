package io.github.opendonationassistant.donationgoal.repository;

import io.github.opendonationassistant.events.goal.GoalCommandSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class GoalRepository {

  private final GoalDataRepository dataRepository;
  private final GoalCommandSender commandSender;

  @Inject
  public GoalRepository(
    GoalDataRepository dataRepository,
    GoalCommandSender commandSender
  ) {
    this.dataRepository = dataRepository;
    this.commandSender = commandSender;
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
