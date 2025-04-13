package com.example.app.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.example.app.model.Action;
import com.example.app.model.UserAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserAuditService {

  @Autowired
  private CqlSession session;

  private static final String INSERT_INTO_USER_AUDIT_QUERY =
      """
      INSERT INTO my_keyspace.user_audit (user_id, event_time, event_type, event_details)
      VALUES (?, ?, ?, ?);
      """;

  private static final String SELECT_ALL_USER_AUDIT_QUERY =
      """
      SELECT user_id, event_time, event_type, event_details
      FROM my_keyspace.user_audit
      WHERE user_id = ?;
      """;

  public void insertUserAudit(UUID userId, Action action, String eventDetails) {
    log.info("Inserting user audit for {} with action {}", userId, action);
    PreparedStatement preparedStatement = session.prepare(INSERT_INTO_USER_AUDIT_QUERY);
    BoundStatement boundStatement =
        preparedStatement.bind(userId, java.time.Instant.now(), action.toString(), eventDetails);

    session.execute(boundStatement);
  }

  public List<UserAudit> readUserAudit(UUID userId) {
    log.info("Reading user audit for {}", userId);
    PreparedStatement preparedStatement = session.prepare(SELECT_ALL_USER_AUDIT_QUERY);
    BoundStatement boundStatement = preparedStatement.bind(userId);
    ResultSet result = session.execute(boundStatement);

    ArrayList<UserAudit> users = new ArrayList<>();
    for (Row row : result.all()) {
      users.add(
          new UserAudit(
              row.getUuid("user_id"),
              row.getInstant("event_time"),
              Action.valueOf(row.getString("event_type")),
              row.getString("event_details")));
    }
    return users;
  }

  List<UserAudit> getAllUsers() {
    log.info("Reading all user audits");
    SimpleStatement statement = SimpleStatement.newInstance("SELECT * FROM my_keyspace.user_audit");
    ResultSet result = session.execute(statement);
    ArrayList<UserAudit> users = new ArrayList<>();
    for (Row row : result.all()) {
      users.add(
          new UserAudit(
              row.getUuid("user_id"),
              row.getInstant("event_time"),
              Action.valueOf(row.getString("event_type")),
              row.getString("event_details")));
    }
    return users;
  }
}
