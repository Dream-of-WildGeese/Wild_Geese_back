package com.ondam.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SttClient {

    private final WebClient openAiWebClient;

    public String transcribe(MultipartFile audioFile) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", audioFile.getResource());
            builder.part("model", "whisper-1");

            Map<String, Object> response = openAiWebClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(15));

            return (String) response.get("text");

        } catch (Exception e) {
            return null;
        }
    }
}