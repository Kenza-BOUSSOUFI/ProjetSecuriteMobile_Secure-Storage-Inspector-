package com.ssinspector.backend.service;

import com.ssinspector.backend.ai.AIResponse;
import com.ssinspector.backend.ai.LlamaService;
import com.ssinspector.backend.classifier.SecurityScoreCalculator;
import com.ssinspector.backend.detector.DataDetector;
import com.ssinspector.backend.model.AnalysisResponse;
import com.ssinspector.backend.model.DetectedIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates the analysis process: Detection -> Scoring -> AI Recommendations -> Response formatting.
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final DataDetector detector;
    private final SecurityScoreCalculator scoreCalculator;
    private final LlamaService llamaService;
    
    // In-memory storage for mock analysis history
    private final java.util.Map<String, AnalysisResponse> analysisCache = new java.util.concurrent.ConcurrentHashMap<>();

    public AnalysisResponse analyze(String content, String source) {
        // 1. Detect issues
        List<DetectedIssue> issues = detector.detect(content, source);
        AnalysisResponse response = buildFinalResponse(issues);
        response.setAnalysisId(source);
        response.setAnalyzedAt(Instant.now().toString());
        
        // Cache the result for report generation (using source as ID for this example)
        analysisCache.put(source, response);
        
        return response;
    }

    public AnalysisResponse getAnalysis(String id) {
        return analysisCache.get(id);
    }

    public void cacheAnalysis(String id, AnalysisResponse response) {
        if (response.getAnalysisId() == null) {
            response.setAnalysisId(id);
        }
        if (response.getAnalyzedAt() == null) {
            response.setAnalyzedAt(Instant.now().toString());
        }
        analysisCache.put(id, response);
    }

    /**
     * Shared logic to calculate score, get AI analysis, and build the response.
     */
    public AnalysisResponse buildFinalResponse(List<DetectedIssue> issues) {
        // 1. Calculate security score
        int score = scoreCalculator.calculateScore(issues);

        // 2. Generate AI Analysis
        AIResponse aiAnalysis = llamaService.generateAnalysis(issues);

        // 3. Build response
        return AnalysisResponse.builder()
                .analyzedAt(Instant.now().toString())
                .score(score)
                .issues(issues)
                .aiAnalysis(aiAnalysis)
                .build();
    }
}
