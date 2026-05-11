package com.ssinspector.backend.controller;

import com.ssinspector.backend.model.AnalysisResponse;
import com.ssinspector.backend.service.AnalysisService;
import com.ssinspector.backend.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for exporting security reports in PDF format.
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdfReportService;
    private final AnalysisService analysisService;

    /**
     * Exports a security report for a given analysis ID (usually the filename).
     */
    @GetMapping("/{analysisId}")
    public ResponseEntity<byte[]> exportReport(@PathVariable String analysisId) {
        AnalysisResponse analysis = analysisService.getAnalysis(analysisId);

        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfContent = pdfReportService.generateReport(analysis, analysisId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SecurityReport_" + analysisId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
}
