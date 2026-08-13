package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.events.MessageProcessor;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@RabbitListener
public class EventsListener {

  public static final String QUEUE_NAME = "goal.events";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final List<Exchange> BINDING = List.of(
    Exchange.Exchange("payments", Map.of("event.PaymentEvent", QUEUE)),
    Exchange.Exchange("history", Map.of("event.HistoryItemEvent", QUEUE))
  );

  private final MessageProcessor processor;

  @Inject
  public EventsListener(MessageProcessor processor) {
    this.processor = processor;
  }

  @Queue(
    value = io.github.opendonationassistant.rabbit.Queue.Goal.EVENTS,
    executor = "events-listener"
  )
  public void listen(
    @MessageHeader String type,
    byte[] payload,
    RabbitAcknowledgement ack
  ) {
    processor.process(type, payload, ack);
  }
}
