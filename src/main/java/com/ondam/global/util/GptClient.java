package com.ondam.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class GptClient {

    private final WebClient openAiWebClient;

    @Value("${openai.model:dummy-model}")
    private String model;

    public String ask(String prompt) {

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 1.1
        );

        try {
            Map<String, Object> response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(15));

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            return (String) message.get("content");

        } catch (WebClientResponseException e) {
            System.err.println("[GPT_DEBUG] 상태 코드: " + e.getStatusCode());
            System.err.println("[GPT_DEBUG] 응답 본문: " + e.getResponseBodyAsString());
            throw e;
        }
    }
}