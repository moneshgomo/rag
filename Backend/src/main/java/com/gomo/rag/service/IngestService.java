package com.gomo.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomo.rag.config.VectorStoreConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
public class IngestService {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public synchronized int ingestLeavePolicy() throws IOException {
        log.info("[IngestService] Starting controlled leave policy ingestion...");

        String text = new ClassPathResource("docs/leave-policy.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        // Split into coherent policy sections using markdown headers "## "
        List<String> rawSections = splitIntoPolicySections(text);
        log.info("[IngestService] Extracted {} coherent policy sections from leave-policy.txt", rawSections.size());

        List<Document> chunks = new ArrayList<>();
        int sectionIndex = 1;
        for (String section : rawSections) {
            String content = section.trim();
            if (content.length() < 20) {
                continue; // Skip trivial whitespace or isolated dividers
            }
            Map<String, Object> metadata = Map.of(
                    "source", "leave-policy.txt",
                    "section", sectionIndex++
            );
            // Use deterministic UUID based on content to prevent duplicate accumulation
            String docId = UUID.nameUUIDFromBytes(content.getBytes(StandardCharsets.UTF_8)).toString();
            chunks.add(new Document(docId, content, metadata));
        }

        // 1. Back up existing store file if it exists
        if (VectorStoreConfig.STORE_FILE.exists()) {
            File backup = new File(VectorStoreConfig.STORE_FILE.getParentFile(),
                    "vectorstore.json.bak-" + System.currentTimeMillis());
            try {
                Files.copy(VectorStoreConfig.STORE_FILE.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("[IngestService] Existing vector store backed up to: {}", backup.getAbsolutePath());
            } catch (Exception e) {
                log.warn("[IngestService] Could not create backup of existing vectorstore file: {}", e.getMessage());
            }
        }

        // 2. Clear previous entries in in-memory store so vectors from older models or duplicate chunks do not coexist
        clearStore((SimpleVectorStore) vectorStore);

        // 3. Add chunks with rate-limit throttling (OpenRouter free tier allows 20 requests/minute)
        // With ~11 chunks and a 3.2-second pause between requests, we stay strictly below the 20 RPM ceiling.
        int storedCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            int attempt = 0;
            boolean success = false;
            while (!success && attempt < 3) {
                attempt++;
                try {
                    log.info("[IngestService] Ingesting chunk {}/{} (attempt {})...", (i + 1), chunks.size(), attempt);
                    vectorStore.add(List.of(chunk));
                    storedCount++;
                    success = true;
                    // Rate-limit throttle: pause 3.2 seconds between chunks to avoid 429
                    if (i < chunks.size() - 1) {
                        Thread.sleep(3200);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Ingestion interrupted", ie);
                } catch (Exception ex) {
                    log.warn("[IngestService] Failed chunk {}/{} on attempt {}: {}", (i + 1), chunks.size(), attempt, ex.getMessage());
                    if (attempt >= 3) {
                        throw new RuntimeException("Ingestion failed after 3 attempts on chunk " + (i + 1) + ": " + ex.getMessage(), ex);
                    }
                    try {
                        log.info("[IngestService] Rate limit backoff: waiting 8 seconds before retry...");
                        Thread.sleep(8000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Ingestion interrupted during retry backoff", ie);
                    }
                }
            }
        }

        // 4. Save updated store to disk
        ((SimpleVectorStore) vectorStore).save(VectorStoreConfig.STORE_FILE);
        log.info("[IngestService] Ingestion completed successfully. {} chunks persisted to {}",
                storedCount, VectorStoreConfig.STORE_FILE.getAbsolutePath());

        return storedCount;
    }

    private void clearStore(SimpleVectorStore store) {
        if (!VectorStoreConfig.STORE_FILE.exists()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(VectorStoreConfig.STORE_FILE);
            List<String> ids = new ArrayList<>();
            root.fieldNames().forEachRemaining(ids::add);
            if (!ids.isEmpty()) {
                store.delete(ids);
                log.info("[IngestService] Cleared {} old entries from in-memory vector store", ids.size());
            }
        } catch (Exception e) {
            log.warn("[IngestService] Could not parse existing store file to clear: {}", e.getMessage());
        }
    }

    private List<String> splitIntoPolicySections(String text) {
        // Split on Markdown H2 headers "## "
        String[] raw = text.split("(?m)(?=^## )");
        List<String> sections = new ArrayList<>();
        for (String s : raw) {
            String trimmed = s.trim().replaceAll("(?m)^---+\\s*", "").trim();
            if (!trimmed.isEmpty()) {
                sections.add(trimmed);
            }
        }
        return sections;
    }
}

