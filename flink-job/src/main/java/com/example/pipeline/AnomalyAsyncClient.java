// src/main/java/com/example/pipeline/AnomalyAsyncClient.java
package com.example.pipeline;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AnomalyAsyncClient extends RichAsyncFunction<Event, AnomalyScore> {
    private transient OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final ObjectMapper M = new ObjectMapper();
    private final String url;

    private static AnomalyScore FALLBACK(Event e) {
        return new AnomalyScore(e, false, 0.0);
    }

    public AnomalyAsyncClient(String url) {
        this.url = url;
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        // Keep these timeouts comfortably below the AsyncWait timeout you set in the
        // job
        client = new OkHttpClient.Builder()
                .connectTimeout(800, TimeUnit.MILLISECONDS)
                .readTimeout(1200, TimeUnit.MILLISECONDS)
                .callTimeout(1800, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public void asyncInvoke(Event e, ResultFuture<AnomalyScore> rf) throws Exception {
        // Up to 2 attempts; totally non-blocking
        performRequest(e, rf, 2);
    }

    private void performRequest(Event e, ResultFuture<AnomalyScore> rf, int attemptsRemaining) throws Exception {
        String body = M.writeValueAsString(e);
        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.getBytes(StandardCharsets.UTF_8), JSON))
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException ex) {
                if (attemptsRemaining > 1) {
                    try {
                        performRequest(e, rf, attemptsRemaining - 1);
                    } catch (Exception ignore) {
                        rf.complete(Collections.singleton(FALLBACK(e)));
                    }
                } else {
                    rf.complete(Collections.singleton(FALLBACK(e)));
                }
            }

            @Override
            public void onResponse(Call call, Response resp) {
                try (ResponseBody responseBody = resp.body()) {
                    if (resp.isSuccessful() && responseBody != null) {
                        ScoreResponse score = M.readValue(responseBody.bytes(), ScoreResponse.class);
                        rf.complete(Collections.singleton(new AnomalyScore(e, score.anomaly, score.score)));
                    } else {
                        rf.complete(Collections.singleton(FALLBACK(e)));
                    }
                } catch (Exception ex) {
                    rf.complete(Collections.singleton(FALLBACK(e)));
                }
            }
        });
    }

    /** Non-throwing timeout: emit a safe fallback so the job keeps running. */
    @Override
    public void timeout(Event input, ResultFuture<AnomalyScore> rf) {
        rf.complete(Collections.singleton(FALLBACK(input)));
    }

    // Response DTO
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
            return "ScoreResponse{anomaly=" + anomaly + ", score=" + score + "}";
        }
    }
}
