package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Parameter extractor for file formats that require manual parameters (STL, OBJ).
 * These formats contain only geometry data, so all print settings must be provided manually.
 */
@Service
@Slf4j
public class ManualParameterExtractor implements ParameterExtractor {

    @Override
    public PrintCalculationRequest extractParameters(Path filePath, PrintCalculationRequest request) {
        String fileName = filePath.getFileName().toString();
        String fileType = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();

        log.debug("📄 {} file requires manual parameters", fileType);

        if (request == null || !hasRequiredFields(request)) {
            throw new IllegalArgumentException(
                String.format("Request parameters are required for %s files. " +
                    "Required fields: technology, material, layerHeight, shells, infill, supporters. " +
                    "Only 3MF files support automatic parameter extraction.", fileType)
            );
        }

        log.debug("✅ Using provided manual parameters for {} file", fileType);
        return request;
    }

    @Override
    public boolean requiresManualParameters() {
        return true;
    }

    /**
     * Checks if the request has all required fields populated.
     * This is necessary because Spring's @ModelAttribute can create a non-null
     * request object with all null fields when no parameters are provided.
     */
    private boolean hasRequiredFields(PrintCalculationRequest request) {
        return request.getTechnology() != null &&
               request.getMaterial() != null &&
               request.getLayerHeight() != null &&
               request.getShells() != null &&
               request.getInfill() != null &&
               request.getSupporters() != null;
    }
}
