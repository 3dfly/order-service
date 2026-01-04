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

        if (request == null) {
            throw new IllegalArgumentException(
                String.format("Request parameters are required for %s files. " +
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
}
