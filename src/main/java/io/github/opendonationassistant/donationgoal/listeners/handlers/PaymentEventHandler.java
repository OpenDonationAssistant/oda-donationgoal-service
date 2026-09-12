package io.github.opendonationassistant.donationgoal.listeners.handlers;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.goal.GoalFacade;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.github.opendonationassistant.events.payments.PaymentEvent;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class PaymentEventHandler extends AbstractMessageHandler<PaymentEvent> {

  private final ODALogger log = new ODALogger(this);
  private final GoalFacade facade;
  private final RabbitClient commandsFacade;

  @Inject
  public PaymentEventHandler(
    ObjectMapper mapper,
    GoalFacade facade,
    RabbitClient commandsFacade
  ) {
    this.facade = facade;
    this.commandsFacade = commandsFacade;
    super(mapper);
  }

  @Override
  public void handle(PaymentEvent payment) throws IOException {
    final var goalId = payment.goal();
    if (goalId == null) {
      log.debug("goalId is null", Map.of("id", payment.id()));
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
    commandsFacade.sendCommand(
      new CountPaymentInGoalWithModeAll(
        payment.id(),
        payment.recipientId(),
        payment.amount()
      )
    );
  }

  @Serdeable
  public static record CountPaymentInGoalWithModeAll(
    String paymentId,
    @Nullable String recipientId,
    Amount amount
  ) {}
}
