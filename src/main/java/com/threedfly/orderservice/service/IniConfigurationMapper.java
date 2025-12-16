package com.threedfly.orderservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class IniConfigurationMapper {

    @Value("${printing.slicer.config.directory:slicer-configs}")
    private String configDirectory;

    /**
     * Maps print parameters to the appropriate INI configuration file.
     *
     * Naming convention: {technology}_{material}_{layerHeight}_{supporters}.ini
     * Example: FDM_PLA_0.2_supports.ini
     *
     * Falls back to base configuration if exact match not found.
     */
    public String getConfigurationFile(String technology, String material, Double layerHeight, Boolean supporters) {
        String technologyLower = technology.toLowerCase();
        String materialLower = material.toLowerCase();
        String layerHeightStr = String.format("%.2f", layerHeight).replace(".", "");
        String supportersStr = supporters ? "supports" : "nosupports";

        // Try exact match first
        String exactMatch = String.format("%s_%s_%s_%s.ini",
            technologyLower, materialLower, layerHeightStr, supportersStr);

        if (configFileExists(exactMatch)) {
            log.info("🎯 Found exact INI match: {}", exactMatch);
            return exactMatch;
        }

        // Try without supporters specification
        String withoutSupports = String.format("%s_%s_%s.ini",
            technologyLower, materialLower, layerHeightStr);

        if (configFileExists(withoutSupports)) {
            log.info("🎯 Found INI match (without supporters): {}", withoutSupports);
            return withoutSupports;
        }

        // Try with just technology and material
        String baseMaterial = String.format("%s_%s.ini",
            technologyLower, materialLower);

        if (configFileExists(baseMaterial)) {
            log.info("🎯 Found base material INI: {}", baseMaterial);
            return baseMaterial;
        }

        // Fall back to technology-only config
        String baseTechnology = String.format("%s.ini", technologyLower);

        if (configFileExists(baseTechnology)) {
            log.info("🎯 Using base technology INI: {}", baseTechnology);
            return baseTechnology;
        }

        // Final fallback - use the default bambu_a1.ini
        log.warn("⚠️ No specific INI found, using default: bambu_a1.ini");
        return "bambu_a1.ini";
    }

    private boolean configFileExists(String filename) {
        Path configPath = Paths.get(configDirectory, filename);
        boolean exists = Files.exists(configPath);
        log.debug("Checking config file: {} - exists: {}", configPath, exists);
        return exists;
    }

    public Path getConfigurationPath(String filename) {
        return Paths.get(configDirectory, filename);
    }
}
