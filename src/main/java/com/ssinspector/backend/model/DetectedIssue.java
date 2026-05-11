package com.ssinspector.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single sensitive data finding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedIssue {
    private String key;
    private String type;
    private String value;
    private RiskLevel risk;
    private String source;
    private String owaspId;
    private String owaspTitle;
}
