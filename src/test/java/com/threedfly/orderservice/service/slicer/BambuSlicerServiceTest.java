package com.threedfly.orderservice.service.slicer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BambuSlicerServiceTest {

    private BambuSlicerService bambuSlicerService;
    private static final String TEST_SLICER_PATH = "/usr/bin/bambu-studio";

    @BeforeEach
    void setUp() {
        bambuSlicerService = new BambuSlicerService();
        ReflectionTestUtils.setField(bambuSlicerService, "slicerPath", TEST_SLICER_PATH);
    }

    @Test
    void testSupports_BambuType_ReturnsTrue() {
        assertTrue(bambuSlicerService.supports("bambu"));
    }

    @Test
    void testSupports_BambuTypeCaseInsensitive_ReturnsTrue() {
        assertTrue(bambuSlicerService.supports("BAMBU"));
        assertTrue(bambuSlicerService.supports("Bambu"));
    }

    @Test
    void testSupports_PrusaType_ReturnsFalse() {
        assertFalse(bambuSlicerService.supports("prusa"));
    }

    @Test
    void testSupports_UnknownType_ReturnsFalse() {
        assertFalse(bambuSlicerService.supports("unknown"));
    }

    @Test
    void testGetSlicerName_ReturnsBambu() {
        assertEquals("bambu", bambuSlicerService.getSlicerName());
    }

    @Test
    void testBuildSlicerCommand_WithSupportersEnabled() {
        Path modelPath = Paths.get("/tmp/model.stl");
        Path iniPath = Paths.get("/tmp/config.ini");
        Path outputPath = Paths.get("/tmp/output.gcode");

        ProcessBuilder processBuilder = bambuSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath,
                0.2, 3, 15, true
        );

        List<String> command = processBuilder.command();

        // Verify command structure
        assertEquals(TEST_SLICER_PATH, command.get(0));
        assertTrue(command.contains("--load"));
        assertTrue(command.contains("--layer-height"));
        assertTrue(command.contains("0.20"));
        assertTrue(command.contains("--perimeters"));
        assertTrue(command.contains("3"));
        assertTrue(command.contains("--fill-density"));
        assertTrue(command.contains("15%"));
        assertTrue(command.contains("--support-material=1"));
        assertTrue(command.contains("--support-material-auto=1"));
        assertTrue(command.contains("--export-gcode"));
        assertTrue(command.contains(modelPath.toAbsolutePath().normalize().toString()));
    }

    @Test
    void testBuildSlicerCommand_WithSupportersDisabled() {
        Path modelPath = Paths.get("/tmp/model.stl");
        Path iniPath = Paths.get("/tmp/config.ini");
        Path outputPath = Paths.get("/tmp/output.gcode");

        ProcessBuilder processBuilder = bambuSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath,
                0.15, 2, 10, false
        );

        List<String> command = processBuilder.command();

        // Verify supporters are disabled
        assertTrue(command.contains("--support-material=0"));
        assertTrue(command.contains("--support-material-auto=0"));
    }

    @Test
    void testBuildSlicerCommand_ParameterFormatting() {
        Path modelPath = Paths.get("/tmp/model.stl");
        Path iniPath = Paths.get("/tmp/config.ini");
        Path outputPath = Paths.get("/tmp/output.gcode");

        ProcessBuilder processBuilder = bambuSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath,
                0.25, 5, 20, true
        );

        List<String> command = processBuilder.command();

        // Verify correct formatting
        assertTrue(command.contains("0.25")); // Layer height formatted as decimal
        assertTrue(command.contains("5")); // Shells as integer
        assertTrue(command.contains("20%")); // Infill with percentage sign
    }

    @Test
    void testBuildSlicerCommand_DoesNotContainDontArrangeFlag() {
        Path modelPath = Paths.get("/tmp/model.stl");
        Path iniPath = Paths.get("/tmp/config.ini");
        Path outputPath = Paths.get("/tmp/output.gcode");

        ProcessBuilder processBuilder = bambuSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath,
                0.2, 3, 15, true
        );

        List<String> command = processBuilder.command();

        // Verify BambuStudio does NOT include --dont-arrange flag (difference from PrusaSlicer)
        assertFalse(command.contains("--dont-arrange"),
                "BambuStudio should not include --dont-arrange flag");
    }

    @Test
    void testBuildSlicerCommand_PathsAreAbsolute() {
        Path modelPath = Paths.get("relative/model.stl");
        Path iniPath = Paths.get("relative/config.ini");
        Path outputPath = Paths.get("relative/output.gcode");

        ProcessBuilder processBuilder = bambuSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath,
                0.2, 3, 15, true
        );

        List<String> command = processBuilder.command();

        // Find the model path in command (should be last)
        String modelPathInCommand = command.get(command.size() - 1);
        assertTrue(Paths.get(modelPathInCommand).isAbsolute(),
                "Model path should be converted to absolute path");
    }
}
