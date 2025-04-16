package com.example.app.service;

import com.example.app.model.Action;
import com.example.app.model.UserAudit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.scylladb.ScyllaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class UserAuditServiceTest {

  @Container
  private static final ScyllaDBContainer scyllaDBContainer =
      new ScyllaDBContainer(DockerImageName.parse("scylladb/scylla:4.1.0"))
          .withExposedPorts(9042)
          .withStartupTimeout(Duration.ofSeconds(60))
          .withCommand("--smp 1");

  @Autowired private UserAuditService userAuditService;

  @BeforeAll
  static void setUp() {
    System.setProperty("scylla.port", String.valueOf(scyllaDBContainer.getMappedPort(9042)));
    scyllaDBContainer.start();
  }

  @Test
  void shouldSuccessfullyInsertUserAudit() {
    String eventDetails = "test event";
    userAuditService.insertUserAudit(1L, Action.INSERT, eventDetails);
    List<UserAudit> users = userAuditService.getAllUsers();
    assertFalse(users.isEmpty());
  }

  @Test
  void shouldSuccessfullyReadUserAudit() {
    String eventDetails = "test event";
    userAuditService.insertUserAudit(2L, Action.INSERT, eventDetails);
    List<UserAudit> userAudits = userAuditService.readUserAudit(2L);
    assertEquals(1, userAudits.size());
  }

  @Test
  void shouldSuccessfullyGetAllUsers() {
    userAuditService.insertUserAudit(3L, Action.INSERT, "test 1");
    userAuditService.insertUserAudit(4L, Action.INSERT, "test 2");
    userAuditService.insertUserAudit(5L, Action.INSERT, "test 3");
    assertTrue(3 <= userAuditService.getAllUsers().size());
  }

  @Test
  void shouldFailToInsertUserAudit() {
    String eventDetails = "test event";
    userAuditService.insertUserAudit(6L, Action.INSERT, eventDetails);
    userAuditService.insertUserAudit(6L, Action.INSERT, eventDetails);
    List<UserAudit> users = userAuditService.getAllUsers();
    assertNotEquals(1, users.size());
  }

  @Test
  void shouldFailToReadUserAudit() {
    Long firstUserId = 9L;
    Long secondUserId = 10L;
    String eventDetails = "test event";
    userAuditService.insertUserAudit(firstUserId, Action.INSERT, eventDetails);
    userAuditService.insertUserAudit(secondUserId, Action.SELECT, eventDetails);
    List<UserAudit> userAudits = userAuditService.readUserAudit(firstUserId);
    List<UserAudit> allUsers = userAuditService.getAllUsers();
    assertTrue(userAudits.size() < allUsers.size());
  }

  @Test
  void shouldFailToGetAllUsers() {
    userAuditService.insertUserAudit(11L, Action.INSERT, "test 1");
    userAuditService.insertUserAudit(12L, Action.INSERT, "test 2");
    userAuditService.insertUserAudit(13L, Action.INSERT, "test 3");
    assertNotEquals(1, userAuditService.getAllUsers().size());
  }
}
