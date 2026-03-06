package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.events.MessageProcessor;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Inject;

@RabbitListener
public class EventsListener {

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
