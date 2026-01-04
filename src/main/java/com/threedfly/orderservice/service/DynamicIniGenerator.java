package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.enums.BrimType;
import com.threedfly.orderservice.enums.InfillPattern;
import com.threedfly.orderservice.enums.SeamPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for generating dynamic INI configuration files based on PrintCalculationRequest.
 * This allows supporting more slicer parameters that may not be available via CLI.
 */
@Service
@Slf4j
public class DynamicIniGenerator {

    @Value("${printing.temp.directory}")
    private String tempDirectory;

    /**
     * Generates a dynamic INI file by loading a base configuration and overriding
     * parameters based on the request.
     *
     * @param baseIniPath Path to the base INI configuration file
     * @param request     The quotation request containing custom parameters
     * @return Path to the generated dynamic INI file
     * @throws IOException if file operations fail
     */
    public Path generateDynamicIni(Path baseIniPath, PrintCalculationRequest request) throws IOException {
        log.info("🔧 Generating dynamic INI from base: {}", baseIniPath);

        // Read base INI file
        Map<String, String> iniConfig = loadBaseIni(baseIniPath);

        // Override with request parameters
        applyRequestParameters(iniConfig, request);

        // Write to temporary file
        Path dynamicIniPath = writeDynamicIni(iniConfig);

        log.info("✅ Generated dynamic INI: {}", dynamicIniPath);
        return dynamicIniPath;
    }

    /**
     * Loads the base INI file into a map preserving order.
     */
    private Map<String, String> loadBaseIni(Path baseIniPath) throws IOException {
        Map<String, String> config = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(baseIniPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Store comments and empty lines with generated keys to preserve them
                if (line.trim().isEmpty()) {
                    config.put("_empty_" + config.size(), "");
                } else if (line.trim().startsWith("#")) {
                    config.put("_comment_" + config.size(), line);
                } else if (line.contains("=")) {
                    // Parse key-value pairs
                    int equalsIndex = line.indexOf("=");
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    config.put(key, value);
                }
            }
        }

        log.debug("📄 Loaded {} configuration entries from base INI", config.size());
        return config;
    }

    /**
     * Applies parameters from PrintCalculationRequest to the INI configuration.
     */
    private void applyRequestParameters(Map<String, String> config, PrintCalculationRequest request) {
        // Layer height
        if (request.getLayerHeight() != null) {
            config.put("layer_height", String.valueOf(request.getLayerHeight()));
        }

        // Shells/Perimeters
        if (request.getShells() != null) {
            config.put("perimeters", String.valueOf(request.getShells()));
        }

        // Infill percentage
        if (request.getInfill() != null) {
            config.put("fill_density", request.getInfill() + "%");
        }

        // Top and bottom shell layers
        if (request.getTopShellLayers() != null) {
            config.put("top_solid_layers", String.valueOf(request.getTopShellLayers()));
        }
        if (request.getBottomShellLayers() != null) {
            config.put("bottom_solid_layers", String.valueOf(request.getBottomShellLayers()));
        }

        // Infill pattern
        if (request.getInfillPattern() != null) {
            config.put("fill_pattern", mapInfillPattern(request.getInfillPattern()));
        }

        // Brim settings
        if (request.getBrimType() != null) {
            config.put("brim_type", mapBrimType(request.getBrimType()));
        }
        if (request.getBrimWidth() != null) {
            config.put("brim_width", String.valueOf(request.getBrimWidth()));
        }

        // Support settings - Always use tree supports when enabled
        if (request.getSupporters() != null) {
            config.put("support_material", request.getSupporters() ? "1" : "0");
            config.put("support_material_auto", request.getSupporters() ? "1" : "0");

            // Apply tree support when supporters are enabled
            if (request.getSupporters()) {
                config.put("support_material_pattern", "tree");
            }
        }

        // Seam position
        if (request.getSeam() != null) {
            config.put("seam_position", mapSeamPosition(request.getSeam()));
        }

        // Color change (for 3MF files)
        if (request.getColorChange() != null && !request.getColorChange().isBlank()) {
            // Color change format: comma-separated layer numbers
            config.put("color_change_gcode", formatColorChangeGcode(request.getColorChange()));
        }

        log.debug("✅ Applied request parameters to INI configuration");
    }

    /**
     * Maps InfillPattern enum to slicer INI value.
     */
    private String mapInfillPattern(InfillPattern pattern) {
        return pattern.getValue();
    }

    /**
     * Maps BrimType enum to slicer INI value.
     */
    private String mapBrimType(BrimType brimType) {
        return brimType.getValue();
    }

    /**
     * Maps SeamPosition enum to slicer INI value.
     */
    private String mapSeamPosition(SeamPosition seam) {
        return seam.getValue();
    }

    /**
     * Formats color change layers into G-code format.
     * Input: "10,20,30" (comma-separated layer numbers)
     * Output: G-code commands for color changes
     */
    private String formatColorChangeGcode(String colorChangeLayers) {
        // Simple format for now - just store the layer numbers
        // Slicers will handle the actual G-code generation
        return colorChangeLayers.trim();
    }

    /**
     * Writes the dynamic INI configuration to a temporary file.
     */
    private Path writeDynamicIni(Map<String, String> config) throws IOException {
        // Create temp directory if it doesn't exist
        Path tempDir = Paths.get(tempDirectory);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        // Generate unique filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = "dynamic_config_" + timestamp + ".ini";
        Path iniPath = tempDir.resolve(filename);

        // Write INI file
        try (BufferedWriter writer = Files.newBufferedWriter(iniPath)) {
            writer.write("# Dynamic INI configuration generated by 3DFly Order Service\n");
            writer.write("# Generated at: " + LocalDateTime.now() + "\n\n");

            for (Map.Entry<String, String> entry : config.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("_empty_")) {
                    writer.write("\n");
                } else if (key.startsWith("_comment_")) {
                    writer.write(value + "\n");
                } else {
                    writer.write(key + " = " + value + "\n");
                }
            }
        }

        log.debug("💾 Wrote dynamic INI file: {}", iniPath);
        return iniPath;
    }

    /**
     * Cleans up a dynamically generated INI file.
     */
    public void cleanupDynamicIni(Path iniPath) {
        try {
            if (iniPath != null && Files.exists(iniPath) &&
                    iniPath.getFileName().toString().startsWith("dynamic_config_")) {
                Files.deleteIfExists(iniPath);
                log.debug("🧹 Cleaned up dynamic INI: {}", iniPath);
            }
        } catch (IOException e) {
            log.warn("⚠️ Could not delete dynamic INI file: {}", iniPath, e);
        }
    }
}
