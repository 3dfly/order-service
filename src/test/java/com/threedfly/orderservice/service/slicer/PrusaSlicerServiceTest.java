package com.threedfly.orderservice.service.slicer;

import com.threedfly.orderservice.dto.PrintQuotationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrusaSlicerServiceTest {

    private PrusaSlicerService prusaSlicerService;
    private static final String TEST_SLICER_PATH = "/usr/bin/prusa-slicer";

    @BeforeEach
    void setUp() {
        prusaSlicerService = new PrusaSlicerService();
        ReflectionTestUtils.setField(prusaSlicerService, "slicerPath", TEST_SLICER_PATH);
    }

    @Test
    void testSupports_PrusaType_ReturnsTrue() {
        assertTrue(prusaSlicerService.supports("prusa"));
    }

    @Test
    void testSupports_PrusaTypeCaseInsensitive_ReturnsTrue() {
        assertTrue(prusaSlicerService.supports("PRUSA"));
        assertTrue(prusaSlicerService.supports("Prusa"));
    }

    @Test
    void testSupports_BambuType_ReturnsFalse() {
        assertFalse(prusaSlicerService.supports("bambu"));
    }

    @Test
    void testSupports_UnknownType_ReturnsFalse() {
        assertFalse(prusaSlicerService.supports("unknown"));
    }

    @Test
    void testGetSlicerName_ReturnsPrusa() {
        assertEquals("prusa", prusaSlicerService.getSlicerName());
    }

    @Test
    void testBuildSlicerCommand_WithSupportersEnabled() {
        Path modelPath = Paths.get("/tmp/model.stl");
        Path iniPath = Paths.get("/tmp/config.ini");
        Path outputPath = Paths.get("/tmp/output.gcode");

        PrintQuotationRequest request = PrintQuotationRequest.builder()
                .layerHeight(0.2)
                .shells(3)
                .infill(15)
                .supporters(true)
                .build();

        ProcessBuilder processBuilder = prusaSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath, request
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
        assertTrue(command.contains("--center")); // Center model on bed
        assertTrue(command.contains(modelPath.toAbsolutePath().normalize().toString()));
    }

    @Test
    void testBuildSlicerCommand_WithSupportersDisabled() {
        Path modelPath = Paths.get("/tmp/model.stl");
        Path iniPath = Paths.get("/tmp/config.ini");
        Path outputPath = Paths.get("/tmp/output.gcode");

        PrintQuotationRequest request = PrintQuotationRequest.builder()
                .layerHeight(0.15)
                .shells(2)
                .infill(10)
                .supporters(false)
                .build();

        ProcessBuilder processBuilder = prusaSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath, request
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

        PrintQuotationRequest request = PrintQuotationRequest.builder()
                .layerHeight(0.25)
                .shells(5)
                .infill(20)
                .supporters(true)
                .build();

        ProcessBuilder processBuilder = prusaSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath, request
        );

        List<String> command = processBuilder.command();

        // Verify correct formatting
        assertTrue(command.contains("0.25")); // Layer height formatted as decimal
        assertTrue(command.contains("5")); // Shells as integer
        assertTrue(command.contains("20%")); // Infill with percentage sign
    }

    @Test
    void testBuildSlicerCommand_ContainsCenterFlag() {
        Path modelPath = Paths.get("/tmp/model.stl");
        Path iniPath = Paths.get("/tmp/config.ini");
        Path outputPath = Paths.get("/tmp/output.gcode");

        PrintQuotationRequest request = PrintQuotationRequest.builder()
                .layerHeight(0.2)
                .shells(3)
                .infill(15)
                .supporters(true)
                .build();

        ProcessBuilder processBuilder = prusaSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath, request
        );

        List<String> command = processBuilder.command();

        // Verify model center flag is present
        assertTrue(command.contains("--center"),
                "PrusaSlicer should include --center flag for model positioning");
    }

    @Test
    void testBuildSlicerCommand_PathsAreAbsolute() {
        Path modelPath = Paths.get("relative/model.stl");
        Path iniPath = Paths.get("relative/config.ini");
        Path outputPath = Paths.get("relative/output.gcode");

        PrintQuotationRequest request = PrintQuotationRequest.builder()
                .layerHeight(0.2)
                .shells(3)
                .infill(15)
                .supporters(true)
                .build();

        ProcessBuilder processBuilder = prusaSlicerService.buildSlicerCommand(
                modelPath, iniPath, outputPath, request
        );

        List<String> command = processBuilder.command();

        // Find the model path in command (should be last)
        String modelPathInCommand = command.get(command.size() - 1);
        assertTrue(Paths.get(modelPathInCommand).isAbsolute(),
                "Model path should be converted to absolute path");
    }
}
