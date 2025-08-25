package com.example.pipeline;

import com.datastax.oss.driver.api.core.CqlSession;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.net.InetSocketAddress;

public class CassandraSinkFunction extends RichSinkFunction<AnomalyScore> {
    private transient CqlSession session;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("cassandra", 9042))
                .withLocalDatacenter("datacenter1")
                .build();
    }

    @Override
    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
        super.close();
    }

    @Override
    public void invoke(AnomalyScore value, Context context) {
        session.execute("INSERT INTO flink_keyspace.events (id, sensor_id, ts, value, anomaly, score) VALUES (uuid(), ?, ?, ?, ?, ?)",
                value.event.sensorId,
                value.event.timestamp,
                value.event.value,
                value.anomaly,
                value.score);
    }
}