package com.ssinspector.backend.classifier; // The user asked for classifier package or separate? 
// Re-reading prompt: classifier, detector, model, service, controller, scanner.
// I'll put it in classifier or just a new package 'calculator' if I want, but I'll stick to the requested structure.
// Wait, the prompt says: scanner, detector, classifier. 
// I'll put the calculator in a separate class, maybe in service or model? 
// The prompt list: controller, service, scanner, detector, classifier, model.
// I'll put SecurityScoreCalculator in its own package if I want, but I'll put it in 'service' or 'detector' if I have to choose.
// Actually, I'll create a 'classifier' package for it or just put it in a logical place.
// I'll put it in its own class for modularity.

import com.ssinspector.backend.model.DetectedIssue;
import com.ssinspector.backend.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calculates the security score based on detected issues.
 * HIGH -> -20, MEDIUM -> -10, LOW -> -2.
 */
@Component
public class SecurityScoreCalculator {

    public int calculateScore(List<DetectedIssue> issues) {
        if (issues == null || issues.isEmpty()) return 100;

        int score = 100;
        
        // Group by type to apply penalty only once per vulnerability type
        java.util.Set<String> detectedTypes = new java.util.HashSet<>();
        int highPenalty = 0;
        int mediumPenalty = 0;
        int lowPenalty = 0;

        for (DetectedIssue issue : issues) {
            if (detectedTypes.add(issue.getType())) {
                switch (issue.getRisk()) {
                    case HIGH -> highPenalty += 20;
                    case MEDIUM -> mediumPenalty += 10;
                    case LOW -> lowPenalty += 5;
                }
            }
        }

        // Apply caps so one risk level doesn't crush the score entirely
        score -= Math.min(60, highPenalty);   // Max 60 points lost for HIGH
        score -= Math.min(30, mediumPenalty); // Max 30 points lost for MEDIUM
        score -= Math.min(10, lowPenalty);    // Max 10 points lost for LOW

        return Math.max(0, score);
    }
}
