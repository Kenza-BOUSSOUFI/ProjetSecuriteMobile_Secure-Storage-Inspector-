package com.ssinspector.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Executes APKTool commands to decompile Android applications.
 */
@Slf4j
@Component
public class ApkToolExecutor {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "ssinspector_apks";

    /**
     * Decompiles an APK file into a temporary directory.
     *
     * @param apkFile The APK file to decompile
     * @return Path to the output directory
     * @throws IOException If decompression fails
     */
    public Path decompile(File apkFile) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        Path outputDir = Path.of(TEMP_DIR, sessionId);

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        log.info("Decompiling APK: {} to {}", apkFile.getAbsolutePath(), outputDir.toAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(
                "java", "-jar", "C:\\Users\\DELL\\AppData\\Local\\apktool\\apktool.jar",
                "d", apkFile.getAbsolutePath(), "-o", outputDir.toAbsolutePath().toString(), "-f"
        );
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("APKTool failed with exit code: {}", exitCode);
                throw new IOException("APKTool failed to decompile the file. Ensure apktool is installed in the system PATH.");
            }

            log.info("APK decompiled successfully.");
            return outputDir;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Decompilation process was interrupted", e);
        }
    }
}
