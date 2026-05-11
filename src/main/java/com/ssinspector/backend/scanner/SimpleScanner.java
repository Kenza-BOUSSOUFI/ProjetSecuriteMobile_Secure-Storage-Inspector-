package com.ssinspector.backend.scanner;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Handles reading content from different inputs (Text or Files).
 */
@Component
public class SimpleScanner {

    public String scanText(String text) {
        return text != null ? text : "";
    }

    public String scanFile(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file content", e);
        }
    }
}
