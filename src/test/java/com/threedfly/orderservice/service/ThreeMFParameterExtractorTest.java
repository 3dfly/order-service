package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.enums.BrimType;
import com.threedfly.orderservice.enums.InfillPattern;
import com.threedfly.orderservice.exception.FileParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ThreeMFParameterExtractorTest {

    private ThreeMFParameterExtractor extractor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        extractor = new ThreeMFParameterExtractor();
    }

    @Test
    void testRequiresManualParameters_ReturnsFalse() {
        // When & Then
        assertFalse(extractor.requiresManualParameters(),
            "3MF extractor should not require manual parameters");
    }

    @Test
    void testExtractParameters_WithValidConfig_ExtractsParameters() throws IOException {
        // Given
        Path threeMfFile = create3MFFileWithConfig(
            "printer_technology = FDM\n" +
            "filament_type = PETG\n" +
            "layer_height = 0.16\n" +
            "perimeters = 3\n" +
            "fill_density = 20%\n" +
            "support_material = 1\n"
        );

        PrintCalculationRequest dummyRequest = PrintCalculationRequest.builder().build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(threeMfFile, dummyRequest);

        // Then
        assertNotNull(result);
        assertEquals("FDM", result.getTechnology());
        assertEquals("PETG", result.getMaterial());
        assertEquals(0.16, result.getLayerHeight());
        assertEquals(3, result.getShells());
        assertEquals(20, result.getInfill());
        assertTrue(result.getSupporters());
    }

    @Test
    void testExtractParameters_WithEmptyConfig_UsesDefaults() throws IOException {
        // Given
        Path threeMfFile = create3MFFileWithConfig("");
        PrintCalculationRequest dummyRequest = PrintCalculationRequest.builder().build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(threeMfFile, dummyRequest);

        // Then
        assertNotNull(result);
        assertEquals("FDM", result.getTechnology(), "Should use default technology");
        assertEquals("PLA", result.getMaterial(), "Should use default material");
        assertEquals(0.2, result.getLayerHeight(), "Should use default layer height");
        assertEquals(2, result.getShells(), "Should use default shells");
        assertEquals(15, result.getInfill(), "Should use default infill");
        assertFalse(result.getSupporters(), "Should use default supporters");
    }

    @Test
    void testExtractParameters_IgnoresProvidedRequest() throws IOException {
        // Given
        Path threeMfFile = create3MFFileWithConfig(
            "filament_type = ABS\n" +
            "layer_height = 0.24\n"
        );

        // Request with different parameters
        PrintCalculationRequest providedRequest = PrintCalculationRequest.builder()
                .technology("SLS")
                .material("TPU")
                .layerHeight(0.12)
                .shells(5)
                .build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(threeMfFile, providedRequest);

        // Then
        assertNotNull(result);
        // Should use extracted values, not provided ones
        assertEquals("ABS", result.getMaterial(), "Should ignore provided material");
        assertEquals(0.24, result.getLayerHeight(), "Should ignore provided layer height");
        assertEquals(2, result.getShells(), "Should use default from 3MF, not provided shells");
    }

    @Test
    void testExtractParameters_WithInvalidZipFile_ThrowsFileParseException() throws IOException {
        // Given
        Path invalidFile = tempDir.resolve("invalid.3mf");
        Files.write(invalidFile, "not a zip file".getBytes());

        PrintCalculationRequest dummyRequest = PrintCalculationRequest.builder().build();

        // When & Then
        assertThrows(FileParseException.class,
            () -> extractor.extractParameters(invalidFile, dummyRequest));
    }

    @Test
    void testExtractParameters_WithMaterialNormalization_ConvertsPETtoPETG() throws IOException {
        // Given
        Path threeMfFile = create3MFFileWithConfig("filament_type = PET\n");
        PrintCalculationRequest dummyRequest = PrintCalculationRequest.builder().build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(threeMfFile, dummyRequest);

        // Then
        assertEquals("PETG", result.getMaterial(), "PET should be normalized to PETG");
    }

    @Test
    void testExtractParameters_WithOptionalParameters_ExtractsCorrectly() throws IOException {
        // Given
        Path threeMfFile = create3MFFileWithConfig(
            "top_solid_layers = 7\n" +
            "bottom_solid_layers = 4\n" +
            "fill_pattern = gyroid\n" +
            "brim_type = outer_brim_only\n" +
            "brim_width = 8\n" +
            "seam_position = rear\n"
        );

        PrintCalculationRequest dummyRequest = PrintCalculationRequest.builder().build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(threeMfFile, dummyRequest);

        // Then
        assertEquals(7, result.getTopShellLayers());
        assertEquals(4, result.getBottomShellLayers());
        assertEquals(InfillPattern.GYROID, result.getInfillPattern());
        assertEquals(BrimType.OUTER_BRIM_ONLY, result.getBrimType());
        assertEquals(8, result.getBrimWidth());
    }

    // Helper method to create a test 3MF file
    private Path create3MFFileWithConfig(String config) throws IOException {
        Path threeMfFile = tempDir.resolve("test.3mf");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(threeMfFile))) {
            // Add a config entry
            ZipEntry configEntry = new ZipEntry("Metadata/Slic3r_PE.config");
            zos.putNextEntry(configEntry);
            zos.write(config.getBytes());
            zos.closeEntry();

            // Add a dummy model file (required for valid 3MF)
            ZipEntry modelEntry = new ZipEntry("3D/3dmodel.model");
            zos.putNextEntry(modelEntry);
            zos.write("<model/>".getBytes());
            zos.closeEntry();
        }

        return threeMfFile;
    }
}
