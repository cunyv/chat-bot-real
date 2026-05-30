package com.sports.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sports.customer.config.MimoConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 小米mimo API服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MimoService {

    private final MimoConfig mimoConfig;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 聊天补全
     */
    public String chatCompletion(List<ChatMessage> messages) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", mimoConfig.getChatModel());
            requestBody.put("max_tokens", mimoConfig.getMaxTokens());
            requestBody.put("temperature", mimoConfig.getTemperature());

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (ChatMessage msg : messages) {
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", msg.getRole());
                messageNode.put("content", msg.getContent());
            }

            String requestJson = requestBody.toString();
            log.debug("发送请求: {}", requestJson);

            Request request = new Request.Builder()
                    .url(mimoConfig.getBaseUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + mimoConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "unknown error";
                    log.error("Chat API调用失败: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("Chat API调用失败: " + response.code());
                }
                String responseBody = response.body().string();
                log.debug("收到响应: {}", responseBody);

                JsonNode json = objectMapper.readTree(responseBody);
                return json.get("choices").get(0).get("message").get("content").asText();
            }
        } catch (IOException e) {
            log.error("聊天补全失败", e);
            throw new RuntimeException("聊天补全失败: " + e.getMessage());
        }
    }

    /**
     * 流式聊天补全
     */
    public void chatCompletionStream(List<ChatMessage> messages, StreamCallback callback) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", mimoConfig.getChatModel());
            requestBody.put("max_tokens", mimoConfig.getMaxTokens());
            requestBody.put("temperature", mimoConfig.getTemperature());
            requestBody.put("stream", true);

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (ChatMessage msg : messages) {
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", msg.getRole());
                messageNode.put("content", msg.getContent());
            }

            Request request = new Request.Builder()
                    .url(mimoConfig.getBaseUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + mimoConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("流式聊天失败", e);
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError(new RuntimeException("API调用失败: " + response.code()));
                        return;
                    }
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) {
                                    callback.onComplete();
                                    return;
                                }
                                JsonNode json = objectMapper.readTree(data);
                                String content = json.get("choices").get(0)
                                        .get("delta").get("content").asText("");
                                if (!content.isEmpty()) {
                                    callback.onContent(content);
                                }
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("流式聊天失败", e);
            callback.onError(e);
        }
    }

    /**
     * 聊天消息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }

    /**
     * 流式回调接口
     */
    public interface StreamCallback {
        void onContent(String content);
        void onComplete();
        void onError(Throwable error);
    }
}
