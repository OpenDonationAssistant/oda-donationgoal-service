package io.github.opendonationassistant.donationgoal.listeners.handlers;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.goal.GoalFacade;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.github.opendonationassistant.events.payments.PaymentEvent;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Map;

public class PaymentEventHandler extends AbstractMessageHandler<PaymentEvent> {

  private final ODALogger log = new ODALogger(this);
  private final GoalFacade facade;

  @Inject
  public PaymentEventHandler(ObjectMapper mapper, GoalFacade facade) {
    this.facade = facade;
    super(mapper);
  }

  @Override
  public void handle(PaymentEvent payment) throws IOException {
    final var goalId = payment.goal();
    if (goalId == null) {
      return;
    }
    log.debug("Received PaymentEvent with Goal", Map.of("payment", payment));
    facade.run(
      new CountPaymentInSpecifiedGoalCommand(
        payment.id(),
        payment.recipientId(),
        goalId,
        payment.amount()
      )
    );
  }
}
