package com.example.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Table(value = "user_audit")
public class UserAudit {

  @PrimaryKeyColumn(value = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
  private UUID userId;

  @PrimaryKeyColumn(value = "event_time", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
  private Instant eventTime;

  @Column(value = "event_type")
  private Action eventType;

  @Column(value = "event_details")
  private String eventDetails;
}
