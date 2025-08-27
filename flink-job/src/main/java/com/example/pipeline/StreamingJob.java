// src/main/java/com/example/pipeline/StreamingJob.java
package com.example.pipeline;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

public class StreamingJob {
        public static void main(String[] args) throws Exception {
                final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

                Properties props = new Properties();
                props.setProperty("bootstrap.servers", "kafka:9092");
                props.setProperty("group.id", "flink-anomaly-group");
                props.setProperty("auto.offset.reset", "latest");

                DataStreamSource<Event> events = env.addSource(
                                new FlinkKafkaConsumer<>("raw-events", new JsonEventDeserializationSchema(), props));

                var scored = AsyncDataStream.unorderedWait(
                                events,
                                new AnomalyAsyncClient("http://anomaly-service:8000/score"),
                                5, TimeUnit.SECONDS, // give a bit more room than the HTTP callTimeout
                                200 // max in-flight async requests
                );

                scored.addSink(new CassandraSinkFunction());

                env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                                10, org.apache.flink.api.common.time.Time.seconds(10)));

                env.execute("Real-time Anomaly Detection Pipeline");
        }
}
