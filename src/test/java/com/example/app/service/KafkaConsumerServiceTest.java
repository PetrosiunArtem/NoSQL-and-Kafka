package com.example.app.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.example.app.configuration.KafkaTopicConfig;
import com.example.app.configuration.ObjectMapperConfig;
import com.example.app.configuration.ScyllaConfig;
import com.example.app.dto.MessageDto;
import com.example.app.model.Action;
import com.example.app.model.UserAudit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import io.netty.channel.ChannelOutboundBuffer;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.scylladb.ScyllaDBContainer;
import org.testcontainers.utility.DockerImageName;
import java.util.List;

@SpringBootTest(classes = {KafkaConsumerService.class})
@Import({
  KafkaAutoConfiguration.class,
  KafkaTopicConfig.class,
  ObjectMapperConfig.class,
  UserAuditService.class,
  ScyllaConfig.class,
  CqlSessionBuilder.class
})
@Testcontainers
class KafkaConsumerServiceTest {

  @Container @ServiceConnection
  public static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

  @MockitoBean private ChannelOutboundBuffer.MessageProcessor messageProcessor;
  @Autowired private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserAuditService userAuditService;
  @Autowired private NewTopic topic;

  @Container
  private static final ScyllaDBContainer scyllaDBContainer =
      new ScyllaDBContainer(DockerImageName.parse("scylladb/scylla:4.1.0"))
          .withExposedPorts(9042)
          .withStartupTimeout(Duration.ofSeconds(60))
          .withCommand("--smp 1");

  @BeforeAll
  static void setUp() {
    scyllaDBContainer.start();
  }

  @Test
  void shouldSendMessageToKafkaSuccessfully() throws JsonProcessingException {

    MessageDto messageDto = new MessageDto(1L, Instant.now(), Action.INSERT, "insert file");
    String message = objectMapper.writeValueAsString(messageDto);
    kafkaTemplate.send(topic.name(), message);

    await()
        .atMost(Duration.ofSeconds(5))
        .pollDelay(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              List<UserAudit> usersAudits = userAuditService.getAllUsers();
              assertEquals(messageDto.getId(), usersAudits.getLast().getUserId());
              assertEquals(messageDto.getAction(), usersAudits.getLast().getEventType());
            });
  }

  @Test
  void shouldFailSendMessageToKafka() throws JsonProcessingException {

    MessageDto messageDto = new MessageDto(2L, Instant.now(), Action.SELECT, "select file");
    String message = objectMapper.writeValueAsString(messageDto);
    List<UserAudit> usersAuditsBefore = userAuditService.getAllUsers();
    kafkaTemplate.send("no_my_topic", message);

    await()
        .atMost(Duration.ofSeconds(5))
        .pollDelay(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              List<UserAudit> usersAuditsAfter = userAuditService.getAllUsers();
              assertEquals(usersAuditsAfter.size(), usersAuditsBefore.size());
            });
  }
}
