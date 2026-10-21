package com.gomo.rag.controller;

import com.gomo.rag.service.TokenActionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/token")
@CrossOrigin(origins = "*")
public class TokenActionController {

    private final TokenActionService tokenActionService;

    public TokenActionController(TokenActionService tokenActionService) {
        this.tokenActionService = tokenActionService;
    }

    @GetMapping(value = "/{token}/action", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleActionGet(@PathVariable String token) {
        String htmlResponse = tokenActionService.executeAction(token);
        return ResponseEntity.ok(htmlResponse);
    }

    @PostMapping(value = "/{token}/action", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleActionPost(@PathVariable String token) {
        String htmlResponse = tokenActionService.executeAction(token);
        return ResponseEntity.ok(htmlResponse);
    }
}