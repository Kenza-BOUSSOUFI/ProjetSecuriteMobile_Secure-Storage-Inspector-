package com.ssinspector.backend.model;

import com.ssinspector.backend.ai.AIResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The final JSON response format for the analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String analysisId;
    private String analyzedAt;
    private int score;
    private List<DetectedIssue> issues;
    private AIResponse aiAnalysis;
}
