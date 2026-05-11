package com.ssinspector.backend.analyzer;

import com.ssinspector.backend.model.DetectedIssue;
import com.ssinspector.backend.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes AndroidManifest.xml for common security misconfigurations.
 */
@Component
@lombok.RequiredArgsConstructor
public class ManifestAnalyzer {

    private final com.ssinspector.backend.service.OwaspMapper owaspMapper;

    public List<DetectedIssue> analyze(String manifestContent) {
        List<DetectedIssue> issues = new ArrayList<>();

        // 1. Check for allowBackup="true"
        if (manifestContent.contains("android:allowBackup=\"true\"")) {
            issues.add(applyOwaspMetadata(DetectedIssue.builder()
                    .type("ALLOW_BACKUP_ENABLED")
                    .key("allowBackup")
                    .value("true")
                    .risk(RiskLevel.HIGH)
                    .source("AndroidManifest.xml")
                    .build()));
        }

        // 2. Check for exported="true" (Components exposed)
        if (manifestContent.contains("android:exported=\"true\"")) {
            issues.add(applyOwaspMetadata(DetectedIssue.builder()
                    .type("EXPORTED_COMPONENT_EXPOSED")
                    .key("exported")
                    .value("true")
                    .risk(RiskLevel.MEDIUM)
                    .source("AndroidManifest.xml")
                    .build()));
        }

        // 3. Permission Analysis
        analyzePermissions(manifestContent, issues);

        return issues;
    }

    private void analyzePermissions(String content, List<DetectedIssue> issues) {
        String[] dangerousPermissions = {
            "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE",
            "READ_CONTACTS",
            "ACCESS_FINE_LOCATION"
        };

        for (String permission : dangerousPermissions) {
            if (content.contains(permission)) {
                issues.add(applyOwaspMetadata(DetectedIssue.builder()
                        .type("DANGEROUS_PERMISSION_REQUESTED")
                        .key("permission")
                        .value(permission)
                        .risk(RiskLevel.LOW)
                        .source("AndroidManifest.xml")
                        .build()));
            }
        }
    }

    private DetectedIssue applyOwaspMetadata(DetectedIssue issue) {
        var category = owaspMapper.getCategory(issue.getType());
        if (category != null) {
            issue.setOwaspId(category.getId());
            issue.setOwaspTitle(category.getTitle());
        }
        return issue;
    }
}
