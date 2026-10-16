package com.gomo.rag.controller;

import com.gomo.rag.service.IngestService;
import com.gomo.rag.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RagController {

    private final IngestService ingestService;
    private final RagService ragService;

    public RagController(IngestService ingestService, RagService ragService) {
        this.ingestService = ingestService;
        this.ragService = ragService;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest() throws IOException {
        int count = ingestService.ingestLeavePolicy();
        return Map.of(
                "status", "done",
                "chunksStored", count,
                "message", "Leave policy successfully ingested into vector store."
        );
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        return ragService.search(q).stream()
                .map(d -> Map.<String, Object>of(
                        "score", d.getScore() != null ? String.valueOf(d.getScore()) : "0.0",
                        "text", d.getText() != null ? d.getText() : ""))
                .toList();
    }

    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam String q) {
        return Map.of("question", q, "answer", ragService.ask(q));
    }
}