package com.threedfly.orderservice.service.slicer;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * BambuStudio slicer implementation of the SlicerService.
 * Handles command-line invocation of BambuStudio with appropriate parameters.
 */
@Service
@Slf4j
public class BambuSlicerService implements SlicerService {

    private static final String SLICER_TYPE = "bambu";

    @Value("${printing.bambu.slicer.path}")
    private String slicerPath;

    @Override
    public ProcessBuilder buildSlicerCommand(Path modelFilePath, Path iniPath, Path outputPath,
                                              PrintCalculationRequest request) {
        // Extract individual parameters from request
        Double layerHeight = request.getLayerHeight();
        Integer shells = request.getShells();
        Integer infill = request.getInfill();
        Boolean supporters = request.getSupporters();
        // Convert paths to absolute strings to prevent injection
        String absoluteIniPath = iniPath.toAbsolutePath().normalize().toString();
        String absoluteOutputPath = outputPath.toAbsolutePath().normalize().toString();
        String absoluteModelPath = modelFilePath.toAbsolutePath().normalize().toString();

        // Build safe command arguments - validated inputs are converted directly to strings
        // These values are already validated in PrintQuotationService.validateNumericParameter()
        String layerHeightArg = String.format("%.2f", layerHeight);
        String shellsArg = Integer.toString(shells);
        String infillArg = Integer.toString(infill) + "%";

        // Support material settings - need to control both support_material and support_material_auto
        // to ensure supports are properly enabled/disabled regardless of INI defaults
        String supportMaterialArg = supporters ? "--support-material=1" : "--support-material=0";
        String supportAutoArg = supporters ? "--support-material-auto=1" : "--support-material-auto=0";

        log.debug("🔧 Building BambuStudio command with parameters: layerHeight={}, shells={}, infill={}%, supporters={}",
                layerHeight, shells, infill, supporters);

        // Bambu slicer command with dynamic parameters (uses same CLI options as PrusaSlicer)
        return new ProcessBuilder(
                slicerPath,
                "--load", absoluteIniPath,
                "--layer-height", layerHeightArg,
                "--perimeters", shellsArg,
                "--fill-density", infillArg,
                supportMaterialArg,
                supportAutoArg,
                "--output", absoluteOutputPath,
                "--export-gcode",
                "--center", "110,110",  // Center model on 220x220mm bed (works for both STL and 3MF)
                absoluteModelPath
        );
    }

    @Override
    public boolean supports(String slicerType) {
        return SLICER_TYPE.equalsIgnoreCase(slicerType);
    }

    @Override
    public String getSlicerName() {
        return SLICER_TYPE;
    }
}
