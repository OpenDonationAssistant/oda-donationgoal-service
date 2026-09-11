package io.github.opendonationassistant.donationgoal.listeners;

import static org.mockito.Mockito.*;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalData;
import io.github.opendonationassistant.donationgoal.repository.GoalDataRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalLinkRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalMode;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInDefaultGoalCommand;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.github.opendonationassistant.events.goal.GoalWidgetCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoal;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender.Stage;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CommandListenerTest {

  private static final String WIDGET_ID = "test-widget-id";

  private final ObjectMapper mapper = ObjectMapper.getDefault();
  private final GoalRepository repository = mock(GoalRepository.class);
  private final UpdatedGoalSender goalSender = mock(UpdatedGoalSender.class);
  private final GoalLinkRepository linkRepository = mock(
    GoalLinkRepository.class
  );
  private final CommandListener listener = new CommandListener(
    repository,
    goalSender,
    linkRepository
  );

  private Goal goal(String id, String recipientId, GoalMode mode) {
    var data = new GoalData(
      id,
      recipientId,
      WIDGET_ID,
      "Test Goal",
      "Full description",
      new Amount(100, 0, "RUB"),
      new Amount(1000, 0, "RUB"),
      true,
      mode
    );
    return new Goal(
      data,
      mock(GoalWidgetCommandSender.class),
      mock(GoalDataRepository.class),
      mock(GoalLinkRepository.class)
    );
  }

  @Test
  @DisplayName(
    "Should add amount to every ALL-mode goal and send AFTER_PAYMENT"
  )
  void listen_allModeCommand_countsInEveryGoal() throws IOException {
    var recipientId = "recipient-id";
    var paymentId = "payment-id";
    var first = goal("goal-1", recipientId, GoalMode.ALL);
    var second = goal("goal-2", recipientId, GoalMode.ALL);
    when(repository.listByMode(recipientId, GoalMode.ALL)).thenReturn(
      List.of(first, second)
    );

    var command = new CommandListener.CountPaymentInGoalWithModeAll(
      paymentId,
      recipientId,
      new Amount(50, 0, "RUB")
    );

    listener.listen(
      "CountPaymentInGoalWithModeAll",
      mapper.writeValueAsBytes(command)
    );

    verify(repository).listByMode(recipientId, GoalMode.ALL);
    verify(goalSender).sendGoal(
      eq(Stage.AFTER_PAYMENT),
      argThat(
        (UpdatedGoal goal) ->
          "goal-1".equals(goal.goalId()) &&
          goal.accumulatedAmount().getMajor() == 150
      )
    );
    verify(goalSender).sendGoal(
      eq(Stage.AFTER_PAYMENT),
      argThat((UpdatedGoal goal) -> "goal-2".equals(goal.goalId()))
    );
  }

  @Test
  @DisplayName("Should do nothing when ALL-mode command has null recipientId")
  void listen_allModeCommandWithNullRecipient_doesNothing()
    throws IOException {
    var command = new CommandListener.CountPaymentInGoalWithModeAll(
      "payment-id",
      null,
      new Amount(50, 0, "RUB")
    );

    listener.listen(
      "CountPaymentInGoalWithModeAll",
      mapper.writeValueAsBytes(command)
    );

    verify(repository, never()).listByMode(any(), any());
    verify(goalSender, never()).sendGoal(any(), any());
  }

  @Test
  @DisplayName("Should send nothing when there are no ALL-mode goals")
  void listen_allModeCommandWithoutGoals_sendsNothing() throws IOException {
    var recipientId = "recipient-id";
    when(repository.listByMode(recipientId, GoalMode.ALL)).thenReturn(List.of());

    var command = new CommandListener.CountPaymentInGoalWithModeAll(
      "payment-id",
      recipientId,
      new Amount(50, 0, "RUB")
    );

    listener.listen(
      "CountPaymentInGoalWithModeAll",
      mapper.writeValueAsBytes(command)
    );

    verify(goalSender, never()).sendGoal(any(), any());
  }

  @Test
  @DisplayName("Should count in specified goal when CountPaymentInSpecifiedGoalCommand is received")
  void listen_specifiedGoalCommand_countsInGoal() throws IOException {
    var recipientId = "recipient-id";
    var goalId = "goal-1";
    var goal = goal(goalId, recipientId, GoalMode.CHOOSE);
    when(repository.getById(goalId)).thenReturn(Optional.of(goal));

    var command = new CountPaymentInSpecifiedGoalCommand(
      "payment-id",
      recipientId,
      goalId,
      new Amount(50, 0, "RUB")
    );

    listener.listen(
      "CountPaymentInSpecifiedGoalCommand",
      mapper.writeValueAsBytes(command)
    );

    verify(goalSender).sendGoal(
      eq(Stage.AFTER_PAYMENT),
      argThat((UpdatedGoal updated) -> goalId.equals(updated.goalId()))
    );
  }

  @Test
  @DisplayName("Should count in default goal when CountPaymentInDefaultGoalCommand is received")
  void listen_defaultGoalCommand_countsInGoal() throws IOException {
    var recipientId = "recipient-id";
    var goal = goal("goal-1", recipientId, GoalMode.DEFAULT);
    when(repository.getDefaultGoal(recipientId)).thenReturn(Optional.of(goal));

    var command = new CountPaymentInDefaultGoalCommand(
      "payment-id",
      recipientId,
      new Amount(50, 0, "RUB")
    );

    listener.listen(
      "CountPaymentInDefaultGoalCommand",
      mapper.writeValueAsBytes(command)
    );

    verify(goalSender).sendGoal(
      eq(Stage.AFTER_PAYMENT),
      argThat((UpdatedGoal updated) -> "goal-1".equals(updated.goalId()))
    );
  }
}
