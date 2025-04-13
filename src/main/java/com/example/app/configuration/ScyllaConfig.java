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

  @Bean
  public CqlSession cqlSession(CqlSessionBuilder sessionBuilder) {
    InetSocketAddress address = InetSocketAddress.createUnresolved("127.0.0.1", port);
    sessionBuilder = sessionBuilder.addContactPoint(address);
    sessionBuilder.withKeyspace((CqlIdentifier) null).withLocalDatacenter("datacenter1");

    try (CqlSession session = sessionBuilder.build()) {

      SimpleStatement statement =
          SchemaBuilder.createKeyspace("my_keyspace")
              .ifNotExists()
              .withNetworkTopologyStrategy(Map.of("datacenter1", 1))
              .build();
      session.execute(statement);
      session.execute(
          """
            CREATE TABLE IF NOT EXISTS my_keyspace.user_audit (
                user_id UUID,
                event_time TIMESTAMP,
                event_type TEXT,
                event_details TEXT,
                PRIMARY KEY (user_id, event_time)
            ) WITH CLUSTERING ORDER BY (event_time DESC)
               AND default_time_to_live = 31536000;
            """);

      return sessionBuilder.withKeyspace("my_keyspace").build();
    }
  }
}
