package com.threedfly.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for automatically orienting 3D models for optimal printing.
 * Uses Python/trimesh for geometry analysis and orientation.
 */
@Service
@Slf4j
public class ModelOrientationService {

    @Value("${printing.orientation.script.path:scripts/auto_orient_model.py}")
    private String orientationScriptPath;

    @Value("${printing.orientation.enabled:true}")
    private boolean orientationEnabled;

    @Value("${printing.orientation.timeout:60000}")
    private long orientationTimeout;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Automatically orients a 3D model for optimal printing if autoOrient is enabled.
     *
     * @param inputPath   Path to the original model file
     * @param autoOrient  Whether to apply auto-orientation
     * @return Path to the oriented model (same as input if orientation disabled or failed)
     * @throws IOException if orientation fails critically
     */
    public Path orientModelIfNeeded(Path inputPath, Boolean autoOrient) throws IOException {
        // If auto-orient is disabled or not requested, return original path
        if (!orientationEnabled || autoOrient == null || !autoOrient) {
            log.debug("Auto-orientation skipped (enabled={}, requested={})",
                     orientationEnabled, autoOrient);
            return inputPath;
        }

        log.info("🔄 Auto-orienting model: {}", inputPath.getFileName());

        // Generate output path
        String originalFilename = inputPath.getFileName().toString();
        String orientedFilename = originalFilename.replaceFirst(
            "(\\.[^.]+)$", "_oriented$1"
        );
        Path outputPath = inputPath.getParent().resolve(orientedFilename);

        try {
            // Build command to run Python orientation script
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3",
                orientationScriptPath,
                inputPath.toAbsolutePath().toString(),
                outputPath.toAbsolutePath().toString()
            );

            processBuilder.redirectErrorStream(true);
            log.debug("Executing orientation command: {}", String.join(" ", processBuilder.command()));

            // Execute the script
            Process process = processBuilder.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Orientation: {}", line);
                }
            }

            // Wait for completion with timeout
            boolean finished = process.waitFor(orientationTimeout, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("⚠️ Auto-orientation timed out, using original model");
                return inputPath;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0 && Files.exists(outputPath)) {
                log.info("✅ Model auto-oriented successfully");

                // Parse output for statistics if available
                String outputStr = output.toString();
                if (outputStr.contains("Height reduction:")) {
                    log.info("   Orientation stats: {}",
                            outputStr.substring(outputStr.indexOf("Original height:")));
                }

                return outputPath;
            } else {
                log.warn("⚠️ Auto-orientation failed (exit code: {}), using original model", exitCode);
                log.warn("   Output: {}", output);
                return inputPath;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Auto-orientation interrupted, using original model", e);
            return inputPath;
        } catch (IOException e) {
            log.warn("⚠️ Auto-orientation failed: {}, using original model", e.getMessage());
            return inputPath;
        }
    }

    /**
     * Cleans up an oriented model file if it was created.
     *
     * @param modelPath Path to the model file
     */
    public void cleanupOrientedModel(Path modelPath) {
        if (modelPath != null &&
            modelPath.getFileName().toString().contains("_oriented")) {
            try {
                Files.deleteIfExists(modelPath);
                log.debug("🧹 Cleaned up oriented model: {}", modelPath);
            } catch (IOException e) {
                log.warn("⚠️ Could not delete oriented model: {}", modelPath, e);
            }
        }
    }

    /**
     * Checks if the orientation service is properly configured and available.
     *
     * @return true if the service can be used
     */
    public boolean isAvailable() {
        if (!orientationEnabled) {
            return false;
        }

        try {
            // Check if Python 3 is available
            ProcessBuilder pb = new ProcessBuilder("python3", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (!finished || process.exitValue() != 0) {
                log.warn("Python 3 not available, auto-orientation disabled");
                return false;
            }

            // Check if script exists
            Path scriptPath = Path.of(orientationScriptPath);
            if (!Files.exists(scriptPath)) {
                log.warn("Orientation script not found at: {}", scriptPath);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.warn("Auto-orientation service not available: {}", e.getMessage());
            return false;
        }
    }
}
