package com.example.app.configuration;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import java.net.InetSocketAddress;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScyllaConfig {

  @Value("${scylla.port}")
  private int port;

  @Value("${scylla.keyspace}")
  private String keyspace;

  @Value("${scylla.datacenter}")
  private String datacenter;

  @Value("${scylla.host}")
  private String host;

  @Bean
  public CqlSession cqlSession(CqlSessionBuilder sessionBuilder) {
    InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
    sessionBuilder = sessionBuilder.addContactPoint(address);
    sessionBuilder.withKeyspace((CqlIdentifier) null).withLocalDatacenter(datacenter);

    try (CqlSession session = sessionBuilder.build()) {

      SimpleStatement statement =
          SchemaBuilder.createKeyspace(keyspace)
              .ifNotExists()
              .withNetworkTopologyStrategy(Map.of(datacenter, 1))
              .build();
      session.execute(statement);
      session.execute(
          String.format(
              """
                   CREATE TABLE IF NOT EXISTS %s.user_audit (
                       user_id UUID,
                       event_time TIMESTAMP,
                       event_type TEXT,
                       event_details TEXT,
                       PRIMARY KEY (user_id, event_time)
                   ) WITH CLUSTERING ORDER BY (event_time DESC)
                      AND default_time_to_live = 31536000;
                   """,
              keyspace));

      return sessionBuilder.withKeyspace(keyspace).build();
    }
  }
}
