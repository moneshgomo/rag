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

    /**
     * One-click action endpoint clicked from email inbox.
     * Returns an HTML confirmation page.
     */
    @GetMapping(value = "/{token}/action", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleActionGet(@PathVariable String token) {
        String htmlResponse = tokenActionService.executeAction(token);
        return ResponseEntity.ok(htmlResponse);
    }

    /**
     * Programmatic POST action for UI client.
     */
    @PostMapping(value = "/{token}/action", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleActionPost(@PathVariable String token) {
        String htmlResponse = tokenActionService.executeAction(token);
        return ResponseEntity.ok(htmlResponse);
    }
}