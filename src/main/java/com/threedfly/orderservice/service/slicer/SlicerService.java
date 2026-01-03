package com.threedfly.orderservice.service.slicer;

import com.threedfly.orderservice.dto.PrintQuotationRequest;
import java.nio.file.Path;

/**
 * Strategy interface for different 3D printer slicer implementations.
 * Each slicer (PrusaSlicer, BambuStudio, etc.) can have its own implementation
 * with specific command-line parameters and behavior.
 */
public interface SlicerService {

    /**
     * Builds a ProcessBuilder configured to execute the slicer with the specified parameters.
     *
     * @param modelFilePath  Path to the 3D model file to slice
     * @param iniPath        Path to the slicer configuration INI file
     * @param outputPath     Path where the output G-code should be saved
     * @param request        Full print quotation request with all parameters
     * @return ProcessBuilder configured with slicer-specific command and arguments
     */
    ProcessBuilder buildSlicerCommand(Path modelFilePath, Path iniPath, Path outputPath,
                                       PrintQuotationRequest request);

    /**
     * Checks if this slicer service supports the given slicer type.
     *
     * @param slicerType The slicer type identifier (e.g., "prusa", "bambu")
     * @return true if this service handles the specified slicer type
     */
    boolean supports(String slicerType);

    /**
     * Returns the name identifier of this slicer.
     *
     * @return Slicer name (e.g., "prusa", "bambu")
     */
    String getSlicerName();
}
