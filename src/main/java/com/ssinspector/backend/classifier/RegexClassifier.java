package com.ssinspector.backend.classifier;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for detecting sensitive patterns using Regular Expressions.
 */
@Component
public class RegexClassifier {

    private final Map<String, Pattern> patterns = new HashMap<>();

    public RegexClassifier() {
        // Simple regex patterns for Step 1
        patterns.put("EMAIL", Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"));
        patterns.put("JWT_TOKEN", Pattern.compile("ey[a-zA-Z0-9_-]+\\.ey[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+"));
        patterns.put("API_KEY", Pattern.compile("(?:api_key|apikey|secret|token)[\"']?\\s*[:=]\\s*[\"']?([a-zA-Z0-9-]{16,})[\"']?", Pattern.CASE_INSENSITIVE));
    }

    public Map<String, List<String>> classify(String content) {
        Map<String, List<String>> findings = new HashMap<>();

        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            String type = entry.getKey();
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(content);

            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                // If it's an API_KEY, we might want to capture the value group if present
                if (type.equals("API_KEY") && matcher.groupCount() > 0) {
                    matches.add(matcher.group(1));
                } else {
                    matches.add(matcher.group());
                }
            }
            
            if (!matches.isEmpty()) {
                findings.put(type, matches);
            }
        }

        return findings;
    }
}
