package io.github.opendonationassistant.donationgoal.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface GoalDataRepository extends CrudRepository<GoalData, String> {
  List<GoalData> getByRecipientId(String recipientId);
  Optional<GoalData> getById(String id);
  List<GoalData> getByRecipientIdAndWidgetId(
    String recipientId,
    String widgetId
  );
}
