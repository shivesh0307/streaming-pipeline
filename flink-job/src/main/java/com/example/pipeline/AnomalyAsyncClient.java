package com.example.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class AnomalyAsyncClient extends RichAsyncFunction<Event, AnomalyScore> {
    private transient OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final ObjectMapper M = new ObjectMapper();
    private final String url;

    public AnomalyAsyncClient(String url) {
        this.url = url;
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        client = new OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).build();
    }

    @Override
    public void asyncInvoke(Event e, ResultFuture<AnomalyScore> rf) throws Exception {
        String body = M.writeValueAsString(e);
        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.getBytes(StandardCharsets.UTF_8), JSON))
                .build();

        client.newCall(req).enqueue(new okhttp3.Callback() { // ✅ use okhttp3.Callback
            @Override
            public void onFailure(Call call, java.io.IOException ex) {
                rf.completeExceptionally(ex);
            }

            @Override
            public void onResponse(Call call, Response resp) throws java.io.IOException {
                try (ResponseBody responseBody = resp.body()) {
                    if (resp.isSuccessful() && responseBody != null) {
                        ScoreResponse score = M.readValue(responseBody.bytes(), ScoreResponse.class);
                        rf.complete(Collections.singleton(new AnomalyScore(e, score.anomaly, score.score)));
                    } else {
                        rf.completeExceptionally(new java.io.IOException("Unexpected code " + resp));
                    }
                } catch (Exception ex) {
                    rf.completeExceptionally(ex);
                }
            }
        });
    }

    public static class ScoreResponse {
        public boolean anomaly;
        public double score;

        public ScoreResponse() {
        }

        public ScoreResponse(boolean anomaly, double score) {
            this.anomaly = anomaly;
            this.score = score;
        }

        @Override
        public String toString() {
            return "ScoreResponse{" +
                    "anomaly=" + anomaly +
                    ", score=" + score +
                    '}';
        }
    }
}
