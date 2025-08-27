// src/main/java/com/example/pipeline/CassandraSinkFunction.java
package com.example.pipeline;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

public class CassandraSinkFunction extends RichSinkFunction<AnomalyScore> {
    private transient CqlSession session;
    private transient PreparedStatement insertStmt;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("cassandra", 9042))
                .withLocalDatacenter("datacenter1")
                .build();

        // Schema bootstrap (idempotent)
        session.execute(
                "CREATE KEYSPACE IF NOT EXISTS flink_keyspace " +
                        "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");

        session.execute(
                "CREATE TABLE IF NOT EXISTS flink_keyspace.events (" +
                        "  id uuid PRIMARY KEY," +
                        "  sensor_id text," +
                        "  ts timestamp," +
                        "  value double," +
                        "  anomaly boolean," +
                        "  score double" +
                        ")");

        // Use IF NOT EXISTS + app-generated deterministic UUID for idempotency
        insertStmt = session.prepare(
                "INSERT INTO flink_keyspace.events " +
                        "(id, sensor_id, ts, value, anomaly, score) " +
                        "VALUES (?, ?, ?, ?, ?, ?) IF NOT EXISTS");
    }

    @Override
    public void invoke(AnomalyScore value, Context context) {
        // Deterministic ID per event: sensorId + timestamp → UUIDv5-like name UUID
        String key = value.event.sensorId + "|" + value.event.timestamp;
        UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));

        Instant ts = Instant.ofEpochMilli(value.event.timestamp);

        BoundStatement bound = insertStmt.bind(
                id,
                value.event.sensorId,
                ts,
                value.event.value,
                value.anomaly,
                value.score);

        session.execute(bound);
    }

    @Override
    public void close() throws Exception {
        if (session != null)
            session.close();
        super.close();
    }
}
