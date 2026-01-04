package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintCalculationRequest;

import java.nio.file.Path;

/**
 * Strategy interface for extracting print calculation parameters from different file types.
 * Implementations handle specific file formats (3MF, STL, OBJ, etc.)
 */
public interface ParameterExtractor {

    /**
     * Extracts or builds print calculation parameters.
     *
     * @param filePath Path to the saved model file
     * @param request  Optional request with manual parameters (may be null for auto-extracting formats)
     * @return PrintCalculationRequest with effective parameters
     * @throws IllegalArgumentException if required parameters are missing
     */
    PrintCalculationRequest extractParameters(Path filePath, PrintCalculationRequest request);

    /**
     * Whether this extractor requires manual request parameters.
     *
     * @return true if manual parameters are required, false if extracted from file
     */
    boolean requiresManualParameters();
}
