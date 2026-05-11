package com.ssinspector.backend.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing the AI-generated security analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    private String explanation;
    private String remediation;
    private String codeExample;
    private String tests;
}
