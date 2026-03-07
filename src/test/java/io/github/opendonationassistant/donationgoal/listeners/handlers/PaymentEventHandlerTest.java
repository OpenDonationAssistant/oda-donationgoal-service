package io.github.opendonationassistant.donationgoal.listeners.handlers;

import static org.instancio.Select.field;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.goal.GoalFacade;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.github.opendonationassistant.events.payments.PaymentEvent;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.junit.Given;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PaymentEventHandlerTest {

  private final ObjectMapper mapper = ObjectMapper.getDefault();
  private final GoalFacade facade = mock(GoalFacade.class);

  private final PaymentEventHandler handler = new PaymentEventHandler(
    mapper,
    facade
  );

  Model<PaymentEvent> paymentEventModel = Instancio.of(PaymentEvent.class)
    .set(field(PaymentEvent::actions), List.of())
    .set(field(PaymentEvent::attachments), List.of())
    .toModel();

  @Test
  @DisplayName(
    "Should count payment in specified goal when valid message with existing goal is handled"
  )
  void handle_validMessage_countsPaymentInGoal(
    @Given String goalId,
    @Given String paymentId,
    @Given String recipientId,
    @Given String widgetId,
    @Given Amount amount
  ) throws IOException {
    final var event = Instancio.of(paymentEventModel)
      .set(field(PaymentEvent::id), paymentId)
      .set(field(PaymentEvent::recipientId), recipientId)
      .set(field(PaymentEvent::amount), amount)
      .set(field(PaymentEvent::goal), goalId)
      .create();

    handler.handle(mapper.writeValueAsBytes(event));

    verify(facade).run(
      (CountPaymentInSpecifiedGoalCommand) argThat(command -> {
        var casted = (CountPaymentInSpecifiedGoalCommand) command;
        return (
          casted.paymentId().equals(paymentId) &&
          casted.recipientId().equals(recipientId) &&
          casted.goalId().equals(goalId) &&
          casted.amount().equals(amount)
        );
      })
    );
  }

  @Test
  @DisplayName("Should return early when goal is null")
  void handle_nullGoal_returnsEarly(
    @Given String paymentId,
    @Given String recipientId,
    @Given Amount amount
  ) throws IOException {
    final var event = Instancio.of(paymentEventModel)
      .set(field(PaymentEvent::recipientId), recipientId)
      .set(field(PaymentEvent::goal), null)
      .create();

    handler.handle(mapper.writeValueAsBytes(event));

    verify(facade, never()).run(any(CountPaymentInSpecifiedGoalCommand.class));
  }
}
