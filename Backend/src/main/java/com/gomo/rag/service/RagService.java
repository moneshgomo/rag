package com.gomo.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {

    private static final String SYSTEM_PROMPT = """
            You are FinFlow's HR Leave Policy Advisor.

            Evaluate employee requests using ONLY the retrieved
            company policy provided in the context.

            Supported categories:
            CASUAL, MEDICAL, OFFICIAL_DUTY.

            EVALUATION RULES:

            1. Identify the applicable leave category and policy rules.

            2. Evaluate requested dates, duration, reason, advance
               notice, eligibility, and applicable restrictions.

            3. CASUAL:
               Check personal leave entitlement, consecutive-day
               limits, and notice requirements.

            4. MEDICAL:
               Evaluate requests sensitively. Digital prescription
               uploads are optional. Explain applicable physical
               document submission requirements after returning
               to the office. Never diagnose medical conditions.

            5. OFFICIAL_DUTY:
               Evaluate the stated business purpose. Approved official
               duty must not consume personal or medical leave balance.
               Mention applicable physical document requirements.

            6. DOCUMENTS:
               Where applicable, employees submit supporting documents
               physically within three working days after returning
               to the office. Never assume documents are verified.

            7. HOLIDAYS:
               Consider working days and company holidays only when
               supporting calendar information is available.

            8. Never invent policy rules, leave balances, holidays,
               employee information, or business commitments.

            9. If information is insufficient, explicitly identify
               what requires manager or HR verification.

            10. Never automatically approve or reject requests.
                The reporting manager makes the final decision.

            OUTPUT:
            Provide a concise, professional manager-facing advisory:
            - Policy assessment
            - Relevant concerns
            - Practical recommendation, if applicable

            Use plain text without Markdown or HTML.
            Maximum 100 words.
        """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public List<Document> search(String question) {
        if (question == null || question.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            log.info("[RAG] Performing similarity search for query: '{}'", question);
            List<Document> result = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question.trim()).topK(3).build());
            log.info("[RAG] Similarity search returned {} matching documents", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (IllegalArgumentException iae) {
            log.error("[RAG] Vector dimension mismatch during search: {}. The vector store needs to be re-ingested with the active embedding model.", iae.getMessage());
            throw iae;
        } catch (Exception e) {
            log.error("[RAG] Error during similarity search: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String ask(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "Please provide a valid leave question.";
        }

        try {
            log.info("[RAG] Processing question: '{}'", question);

            List<Document> docs = search(question);

            if (docs.isEmpty()) {
                log.warn("[RAG] No policy documents found matching question. Vector store may be unpopulated.");
                return "No leave policy context is currently available. Please ensure the policy has been ingested via POST /api/ingest.";
            }

            String context = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));

            String userMessage = "Context:\n" + context + "\n\nQuestion: " + question;

            log.info("[RAG] Retrieved {} context documents. Calling LLM...", docs.size());

            String answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();

            log.info("[RAG] LLM advisory generated successfully ({} chars)", answer != null ? answer.length() : 0);
            return answer != null ? answer.trim() : "No response generated by the advisor.";

        } catch (Exception e) {
            log.error("[RAG] Failed to generate answer for question '{}': {}", question, e.getMessage(), e);
            throw e;
        }
    }
}