package com.ssinspector.backend.ai;

import com.ssinspector.backend.model.DetectedIssue;
import com.ssinspector.backend.model.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for communicating with the local LLaMA instance via Ollama.
 */
@Slf4j
@Service
public class LlamaService {

    private final RestTemplate restTemplate;

    public LlamaService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(300000);  // 5 minutes
        this.restTemplate = new RestTemplate(factory);
    }
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3:latest";
    private static final int MAX_FINDINGS_IN_PROMPT = 12;
    private static final int MAX_VALUE_LENGTH = 96;

    /**
     * Sends detected issues to LLaMA and returns a structured AI analysis.
     */
    public AIResponse generateAnalysis(List<DetectedIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return AIResponse.builder()
                    .explanation("No security issues detected to analyze.")
                    .remediation("N/A")
                    .codeExample("N/A")
                    .tests("N/A")
                    .build();
        }

        String prompt = buildPrompt(issues);

        try {
            log.info("Requesting AI analysis from Ollama using model: {}...", MODEL);
            long start = System.currentTimeMillis();
            
            Map<String, Object> request = new HashMap<>();
            request.put("model", MODEL);
            request.put("prompt", prompt);
            request.put("stream", false);
            request.put("format", "json");
 
            Map<String, Object> response = restTemplate.postForObject(OLLAMA_URL, request, Map.class);
            long duration = System.currentTimeMillis() - start;
 
            if (response != null && response.containsKey("response")) {
                log.info("AI analysis received in {}ms.", duration);
                String aiText = (String) response.get("response");
                return normalizeResponse(parseAIResponse(aiText), issues);
            }
        } catch (Exception e) {
            log.error("Failed to connect to Ollama server: {}", e.getMessage());
            return AIResponse.builder()
                    .explanation("AI Analysis is currently unavailable. Please ensure Ollama is running locally with the " + MODEL + " model.")
                    .remediation("Service Error: " + e.getLocalizedMessage())
                    .codeExample("// Could not generate example")
                    .tests("// Could not generate tests")
                    .build();
        }

        return buildFallbackResponse(issues);
    }

    private String buildPrompt(List<DetectedIssue> issues) {
        StringBuilder sb = new StringBuilder();
        Map<RiskLevel, Long> counts = issues.stream()
                .filter(issue -> issue.getRisk() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        DetectedIssue::getRisk,
                        () -> new java.util.EnumMap<>(RiskLevel.class),
                        java.util.stream.Collectors.counting()));

        sb.append("You are a mobile application security reviewer.\n");
        sb.append("Return only valid JSON. Do not use markdown fences.\n");
        sb.append("Use these exact JSON keys: explanation, remediation, codeExample, tests.\n");
        sb.append("Each value must be a non-empty string.\n\n");
        sb.append("Finding counts: ");
        sb.append("HIGH=").append(counts.getOrDefault(RiskLevel.HIGH, 0L)).append(", ");
        sb.append("MEDIUM=").append(counts.getOrDefault(RiskLevel.MEDIUM, 0L)).append(", ");
        sb.append("LOW=").append(counts.getOrDefault(RiskLevel.LOW, 0L)).append(".\n\n");
        sb.append("Representative findings:\n");

        issues.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DetectedIssue::getRisk, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_FINDINGS_IN_PROMPT)
                .forEach(issue -> sb.append(String.format(
                        "- risk=%s, type=%s, key=%s, source=%s, value=%s%n",
                        safe(issue.getRisk()),
                        safe(issue.getType()),
                        safe(issue.getKey()),
                        safe(issue.getSource()),
                        summarizeValue(issue.getValue()))));

        sb.append("\nWrite concise guidance for an Android developer. ");
        sb.append("For codeExample, show secure Android storage or secret-handling code when relevant. ");
        sb.append("For tests, describe practical verification steps.");

        return sb.toString();
    }

    private AIResponse parseAIResponse(String aiText) {
        AIResponse jsonResponse = parseJsonResponse(aiText);
        if (jsonResponse != null) {
            return jsonResponse;
        }

        String explanation = extractSection(aiText, "[EXPLANATION]", "[REMEDIATION]");
        String remediation = extractSection(aiText, "[REMEDIATION]", "[CODE]");
        String code = extractSection(aiText, "[CODE]", "[TESTS]");
        String tests = extractSection(aiText, "[TESTS]", null);

        return AIResponse.builder()
                .explanation(explanation.trim())
                .remediation(remediation.trim())
                .codeExample(code.trim())
                .tests(tests.trim())
                .build();
    }

    private AIResponse parseJsonResponse(String aiText) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(aiText, AIResponse.class);
        } catch (Exception e) {
            log.debug("AI response was not parseable JSON: {}", e.getMessage());
            return null;
        }
    }

    private AIResponse normalizeResponse(AIResponse response, List<DetectedIssue> issues) {
        AIResponse fallback = buildFallbackResponse(issues);
        return AIResponse.builder()
                .explanation(firstUseful(response.getExplanation(), fallback.getExplanation()))
                .remediation(firstUseful(response.getRemediation(), fallback.getRemediation()))
                .codeExample(firstUseful(response.getCodeExample(), fallback.getCodeExample()))
                .tests(firstUseful(response.getTests(), fallback.getTests()))
                .build();
    }

    private AIResponse buildFallbackResponse(List<DetectedIssue> issues) {
        long high = issues.stream().filter(issue -> issue.getRisk() == RiskLevel.HIGH).count();
        long medium = issues.stream().filter(issue -> issue.getRisk() == RiskLevel.MEDIUM).count();
        long low = issues.stream().filter(issue -> issue.getRisk() == RiskLevel.LOW).count();

        return AIResponse.builder()
                .explanation(String.format(
                        "The scan found %d high, %d medium, and %d low risk finding(s). Review the highest-risk exposed secrets, tokens, insecure storage patterns, and APK configuration issues first.",
                        high, medium, low))
                .remediation("Remove hardcoded secrets from the app, rotate any exposed credentials, use Android Keystore or EncryptedSharedPreferences for sensitive local data, and review network/configuration findings before release.")
                .codeExample("Use AndroidX Security Crypto, EncryptedSharedPreferences, or Android Keystore instead of storing secrets in plain text resources, logs, SharedPreferences, or source code.")
                .tests("Rescan the APK after fixes, verify no secrets appear in decompiled resources or smali, run mobile security tests for local storage and logging, and confirm rotated credentials are no longer accepted.")
                .build();
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return "";
        start += startMarker.length();

        int end = (endMarker != null) ? text.indexOf(endMarker, start) : text.length();
        if (end == -1) end = text.length();

        return text.substring(start, end).trim();
    }

    private String firstUseful(String value, String fallback) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("Not provided by AI.")) {
            return fallback;
        }
        return value.trim();
    }

    private String summarizeValue(String value) {
        String clean = safe(value).replaceAll("\\s+", " ");
        if (clean.length() <= MAX_VALUE_LENGTH) {
            return clean;
        }
        Matcher matcher = Pattern.compile("[A-Za-z0-9_./:@=-]{8,}").matcher(clean);
        if (matcher.find()) {
            clean = matcher.group();
        }
        return clean.length() <= MAX_VALUE_LENGTH ? clean : clean.substring(0, MAX_VALUE_LENGTH) + "...";
    }

    private String safe(Object value) {
        return value == null ? "N/A" : String.valueOf(value);
    }
}
