package com.ssinspector.backend.controller;

import com.ssinspector.backend.model.AnalysisResponse;
import com.ssinspector.backend.scanner.SimpleScanner;
import com.ssinspector.backend.service.ApkAnalysisService;
import com.ssinspector.backend.service.AnalysisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST Controller for the Analysis API.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final AnalysisService analysisService;
    private final ApkAnalysisService apkAnalysisService;
    private final SimpleScanner scanner;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Accepts a text input or a file for analysis.
     * Supports TXT, XML, JSON, and APK files.
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeMultipart(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return analyzeInput(text, file, "raw_text");
    }

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> analyzeForm(@RequestParam(value = "text", required = false) String text) {
        return analyzeInput(text, null, "raw_text");
    }

    @PostMapping(value = "/analyze", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> analyzePlainText(@RequestBody(required = false) String text) {
        return analyzeInput(text, null, "raw_text");
    }

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyzeJson(@RequestBody(required = false) AnalyzeTextRequest request) {
        if (request == null) {
            return badRequest();
        }

        String source = isBlank(request.getSource()) ? "raw_text" : request.getSource().trim();
        return analyzeInput(request.getText(), null, source);
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFallback(@RequestParam(value = "text", required = false) String text) {
        return analyzeInput(text, null, "raw_text");
    }

    private ResponseEntity<?> analyzeInput(String text, MultipartFile file, String source) {
        try {
            if (file != null && !file.isEmpty()) {
                String filename = isBlank(file.getOriginalFilename()) ? "uploaded_file" : file.getOriginalFilename();

                if (filename != null && filename.toLowerCase().endsWith(".apk")) {
                    AnalysisResponse response = apkAnalysisService.analyzeApk(file);
                    return ResponseEntity.ok(response);
                }

                String content = scanner.scanFile(file);
                AnalysisResponse response = analysisService.analyze(content, filename);
                return ResponseEntity.ok(response);

            } else if (!isBlank(text)) {
                String content = scanner.scanText(text);
                AnalysisResponse response = analysisService.analyze(content, source);
                return ResponseEntity.ok(response);
            } else {
                return badRequest();
            }
        } catch (Exception e) {
            log.error("Analysis request failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Analysis failed", "message", e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, String>> badRequest() {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Provide a non-empty text field, text/plain body, JSON text value, or uploaded file."));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Data
    private static class AnalyzeTextRequest {
        private String text;
        private String source;
    }
}
