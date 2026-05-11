package com.ssinspector.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ssinspector.backend.model.AnalysisResponse;
import com.ssinspector.backend.model.DetectedIssue;
import com.ssinspector.backend.model.RiskLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating professional PDF security reports.
 */
@Service
public class PdfReportService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD);
    private static final Font SUB_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);

    public byte[] generateReport(AnalysisResponse analysis, String filename) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Title
            addTitle(document);

            // 2. Analysis Summary
            addSummary(document, filename, analysis.getScore());

            // 3. Risk Overview (Counts)
            addRiskOverview(document, analysis.getIssues());

            document.add(new Paragraph("\n"));

            // 4. Vulnerabilities Table
            addVulnerabilitiesTable(document, analysis.getIssues());

            document.add(new Paragraph("\n"));

            // 5. AI Analysis
            if (analysis.getAiAnalysis() != null) {
                addAiAnalysis(document, analysis);
            }

            // 6. Conclusion
            addConclusion(document);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }

        return out.toByteArray();
    }

    private void addTitle(Document document) throws DocumentException {
        Paragraph title = new Paragraph("Secure Storage Inspector Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        Paragraph subtitle = new Paragraph("Static Application Security Testing (SAST)", NORMAL_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(30);
        document.add(subtitle);
    }

    private void addSummary(Document document, String filename, int score) throws DocumentException {
        document.add(new Paragraph("1. Analysis Summary", HEADER_FONT));
        document.add(new Paragraph("File Name: " + filename, NORMAL_FONT));
        document.add(new Paragraph("Analysis Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), NORMAL_FONT));
        
        Paragraph scorePara = new Paragraph("Security Score: " + score + "/100", BOLD_FONT);
        scorePara.setSpacingAfter(15);
        document.add(scorePara);
    }

    private void addRiskOverview(Document document, List<DetectedIssue> issues) throws DocumentException {
        long high = issues.stream().filter(i -> i.getRisk() == RiskLevel.HIGH).count();
        long medium = issues.stream().filter(i -> i.getRisk() == RiskLevel.MEDIUM).count();
        long low = issues.stream().filter(i -> i.getRisk() == RiskLevel.LOW).count();

        document.add(new Paragraph("2. Risk Overview", HEADER_FONT));
        document.add(new Paragraph("High Risk Findings: " + high, NORMAL_FONT));
        document.add(new Paragraph("Medium Risk Findings: " + medium, NORMAL_FONT));
        document.add(new Paragraph("Low Risk Findings: " + low, NORMAL_FONT));
    }

    private void addVulnerabilitiesTable(Document document, List<DetectedIssue> issues) throws DocumentException {
        document.add(new Paragraph("3. Detected Vulnerabilities", HEADER_FONT));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 15, 30, 25});

        String[] headers = {"Type", "Risk", "Source", "OWASP"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, BOLD_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (DetectedIssue issue : issues) {
            table.addCell(new Phrase(issue.getType(), NORMAL_FONT));
            table.addCell(new Phrase(issue.getRisk().toString(), NORMAL_FONT));
            table.addCell(new Phrase(issue.getSource(), NORMAL_FONT));
            table.addCell(new Phrase(issue.getOwaspId() != null ? issue.getOwaspId() : "N/A", NORMAL_FONT));
        }

        document.add(table);
    }

    private void addAiAnalysis(Document document, AnalysisResponse analysis) throws DocumentException {
        document.add(new Paragraph("4. AI Security Analysis", HEADER_FONT));
        document.add(new Paragraph("\n"));

        var ai = analysis.getAiAnalysis();

        document.add(new Paragraph("Explanation:", SUB_HEADER_FONT));
        document.add(new Paragraph(ai.getExplanation(), NORMAL_FONT));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Remediation Plan:", SUB_HEADER_FONT));
        document.add(new Paragraph(ai.getRemediation(), NORMAL_FONT));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Secure Code Example:", SUB_HEADER_FONT));
        document.add(new Paragraph(ai.getCodeExample(), NORMAL_FONT));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Security Test Cases:", SUB_HEADER_FONT));
        document.add(new Paragraph(ai.getTests(), NORMAL_FONT));
        document.add(new Paragraph("\n"));
    }

    private void addConclusion(Document document) throws DocumentException {
        document.add(new Paragraph("5. Conclusion", HEADER_FONT));
        document.add(new Paragraph("This report was automatically generated by Secure Storage Inspector. " +
                "The findings are based on static analysis and AI-driven heuristics. " +
                "Regular security reviews and manual penetration testing are recommended to supplement these results.", NORMAL_FONT));
    }
}
