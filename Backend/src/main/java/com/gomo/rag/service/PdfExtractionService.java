package com.gomo.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfExtractionService {

    @Value("${pdfco.api-key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PdfExtractionService() {
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public String extractText(MultipartFile file) throws IOException {

        String uploadedFileUrl = uploadPdf(file);

        String resultFileUrl = convertPdfToText(uploadedFileUrl);

        Path destinationFile = Path.of("bill.txt");

        downloadFile(resultFileUrl, destinationFile);

        return Files.readString(
                destinationFile,
                StandardCharsets.UTF_8
        );
    }

    private String uploadPdf(MultipartFile file) throws IOException {

        String fileName = file.getOriginalFilename();

        String presignedResponse =
                restClient.get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .scheme("https")
                                        .host("api.pdf.co")
                                        .path("/v1/file/upload/get-presigned-url")
                                        .queryParam(
                                                "contenttype",
                                                "application/octet-stream"
                                        )
                                        .queryParam(
                                                "name",
                                                fileName
                                        )
                                        .build()
                        )
                        .header("x-api-key", apiKey)
                        .retrieve()
                        .body(String.class);

        JsonNode json = objectMapper.readTree(presignedResponse);

        if (json.path("error").asBoolean()) {
            throw new RuntimeException(
                    "PDF.co upload URL failed: "
                            + json.path("message").asText()
            );
        }

        String presignedUrl = json.path("presignedUrl").asText();

        String uploadedFileUrl = json.path("url").asText();

        restClient.put()
                .uri(java.net.URI.create(presignedUrl))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.getBytes())
                .retrieve()
                .toBodilessEntity();

        return uploadedFileUrl;
    }

    private String convertPdfToText(String uploadedFileUrl) throws IOException {

        String requestBody = objectMapper.writeValueAsString(new PdfToTextRequest("bill.txt", "", "", uploadedFileUrl));

        String SOURCE_URL = "https://api.pdf.co/v1/pdf/convert/to/text";

        String response = restClient.post()
                        .uri(SOURCE_URL)
                        .header("x-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

        JsonNode json = objectMapper.readTree(response);

        if (json.path("error").asBoolean()) {
            throw new RuntimeException(
                    "PDF.co text extraction failed: "
                            + json.path("message").asText()
            );
        }

        return json.path("url").asText();
    }


    private void downloadFile(
            String fileUrl,
            Path destinationFile
    ) throws IOException {

        URI downloadUri = UriComponentsBuilder
                .fromUriString(fileUrl)
                .build(true)
                .toUri();

        byte[] fileBytes =
                restClient.get()
                        .uri(downloadUri)
                        .retrieve()
                        .body(byte[].class);

        if (fileBytes == null) {
            throw new RuntimeException(
                    "PDF.co returned empty TXT file"
            );
        }

        Files.write(
                destinationFile,
                fileBytes
        );
    }

    private record PdfToTextRequest(
            String name,
            String password,
            String pages,
            String url
    ) {
    }
}