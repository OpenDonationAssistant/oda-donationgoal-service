package io.github.opendonationassistant;

import io.github.opendonationassistant.donationgoal.listeners.CommandListener;
import io.github.opendonationassistant.donationgoal.listeners.ConfigListener;
import io.github.opendonationassistant.donationgoal.listeners.EventsListener;
import io.github.opendonationassistant.donationgoal.listeners.GoalListener;
import io.github.opendonationassistant.rabbit.AMQPConfiguration;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.annotation.Factory;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Singleton;
import java.util.ArrayList;

@Factory
public class Application {

  @ContextConfigurer
  public static class DefaultEnvironmentConfigurer
    implements ApplicationContextConfigurer {

    @Override
    public void configure(ApplicationContextBuilder builder) {
      builder.defaultEnvironments("standalone");
    }
  }

  @Singleton
  public ChannelInitializer channelInitializer() {
    var exchanges = new ArrayList<Exchange>();
    exchanges.addAll(ConfigListener.BINDING);
    exchanges.addAll(GoalListener.BINDING);
    exchanges.addAll(EventsListener.BINDING);
    exchanges.addAll(CommandListener.BINDING);
    return new AMQPConfiguration(exchanges);
  }

  public static void main(String[] args) {
    Micronaut.build(args).banner(false).classes(Application.class).start();
  }
}
