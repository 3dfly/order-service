package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ManualParameterExtractorTest {

    private ManualParameterExtractor extractor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        extractor = new ManualParameterExtractor();
    }

    @Test
    void testExtractParameters_WithValidRequest_ReturnsRequest() throws IOException {
        // Given
        Path stlFile = tempDir.resolve("test.stl");
        Files.createFile(stlFile);

        PrintCalculationRequest request = PrintCalculationRequest.builder()
                .technology("FDM")
                .material("PLA")
                .layerHeight(0.2)
                .shells(4)
                .infill(10)
                .supporters(false)
                .build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(stlFile, request);

        // Then
        assertNotNull(result);
        assertSame(request, result, "Should return the same request object");
        assertEquals("FDM", result.getTechnology());
        assertEquals("PLA", result.getMaterial());
    }

    @Test
    void testExtractParameters_WithNullRequest_ThrowsException() throws IOException {
        // Given
        Path stlFile = tempDir.resolve("test.stl");
        Files.createFile(stlFile);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> extractor.extractParameters(stlFile, null)
        );

        assertTrue(exception.getMessage().contains("Request parameters are required"));
        assertTrue(exception.getMessage().contains("STL"));
    }

    @Test
    void testExtractParameters_WithObjFile_ThrowsExceptionWithCorrectFileType() throws IOException {
        // Given
        Path objFile = tempDir.resolve("test.obj");
        Files.createFile(objFile);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> extractor.extractParameters(objFile, null)
        );

        assertTrue(exception.getMessage().contains("OBJ"));
        assertTrue(exception.getMessage().contains("Request parameters are required"));
    }

    @Test
    void testExtractParameters_WithEmptyRequest_PassesThrough() throws IOException {
        // Given
        Path stlFile = tempDir.resolve("test.stl");
        Files.createFile(stlFile);

        // Create a non-null request with all null fields (simulates @ModelAttribute behavior)
        // Controller validation will catch missing required fields, not the extractor
        PrintCalculationRequest emptyRequest = PrintCalculationRequest.builder().build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(stlFile, emptyRequest);

        // Then
        // The extractor should pass through the request as-is
        // Controller validation will handle validation of required fields
        assertNotNull(result);
        assertSame(emptyRequest, result);
    }

    @Test
    void testRequiresManualParameters_ReturnsTrue() {
        // When & Then
        assertTrue(extractor.requiresManualParameters(),
            "Manual extractor should always require manual parameters");
    }

    @Test
    void testExtractParameters_PreservesAllRequestFields() throws IOException {
        // Given
        Path stlFile = tempDir.resolve("test.stl");
        Files.createFile(stlFile);

        PrintCalculationRequest request = PrintCalculationRequest.builder()
                .technology("FDM")
                .material("PETG")
                .layerHeight(0.16)
                .shells(3)
                .infill(15)
                .supporters(true)
                .topShellLayers(6)
                .bottomShellLayers(4)
                .autoOrient(true)
                .build();

        // When
        PrintCalculationRequest result = extractor.extractParameters(stlFile, request);

        // Then
        assertEquals(request.getTechnology(), result.getTechnology());
        assertEquals(request.getMaterial(), result.getMaterial());
        assertEquals(request.getLayerHeight(), result.getLayerHeight());
        assertEquals(request.getShells(), result.getShells());
        assertEquals(request.getInfill(), result.getInfill());
        assertEquals(request.getSupporters(), result.getSupporters());
        assertEquals(request.getTopShellLayers(), result.getTopShellLayers());
        assertEquals(request.getBottomShellLayers(), result.getBottomShellLayers());
        assertEquals(request.getAutoOrient(), result.getAutoOrient());
    }
}
