package com.ssinspector.backend.detector;

import com.ssinspector.backend.classifier.RegexClassifier;
import com.ssinspector.backend.model.DetectedIssue;
import com.ssinspector.backend.model.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates detection and assigns risk levels based on the type of data found.
 */
@Component
@RequiredArgsConstructor
public class DataDetector {

    private final RegexClassifier classifier;
    private final HeuristicDetector heuristicDetector;
    private final com.ssinspector.backend.service.OwaspMapper owaspMapper;

    public List<DetectedIssue> detect(String content, String source) {
        List<DetectedIssue> allIssues = new ArrayList<>();

        // 1. Run Regex Detection
        Map<String, List<String>> regexFindings = classifier.classify(content);
        regexFindings.forEach((type, values) -> {
            for (String value : values) {
                allIssues.add(applyOwaspMetadata(DetectedIssue.builder()
                        .type(type)
                        .value(value)
                        .risk(assignRegexRisk(type))
                        .source(source)
                        .build()));
            }
        });

        // 2. Run Heuristic Detection
        List<DetectedIssue> heuristicIssues = heuristicDetector.detect(content, source);
        heuristicIssues.forEach(this::applyOwaspMetadata);
        allIssues.addAll(heuristicIssues);

        // 3. Merge results and avoid duplicates
        return mergeResults(allIssues);
    }

    private DetectedIssue applyOwaspMetadata(DetectedIssue issue) {
        var category = owaspMapper.getCategory(issue.getType());
        if (category != null) {
            issue.setOwaspId(category.getId());
            issue.setOwaspTitle(category.getTitle());
        }
        return issue;
    }

    private List<DetectedIssue> mergeResults(List<DetectedIssue> issues) {
        Map<String, DetectedIssue> uniqueIssues = new java.util.LinkedHashMap<>();

        for (DetectedIssue issue : issues) {
            String value = issue.getValue();
            // If we already have this value, we keep the one with higher risk or the one from Regex
            if (uniqueIssues.containsKey(value)) {
                DetectedIssue existing = uniqueIssues.get(value);
                if (issue.getRisk().ordinal() < existing.getRisk().ordinal()) {
                    // RiskLevel is Enum: HIGH(0), MEDIUM(1), LOW(2)
                    // Wait, ordinal() is 0, 1, 2. So smaller is HIGHER risk.
                    uniqueIssues.put(value, issue);
                }
            } else {
                uniqueIssues.put(value, issue);
            }
        }

        return new ArrayList<>(uniqueIssues.values());
    }

    private RiskLevel assignRegexRisk(String type) {
        return switch (type) {
            case "JWT_TOKEN", "API_KEY" -> RiskLevel.HIGH;
            case "EMAIL" -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }
}
