package com.threedfly.orderservice.service;

import com.threedfly.orderservice.entity.ModelFileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Factory for selecting the appropriate parameter extractor based on file type.
 * Implements the Strategy pattern for handling different 3D model file formats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParameterExtractorFactory {

    private final ThreeMFParameterExtractor threeMFExtractor;
    private final ManualParameterExtractor manualExtractor;

    /**
     * Returns the appropriate parameter extractor for the given file type.
     *
     * @param fileType The type of 3D model file
     * @return ParameterExtractor instance for the file type
     * @throws IllegalArgumentException if file type is not supported
     */
    public ParameterExtractor getExtractor(ModelFileType fileType) {
        log.debug("🏭 Selecting parameter extractor for file type: {}", fileType);

        return switch (fileType) {
            case THREE_MF -> {
                log.debug("✅ Selected ThreeMFParameterExtractor (auto-extraction)");
                yield threeMFExtractor;
            }
            case STL, OBJ -> {
                log.debug("✅ Selected ManualParameterExtractor (manual parameters required)");
                yield manualExtractor;
            }
            default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
        };
    }
}
