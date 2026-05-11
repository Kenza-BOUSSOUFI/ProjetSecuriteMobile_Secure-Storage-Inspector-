package com.ssinspector.backend.scanner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Scans the decompiled APK directory structure to collect files for analysis.
 */
@Slf4j
@Component
public class ApkScanner {

    private static final int MAX_ENTRY_BYTES = 5 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 25 * 1024 * 1024;

    public Path getManifestPath(Path decompiledDir) {
        return decompiledDir.resolve("AndroidManifest.xml");
    }

    /**
     * Collects all text-based files (XML, properties, txt, java, smali) from the directory.
     */
    public List<Path> collectAnalyzeableFiles(Path decompiledDir) {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(decompiledDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String name = path.getFileName().toString();
                     return !path.toString().contains("original") && 
                            !path.toString().contains("lib") &&
                            !name.equals("R.smali") &&
                            !name.startsWith("R$") &&
                            !name.equals("BuildConfig.smali");
                 })
                 .filter(this::isAnalyzeable)
                 .forEach(files::add);
        } catch (IOException e) {
            log.error("Failed to walk through decompiled directory", e);
        }
        return files;
    }

    private boolean isAnalyzeable(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return isAnalyzeableFilename(filename);
    }

    private boolean isAnalyzeableFilename(String filename) {
        return filename.endsWith(".xml") || 
               filename.endsWith(".properties") || 
               filename.endsWith(".txt") || 
               filename.endsWith(".json") ||
               filename.endsWith(".smali") ||
               filename.endsWith(".dex") ||
               filename.endsWith(".arsc") ||
               filename.endsWith(".conf") ||
               filename.endsWith(".ini");
    }

    public String readFileContent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.warn("Could not read file: {}", path);
            return "";
        }
    }

    /**
     * Fallback APK scanner used when apktool is not available. APK files are ZIP
     * archives, so this reads likely text-bearing entries and extracts printable
     * strings from binary entries such as classes.dex.
     */
    public Map<String, String> readAnalyzeableArchiveEntries(File apkFile) throws IOException {
        Map<String, String> contentsByEntry = new LinkedHashMap<>();
        int totalBytesRead = 0;

        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(apkFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && totalBytesRead < MAX_TOTAL_BYTES) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                String lowerName = name.toLowerCase();
                if (!isAnalyzeableFilename(lowerName)) {
                    continue;
                }

                byte[] bytes = readEntryBytes(zip, Math.min(MAX_ENTRY_BYTES, MAX_TOTAL_BYTES - totalBytesRead));
                totalBytesRead += bytes.length;

                String content = looksTextLike(lowerName)
                        ? new String(bytes, StandardCharsets.UTF_8)
                        : extractPrintableStrings(bytes);

                if (!content.isBlank()) {
                    contentsByEntry.put(name, content);
                }
            }
        }

        return contentsByEntry;
    }

    private byte[] readEntryBytes(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;

        while (total < maxBytes && (read = input.read(buffer, 0, Math.min(buffer.length, maxBytes - total))) != -1) {
            out.write(buffer, 0, read);
            total += read;
        }

        return out.toByteArray();
    }

    private boolean looksTextLike(String filename) {
        return filename.endsWith(".xml")
                || filename.endsWith(".properties")
                || filename.endsWith(".txt")
                || filename.endsWith(".json")
                || filename.endsWith(".smali")
                || filename.endsWith(".conf")
                || filename.endsWith(".ini");
    }

    private String extractPrintableStrings(byte[] bytes) {
        StringBuilder output = new StringBuilder();
        StringBuilder current = new StringBuilder();

        for (byte item : bytes) {
            int value = item & 0xff;
            if (value >= 32 && value <= 126) {
                current.append((char) value);
            } else {
                appendIfUseful(output, current);
            }
        }

        appendIfUseful(output, current);
        return output.toString();
    }

    private void appendIfUseful(StringBuilder output, StringBuilder current) {
        if (current.length() >= 6) {
            output.append(current).append('\n');
        }
        current.setLength(0);
    }
}
