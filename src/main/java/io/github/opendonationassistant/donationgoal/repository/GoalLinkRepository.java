package io.github.opendonationassistant.donationgoal.repository;

import java.util.Optional;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface GoalLinkRepository extends CrudRepository<GoalLink, String> {
  Optional<GoalLink> getByOriginId(String originId);
}
