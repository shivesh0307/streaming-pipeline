package com.example.pipeline;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class StreamingJob {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka source
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "kafka:9092");
        props.setProperty("group.id", "flink-anomaly-group");

        DataStreamSource<Event> events = env
                .addSource(new FlinkKafkaConsumer<>("raw-events",
                        new JsonEventDeserializationSchema(),
                        props));

        // Async call to Python ML service
        var scored = AsyncDataStream.unorderedWait(
                events,
                new AnomalyAsyncClient("http://anomaly-service:8000/score"),
                3, TimeUnit.SECONDS, 100);

        // Sink to Cassandra
        scored.addSink(new CassandraSinkFunction());

        env.execute("Real-time Anomaly Detection Pipeline");
    }
}
