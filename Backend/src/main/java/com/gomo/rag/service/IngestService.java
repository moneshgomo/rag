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
        long startTime = System.currentTimeMillis();
        log.info("[IngestService] Starting fast local leave policy ingestion...");

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

        if (chunks.isEmpty()) {
            log.warn("[IngestService] No valid chunks extracted. Aborting ingestion.");
            return 0;
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

        // 2. Clear previous entries in in-memory store
        clearStore((SimpleVectorStore) vectorStore);

        // 3. Low-Latency Local Batch Processing
        // No loop, no network calls, no Thread.sleep(). The entire collection is embedded inside the JVM using your CPU.
        log.info("[IngestService] Generating local embeddings for all {} chunks simultaneously...", chunks.size());
        vectorStore.add(chunks);

        // 4. Save updated store to disk
        ((SimpleVectorStore) vectorStore).save(VectorStoreConfig.STORE_FILE);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[IngestService] Ingestion completed successfully in {} ms. {} chunks persisted to {}",
                duration, chunks.size(), VectorStoreConfig.STORE_FILE.getAbsolutePath());

        return chunks.size();
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
