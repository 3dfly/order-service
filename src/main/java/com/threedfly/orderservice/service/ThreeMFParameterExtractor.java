package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.enums.BrimType;
import com.threedfly.orderservice.enums.InfillPattern;
import com.threedfly.orderservice.enums.SeamPosition;
import com.threedfly.orderservice.exception.FileParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service for extracting print parameters from 3MF files.
 *
 * 3MF files are ZIP archives containing:
 * - 3D/3dmodel.model (geometry)
 * - Metadata/Slic3r_PE.config (PrusaSlicer print settings)
 * - Metadata/Slic3r_PE_model.config (per-model settings)
 *
 * This extractor focuses on PrusaSlicer 3MF format.
 */
@Service
@Slf4j
public class ThreeMFParameterExtractor implements ParameterExtractor {

    private static final long MAX_ZIP_ENTRY_SIZE = 200 * 1024 * 1024; // 200MB per entry
    private static final int MAX_ZIP_ENTRIES = 1000;

    // Default values for missing parameters
    private static final String DEFAULT_TECHNOLOGY = "FDM";
    private static final String DEFAULT_MATERIAL = "PLA";
    private static final Double DEFAULT_LAYER_HEIGHT = 0.2;
    private static final Integer DEFAULT_SHELLS = 2;
    private static final Integer DEFAULT_INFILL = 15;
    private static final Boolean DEFAULT_SUPPORTERS = false;
    private static final Integer DEFAULT_TOP_SHELL_LAYERS = 5;
    private static final Integer DEFAULT_BOTTOM_SHELL_LAYERS = 3;
    private static final InfillPattern DEFAULT_INFILL_PATTERN = InfillPattern.GRID;
    private static final BrimType DEFAULT_BRIM_TYPE = BrimType.AUTO;

    /**
     * Extracts print parameters from a 3MF file.
     * The request parameter is ignored as 3MF files contain embedded parameters.
     *
     * @param filePath Path to the 3MF file to extract from
     * @param request  Ignored for 3MF files (parameters extracted from file)
     * @return PrintCalculationRequest with extracted or default parameters
     * @throws FileParseException if the 3MF file cannot be parsed
     */
    @Override
    public PrintCalculationRequest extractParameters(Path filePath, PrintCalculationRequest request) {
        log.info("🔍 Extracting parameters from 3MF file: {}", filePath.getFileName());

        try {
            Map<String, String> config = extractConfigFromZip(filePath);

            if (config.isEmpty()) {
                log.warn("⚠️ No config found in 3MF file, using all defaults");
            } else {
                log.info("📋 Found {} configuration entries in 3MF", config.size());
            }

            return buildRequestFromConfig(config);

        } catch (IOException e) {
            throw new FileParseException("Failed to parse 3MF file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean requiresManualParameters() {
        return false; // 3MF extracts parameters from file metadata
    }

    /**
     * Extracts configuration entries from the 3MF ZIP archive.
     * Uses ZipFile instead of ZipInputStream to handle modern 3MF files properly.
     */
    private Map<String, String> extractConfigFromZip(Path filePath) throws IOException {
        Map<String, String> config = new HashMap<>();

        // Open the 3MF file as a ZIP archive
        try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int entryCount = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entryCount++;

                // Security check: prevent ZIP bomb attacks
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw new FileParseException("3MF file contains too many entries (max: " + MAX_ZIP_ENTRIES + ")");
                }

                if (entry.getSize() > MAX_ZIP_ENTRY_SIZE) {
                    throw new FileParseException("3MF entry too large: " + entry.getName());
                }

                String entryName = entry.getName();

                // Look for PrusaSlicer config files
                if (entryName.equals("Metadata/Slic3r_PE.config") ||
                    entryName.equals("Metadata/Slic3r_PE_model.config") ||
                    entryName.equals("Metadata/PrusaSlicer.config")) {

                    log.debug("📄 Reading config from: {}", entryName);

                    try (InputStream entryStream = zipFile.getInputStream(entry)) {
                        parseConfigFile(entryStream, config);
                    }
                }
            }
        }

        return config;
    }

    /**
     * Parses an INI-style config file from an input stream.
     * Format: key = value
     */
    private void parseConfigFile(InputStream inputStream, Map<String, String> config) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }

                // Parse key=value pairs
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    config.put(key, value);
                    log.trace("  {} = {}", key, value);
                }
            }
        }
    }

    /**
     * Builds a PrintCalculationRequest from the extracted config,
     * using defaults for any missing parameters.
     */
    private PrintCalculationRequest buildRequestFromConfig(Map<String, String> config) {
        PrintCalculationRequest.PrintCalculationRequestBuilder builder = PrintCalculationRequest.builder();

        // Technology (default to FDM, not typically stored in 3MF)
        String technology = config.getOrDefault("printer_technology", DEFAULT_TECHNOLOGY);
        builder.technology(technology);
        log.debug("  Technology: {}", technology);

        // Material (from filament_type or default)
        String material = extractMaterial(config);
        builder.material(material);
        log.debug("  Material: {}", material);

        // Layer height
        Double layerHeight = extractDouble(config, "layer_height", DEFAULT_LAYER_HEIGHT);
        builder.layerHeight(layerHeight);
        log.debug("  Layer height: {}mm", layerHeight);

        // Shells (perimeters)
        Integer shells = extractInteger(config, "perimeters", DEFAULT_SHELLS);
        builder.shells(shells);
        log.debug("  Shells: {}", shells);

        // Infill percentage
        Integer infill = extractInfillPercentage(config);
        builder.infill(infill);
        log.debug("  Infill: {}%", infill);

        // Top shell layers
        Integer topShellLayers = extractInteger(config, "top_solid_layers", DEFAULT_TOP_SHELL_LAYERS);
        builder.topShellLayers(topShellLayers);
        log.debug("  Top shell layers: {}", topShellLayers);

        // Bottom shell layers
        Integer bottomShellLayers = extractInteger(config, "bottom_solid_layers", DEFAULT_BOTTOM_SHELL_LAYERS);
        builder.bottomShellLayers(bottomShellLayers);
        log.debug("  Bottom shell layers: {}", bottomShellLayers);

        // Infill pattern
        InfillPattern infillPattern = extractInfillPattern(config);
        builder.infillPattern(infillPattern);
        log.debug("  Infill pattern: {}", infillPattern);

        // Support material
        Boolean supporters = extractBoolean(config, "support_material", DEFAULT_SUPPORTERS);
        builder.supporters(supporters);
        log.debug("  Supporters: {}", supporters);

        // Brim
        BrimType brimType = extractBrimType(config);
        builder.brimType(brimType);
        log.debug("  Brim type: {}", brimType);

        Integer brimWidth = extractInteger(config, "brim_width", null);
        if (brimWidth != null) {
            builder.brimWidth(brimWidth);
            log.debug("  Brim width: {}mm", brimWidth);
        }

        // Seam position
        SeamPosition seam = extractSeamPosition(config);
        if (seam != null) {
            builder.seam(seam);
            log.debug("  Seam position: {}", seam);
        }

        // Auto-orient (not in 3MF, default to false since 3MF already has orientation)
        builder.autoOrient(false);

        log.info("✅ Successfully extracted parameters from 3MF");
        return builder.build();
    }

    private String extractMaterial(Map<String, String> config) {
        // Try various material-related keys
        String material = config.get("filament_type");
        if (material == null) {
            material = config.get("material");
        }
        if (material == null) {
            material = DEFAULT_MATERIAL;
        }

        // Normalize material name (PrusaSlicer might use "PLA", "PET", "PETG", etc.)
        material = material.toUpperCase();
        if (material.equals("PET")) {
            material = "PETG";
        }

        return material;
    }

    private Integer extractInfillPercentage(Map<String, String> config) {
        String fillDensity = config.get("fill_density");
        if (fillDensity != null) {
            try {
                // Remove % sign if present
                fillDensity = fillDensity.replace("%", "").trim();
                return Integer.parseInt(fillDensity);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Invalid fill_density value: {}", fillDensity);
            }
        }
        return DEFAULT_INFILL;
    }

    private InfillPattern extractInfillPattern(Map<String, String> config) {
        String pattern = config.get("fill_pattern");
        if (pattern != null) {
            try {
                return InfillPattern.fromValue(pattern);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Unknown infill pattern: {}, using default", pattern);
            }
        }
        return DEFAULT_INFILL_PATTERN;
    }

    private BrimType extractBrimType(Map<String, String> config) {
        String brimType = config.get("brim_type");
        if (brimType != null) {
            try {
                return BrimType.fromValue(brimType);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Unknown brim type: {}, using default", brimType);
            }
        }
        return DEFAULT_BRIM_TYPE;
    }

    private SeamPosition extractSeamPosition(Map<String, String> config) {
        String seam = config.get("seam_position");
        if (seam != null) {
            try {
                return SeamPosition.fromValue(seam);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Unknown seam position: {}, skipping", seam);
            }
        }
        return null; // Optional parameter
    }

    private Double extractDouble(Map<String, String> config, String key, Double defaultValue) {
        String value = config.get(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Invalid double value for {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    private Integer extractInteger(Map<String, String> config, String key, Integer defaultValue) {
        String value = config.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Invalid integer value for {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    private Boolean extractBoolean(Map<String, String> config, String key, Boolean defaultValue) {
        String value = config.get(key);
        if (value != null) {
            // PrusaSlicer uses "1"/"0" for boolean values
            return value.equals("1") || value.equalsIgnoreCase("true");
        }
        return defaultValue;
    }
}
