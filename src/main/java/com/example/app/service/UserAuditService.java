package com.example.app.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.example.app.dto.MessageDto;
import com.example.app.model.Action;
import com.example.app.model.UserAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserAuditService {
  private final CqlSession session;
  private final PreparedStatement insertUserAuditStatement;
  private final PreparedStatement selectUserAuditsStatement;

  private static final String INSERT_INTO_USER_AUDIT_QUERY =
      """
      INSERT INTO user_audit (user_id, event_time, event_type, event_details)
      VALUES (?, ?, ?, ?);
      """;

  private static final String SELECT_USER_AUDIT_QUERY =
      """
      SELECT user_id, event_time, event_type, event_details
      FROM user_audit
      WHERE user_id = ?;
      """;
  private static final String SELECT_ALL_USER_AUDIT_QUERY =
      """
      SELECT *
      FROM user_audit;
      """;

  @Autowired
  public UserAuditService(CqlSession session) {
    this.session = session;
    this.insertUserAuditStatement = session.prepare(INSERT_INTO_USER_AUDIT_QUERY);
    this.selectUserAuditsStatement = session.prepare(SELECT_USER_AUDIT_QUERY);
  }

  public void insertUserAudit(Long userId, Action action, String eventDetails) {
    log.info("Inserting user audit for {} with action {}", userId, action);
    BoundStatement boundStatement =
        insertUserAuditStatement.bind(
            userId, java.time.Instant.now(), action.toString(), eventDetails);
    session.execute(boundStatement);
  }

  public void saveMessage(MessageDto message) {
    log.info("save message");
    BoundStatement boundStatement =
        insertUserAuditStatement.bind(
            1L, Instant.now(), message.getAction().toString(), message.getEventDetails());
    session.execute(boundStatement);
  }

  public List<UserAudit> readUserAudit(Long userId) {
    log.info("Reading user audit for {}", userId);
    BoundStatement boundStatement = selectUserAuditsStatement.bind(userId);
    ResultSet result = session.execute(boundStatement);
    ArrayList<UserAudit> users = new ArrayList<>();
    for (Row row : result.all()) {
      users.add(
          new UserAudit(
              row.getLong("user_id"),
              row.getInstant("event_time"),
              Action.valueOf(row.getString("event_type")),
              row.getString("event_details")));
    }
    return users;
  }

  List<UserAudit> getAllUsers() {
    log.info("Reading all user audits");
    ResultSet result = session.execute(SELECT_ALL_USER_AUDIT_QUERY);
    ArrayList<UserAudit> users = new ArrayList<>();
    for (Row row : result.all()) {
      users.add(
          new UserAudit(
              row.getLong("user_id"),
              row.getInstant("event_time"),
              Action.valueOf(row.getString("event_type")),
              row.getString("event_details")));
    }
    return users;
  }
}
