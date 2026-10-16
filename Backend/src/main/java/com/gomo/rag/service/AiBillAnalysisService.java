package com.gomo.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiBillAnalysisService {

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiBillAnalysisService() {
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public String analyzeBill(String billText) throws Exception {

        String prompt = """
                Analyze the following extracted bill text.

                Extract and summarize the bill context.

                Return only the following information:

                Vendor:
                Bill Number:
                Bill Date:
                Billing Period:
                Bill Amount:
                Category:
                Bill Context:

                Bill Text:
                ----------------
                %s
                ----------------
                """.formatted(billText);

        String requestBody = objectMapper.writeValueAsString(
                new GroqRequest(
                        model,
                        new GroqMessage[]{
                                new GroqMessage(
                                        "system",
                                        "You are a bill analysis assistant."
                                ),
                                new GroqMessage(
                                        "user",
                                        prompt
                                )
                        }
                )
        );

        String response =
                restClient.post()
                        .uri(
                                "https://api.groq.com/openai/v1/chat/completions"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

        JsonNode json = objectMapper.readTree(response);

        return json
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }

    record GroqRequest(
            String model,
            GroqMessage[] messages
    ) {}

    record GroqMessage(
            String role,
            String content
    ) {}
}