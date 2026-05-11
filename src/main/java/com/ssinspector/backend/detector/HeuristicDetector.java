package com.ssinspector.backend.detector;

import com.ssinspector.backend.model.DetectedIssue;
import com.ssinspector.backend.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects sensitive data using heuristics and context awareness.
 */
@Component
public class HeuristicDetector {

    // Pattern to find key-value pairs (e.g., key=value, key: value, "key": "value")
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
            "([\\w._-]+)\\s*[:=]\\s*[\"']?([^\"'\\s,{}\\[\\]]+)[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for Android Logging (Log.d, Log.e, etc.)
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "Log\\.[vdiwe]\\s*\\(.*?,\\s*[\"']?(.*?)[\"']?\\)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for SharedPreferences insecure usage
    private static final Pattern PREFS_PATTERN = Pattern.compile(
            "\\.putString\\s*\\([\"']?([\\w._-]+)[\"']?,\\s*[\"']?(.*?)[\"']?\\)",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "token", "password", "secret", "api_key", "auth"
    );

    public List<DetectedIssue> detect(String content, String source) {
        List<DetectedIssue> issues = new ArrayList<>();
        
        // 1. Key-Value Detection
        Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(content);
        while (kvMatcher.find()) {
            addIssueIfSensitive(kvMatcher.group(1), kvMatcher.group(2), "HEURISTIC_KV", source, issues);
        }

        // 2. Log Detection
        Matcher logMatcher = LOG_PATTERN.matcher(content);
        while (logMatcher.find()) {
            addIssueIfSensitive("log_content", logMatcher.group(1), "INSECURE_LOGGING", source, issues);
        }

        // 3. SharedPreferences Detection
        Matcher prefsMatcher = PREFS_PATTERN.matcher(content);
        while (prefsMatcher.find()) {
            addIssueIfSensitive(prefsMatcher.group(1), prefsMatcher.group(2), "SHARED_PREFS_STORAGE", source, issues);
        }

        return issues;
    }

    private void addIssueIfSensitive(String key, String value, String type, String source, List<DetectedIssue> issues) {
        RiskLevel risk = calculateRisk(key, value, source);
        if (risk != null) {
            issues.add(DetectedIssue.builder()
                    .key(key)
                    .value(value)
                    .type(type)
                    .risk(risk)
                    .source(source)
                    .build());
        }
    }

    private RiskLevel calculateRisk(String key, String value, String source) {
        RiskLevel risk = null;

        // 1. Sensitive Keywords (HIGH risk)
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (key.toLowerCase().contains(keyword) || value.toLowerCase().contains(keyword)) {
                risk = RiskLevel.HIGH;
                break;
            }
        }

        // 2. Suspicious Length (MEDIUM risk if not already HIGH)
        if (risk == null && value.length() > 20 && value.matches(".*[a-zA-Z].*") && value.matches(".*[0-9].*")) {
            risk = RiskLevel.MEDIUM;
        }

        // 3. Numeric IDs (LOW/MEDIUM risk)
        if (risk == null && value.length() > 6 && value.matches("\\d+")) {
            risk = value.length() > 10 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        }

        // 4. File/Context Awareness (Increase risk)
        if (risk != null && source != null) {
            String sourceLower = source.toLowerCase();
            if (sourceLower.contains("prefs") || sourceLower.contains("config") || sourceLower.contains("user")) {
                risk = upgradeRisk(risk);
            }
        }

        return risk;
    }

    private String determineType(String key, String value) {
        if (key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")) {
            return "HEURISTIC_SECRET";
        }
        if (value.matches("\\d+")) {
            return "SUSPICIOUS_NUMERIC_ID";
        }
        return "SUSPICIOUS_STRING";
    }

    private RiskLevel upgradeRisk(RiskLevel current) {
        return switch (current) {
            case LOW -> RiskLevel.MEDIUM;
            case MEDIUM, HIGH -> RiskLevel.HIGH;
        };
    }
}
