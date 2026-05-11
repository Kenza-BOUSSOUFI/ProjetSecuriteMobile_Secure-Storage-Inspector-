package com.ssinspector.backend.service;

import com.ssinspector.backend.analyzer.ManifestAnalyzer;
import com.ssinspector.backend.detector.DataDetector;
import com.ssinspector.backend.model.AnalysisResponse;
import com.ssinspector.backend.model.DetectedIssue;
import com.ssinspector.backend.scanner.ApkScanner;
import com.ssinspector.backend.util.ApkToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service that orchestrates the APK analysis process.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApkAnalysisService {

    private final ApkToolExecutor apkToolExecutor;
    private final ApkScanner apkScanner;
    private final ManifestAnalyzer manifestAnalyzer;
    private final DataDetector detector;
    private final AnalysisService analysisService;

    /**
     * Performs a full static analysis on an uploaded APK file.
     */
    public AnalysisResponse analyzeApk(MultipartFile file) throws IOException {
        // 1. Save MultipartFile to temp file
        File tempApk = File.createTempFile("upload_", ".apk");
        file.transferTo(tempApk);

        try {
            List<DetectedIssue> allIssues = new ArrayList<>();
            String originalFilename = file.getOriginalFilename();

            try {
                analyzeWithApkTool(tempApk, allIssues);
            } catch (IOException e) {
                log.warn("APKTool analysis unavailable for {}. Falling back to archive scan: {}",
                        originalFilename, e.getMessage());
                analyzeAsArchive(tempApk, allIssues);
            }

            // 5. Use existing analysis service logic for scoring and AI analysis
            AnalysisResponse response = analysisService.buildFinalResponse(allIssues);
            
            // Cache the result for report generation
            String analysisId = originalFilename != null && !originalFilename.isBlank() ? originalFilename : tempApk.getName();
            response.setAnalysisId(analysisId);
            analysisService.cacheAnalysis(analysisId, response);
            
            return response;

        } finally {
            // Clean up temp APK
            if (tempApk.exists()) {
                tempApk.delete();
            }
        }
    }

    private void analyzeWithApkTool(File tempApk, List<DetectedIssue> allIssues) throws IOException {
        Path decompiledDir = apkToolExecutor.decompile(tempApk);

        Path manifestPath = apkScanner.getManifestPath(decompiledDir);
        if (manifestPath.toFile().exists()) {
            String manifestContent = apkScanner.readFileContent(manifestPath);
            allIssues.addAll(manifestAnalyzer.analyze(manifestContent));
        }

        List<Path> filesToScan = apkScanner.collectAnalyzeableFiles(decompiledDir);
        log.info("Starting analysis of {} files...", filesToScan.size());
        
        int count = 0;
        for (Path path : filesToScan) {
            String content = apkScanner.readFileContent(path);
            String relativePath = decompiledDir.relativize(path).toString();
            allIssues.addAll(detector.detect(content, relativePath));
            count++;
            if (count % 100 == 0) {
                log.info("Analyzed {}/{} files...", count, filesToScan.size());
            }
        }
        log.info("File analysis complete. Found {} raw issues.", allIssues.size());
    }

    private void analyzeAsArchive(File tempApk, List<DetectedIssue> allIssues) throws IOException {
        Map<String, String> archiveEntries = apkScanner.readAnalyzeableArchiveEntries(tempApk);

        for (Map.Entry<String, String> entry : archiveEntries.entrySet()) {
            String source = "apk://" + entry.getKey();
            if (entry.getKey().equalsIgnoreCase("AndroidManifest.xml")) {
                allIssues.addAll(manifestAnalyzer.analyze(entry.getValue()));
            }
            allIssues.addAll(detector.detect(entry.getValue(), source));
        }
    }
}
