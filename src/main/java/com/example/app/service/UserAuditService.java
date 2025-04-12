package com.example.app.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.example.app.model.Action;
import com.example.app.model.UserAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAuditService {

  private CqlSession session;
  private static final String INSERT_INTO_USER_AUDIT_QUERY =
      """
      INSERT INTO my_keyspace.user_audit (user_id, event_time, event_type, event_details)
      VALUES (?, ?, ?, ?)
      """;
  private static final String SELECT_ALL_USER_AUDIT_QUERY =
      """
      SELECT user_id, event_time, event_type, event_details
      FROM my_keyspace.user_audit
      WHERE user_id = ?)
      """;

  public void insertUserAction(Action action, String eventDetails) {
    PreparedStatement preparedStatement = session.prepare(INSERT_INTO_USER_AUDIT_QUERY);
    BoundStatement boundStatement =
        preparedStatement.bind(
            java.util.UUID.randomUUID(), java.time.Instant.now(), action, eventDetails);

    session.execute(boundStatement);
  }

  public List<UserAudit> readUserAction(UUID userId) {
    PreparedStatement preparedStatement = session.prepare(SELECT_ALL_USER_AUDIT_QUERY);
    BoundStatement boundStatement = preparedStatement.bind(userId);
    ResultSet result = session.execute(boundStatement);

    List<UserAudit> users = new ArrayList<>();
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
