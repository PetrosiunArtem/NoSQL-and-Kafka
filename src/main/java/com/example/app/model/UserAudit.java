package com.example.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Table(value = "user_audit")
public class UserAudit {
    
    @PrimaryKey
    @Column("user_id")
    @Id
    private UUID user_id;

    @PrimaryKey
    @Column("event_time")
    @Id
    private Instant event_time;

    @Column(value = "event_type")
    private Action event_type;

    @Column(value = "event_details")
    private String event_details;
}