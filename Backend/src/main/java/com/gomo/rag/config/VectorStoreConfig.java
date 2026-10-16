package com.gomo.rag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Configuration
public class VectorStoreConfig {

    public static final File STORE_FILE = new File("data/vectorstore.json");

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        File parent = STORE_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (STORE_FILE.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(STORE_FILE);

                int storedDim = -1;
                var iterator = root.elements();
                if (iterator.hasNext()) {
                    JsonNode firstDoc = iterator.next();
                    JsonNode embNode = firstDoc.get("embedding");
                    if (embNode != null && embNode.isArray()) {
                        storedDim = embNode.size();
                    }
                }

                if (storedDim > 0) {
                    log.info("[VectorStoreConfig] Found existing vector store with {} documents (embedding dimension: {})",
                            root.size(), storedDim);
                    store.load(STORE_FILE);
                    log.info("[VectorStoreConfig] Successfully loaded existing vector store from: {}", STORE_FILE.getAbsolutePath());
                } else {
                    log.warn("[VectorStoreConfig] Saved vector store file exists but contains no valid documents. Starting clean.");
                }
            } catch (Exception e) {
                log.warn("[VectorStoreConfig] Failed to load existing vector store: {}. Backing up and starting fresh.", e.getMessage());
                backupCorruptStore();
            }
        } else {
            log.info("[VectorStoreConfig] No saved vector store found at {}. Starting empty. Call POST /api/ingest to populate.", STORE_FILE.getAbsolutePath());
        }

        return store;
    }

    private void backupCorruptStore() {
        if (STORE_FILE.exists()) {
            try {
                File backup = new File(STORE_FILE.getParentFile(), "vectorstore.json.bak-" + System.currentTimeMillis());
                Files.move(STORE_FILE.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("[VectorStoreConfig] Stale/corrupt store backed up to: {}", backup.getAbsolutePath());
            } catch (Exception ex) {
                log.error("[VectorStoreConfig] Could not back up stale store: {}", ex.getMessage());
            }
        }
    }
}