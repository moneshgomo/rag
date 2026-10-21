
package com.gomo.rag.controller;

import com.gomo.rag.config.VectorStoreConfig;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PolicyIngestionController {

    private final VectorStore vectorStore;

    public PolicyIngestionController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping(
            value = "/ingest",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> ingest(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please upload a non-empty file."));
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                !filename.toLowerCase().endsWith(".txt")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only .txt files are supported."));
        }

        String text = new String(
                file.getBytes(), StandardCharsets.UTF_8);

        List<Document> documents = chunkText(text, filename);

        if (documents.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "The file contains no text."));
        }

        vectorStore.add(documents);

        if (vectorStore instanceof SimpleVectorStore simpleStore) {
            simpleStore.save(VectorStoreConfig.STORE_FILE);
        } else {
            throw new IllegalStateException(
                    "Expected a SimpleVectorStore for file persistence.");
        }

        return ResponseEntity.ok(Map.of(
                "message", "Policy ingested successfully",
                "filename", filename,
                "chunksCreated", documents.size(),
                "storagePath",
                VectorStoreConfig.STORE_FILE.getAbsolutePath()
        ));
    }

    private List<Document> chunkText(String text, String filename) {
        int chunkSize = 1000;
        int overlap = 150;
        List<Document> documents = new ArrayList<>();

        text = text.trim();

        for (int start = 0; start < text.length();) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                documents.add(new Document(
                        chunk,
                        Map.of("source", filename)
                ));
            }

            if (end == text.length()) {
                break;
            }

            start = end - overlap;
        }

        return documents;
    }
}
