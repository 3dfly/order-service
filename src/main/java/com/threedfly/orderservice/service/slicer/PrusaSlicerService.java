package com.threedfly.orderservice.service.slicer;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.enums.BrimType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PrusaSlicer implementation of the SlicerService.
 * Handles command-line invocation of PrusaSlicer with appropriate parameters.
 */
@Service
@Slf4j
public class PrusaSlicerService implements SlicerService {

    private static final String SLICER_TYPE = "prusa";

    @Value("${printing.prusa.slicer.path}")
    private String slicerPath;

    @Override
    public ProcessBuilder buildSlicerCommand(Path modelFilePath, Path iniPath, Path outputPath,
                                              PrintCalculationRequest request) {
        // Convert paths to absolute strings to prevent injection
        String absoluteIniPath = iniPath.toAbsolutePath().normalize().toString();
        String absoluteOutputPath = outputPath.toAbsolutePath().normalize().toString();
        String absoluteModelPath = modelFilePath.toAbsolutePath().normalize().toString();

        // Build command arguments list
        List<String> command = new ArrayList<>();
        command.add(slicerPath);
        command.add("--load");
        command.add(absoluteIniPath);

        // Layer height
        command.add("--layer-height");
        command.add(String.format("%.2f", request.getLayerHeight()));

        // Perimeters (walls)
        command.add("--perimeters");
        command.add(Integer.toString(request.getShells()));

        // Infill density
        command.add("--fill-density");
        command.add(request.getInfill() + "%");

        // Infill pattern
        if (request.getInfillPattern() != null) {
            command.add("--fill-pattern");
            command.add(request.getInfillPattern().getValue());
        }

        // Top solid layers
        if (request.getTopShellLayers() != null) {
            command.add("--top-solid-layers");
            command.add(Integer.toString(request.getTopShellLayers()));
        }

        // Bottom solid layers
        if (request.getBottomShellLayers() != null) {
            command.add("--bottom-solid-layers");
            command.add(Integer.toString(request.getBottomShellLayers()));
        }

        // Brim settings
        if (request.getBrimType() != null && request.getBrimType() != BrimType.NONE) {
            int brimWidth = (request.getBrimWidth() != null) ? request.getBrimWidth() : 5;
            command.add("--brim-width");
            command.add(Integer.toString(brimWidth));
        } else {
            command.add("--brim-width");
            command.add("0");
        }

        // Support material settings - Always use tree supports (organic style)
        if (request.getSupporters() != null && request.getSupporters()) {
            command.add("--support-material=1");
            command.add("--support-material-auto=1");

            // Always use organic style (tree supports)
            // PrusaSlicer uses --support-material-style for organic (tree) supports
            command.add("--support-material-style");
            command.add("organic");
        } else {
            command.add("--support-material=0");
            command.add("--support-material-auto=0");
        }

        // Seam position
        if (request.getSeam() != null) {
            command.add("--seam-position");
            command.add(request.getSeam().getValue());
        }

        // Output and export settings
        command.add("--output");
        command.add(absoluteOutputPath);
        command.add("--export-gcode");
        command.add("--center");
        command.add("110,110");  // Center model on 220x220mm bed (works for both STL and 3MF)

        // Input model file
        command.add(absoluteModelPath);

        log.debug("🔧 Building PrusaSlicer command with parameters: layerHeight={}, shells={}, infill={}%, " +
                  "topLayers={}, bottomLayers={}, brimWidth={}, supporters={}, infillPattern={}",
                request.getLayerHeight(), request.getShells(), request.getInfill(),
                request.getTopShellLayers(), request.getBottomShellLayers(),
                request.getBrimWidth(), request.getSupporters(),
                request.getInfillPattern());

        return new ProcessBuilder(command);
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
