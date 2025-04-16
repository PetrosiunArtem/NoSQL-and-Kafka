package com.example.app.service;

import com.example.app.dto.MessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaListener.class);
  private final ObjectMapper objectMapper;
  private final UserAuditService userAuditService;

  public KafkaConsumerService(ObjectMapper objectMapper, UserAuditService userAuditService) {
    this.objectMapper = objectMapper;
    this.userAuditService = userAuditService;
  }

  @KafkaListener(topics = {"${topic-to-consume-message}"})
  public void consumeMessage(String message) throws JsonProcessingException {
    MessageDto parsedMessage = objectMapper.readValue(message, MessageDto.class);
    LOGGER.info("Retrieved message {}", message);
    userAuditService.saveMessage(parsedMessage);
  }
}
