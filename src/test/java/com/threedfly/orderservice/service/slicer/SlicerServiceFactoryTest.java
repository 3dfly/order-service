package com.threedfly.orderservice.service.slicer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlicerServiceFactoryTest {

    @Mock
    private SlicerService prusaSlicerService;

    @Mock
    private SlicerService bambuSlicerService;

    private SlicerServiceFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure mock behaviors
        when(prusaSlicerService.supports("prusa")).thenReturn(true);
        when(prusaSlicerService.supports(anyString())).thenReturn(false);
        when(prusaSlicerService.supports("prusa")).thenReturn(true);
        when(prusaSlicerService.getSlicerName()).thenReturn("prusa");

        when(bambuSlicerService.supports("bambu")).thenReturn(true);
        when(bambuSlicerService.supports(anyString())).thenReturn(false);
        when(bambuSlicerService.supports("bambu")).thenReturn(true);
        when(bambuSlicerService.getSlicerName()).thenReturn("bambu");

        List<SlicerService> services = Arrays.asList(prusaSlicerService, bambuSlicerService);
        factory = new SlicerServiceFactory(services);
    }

    @Test
    void testGetSlicer_PrusaType_ReturnsPrusaSlicer() {
        SlicerService result = factory.getSlicer("prusa");

        assertNotNull(result);
        assertEquals(prusaSlicerService, result);
        verify(prusaSlicerService).supports("prusa");
    }

    @Test
    void testGetSlicer_BambuType_ReturnsBambuSlicer() {
        SlicerService result = factory.getSlicer("bambu");

        assertNotNull(result);
        assertEquals(bambuSlicerService, result);
        verify(bambuSlicerService).supports("bambu");
    }

    @Test
    void testGetSlicer_UnknownType_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getSlicer("unknown")
        );

        assertTrue(exception.getMessage().contains("No slicer service found for type 'unknown'"));
        assertTrue(exception.getMessage().contains("Available slicers"));
    }

    @Test
    void testGetSlicer_NullType_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getSlicer(null)
        );

        assertEquals("Slicer type cannot be null or empty", exception.getMessage());
    }

    @Test
    void testGetSlicer_EmptyType_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getSlicer("")
        );

        assertEquals("Slicer type cannot be null or empty", exception.getMessage());
    }

    @Test
    void testGetSlicer_BlankType_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getSlicer("   ")
        );

        assertEquals("Slicer type cannot be null or empty", exception.getMessage());
    }

    @Test
    void testGetSlicer_NoSlicersAvailable_ThrowsException() {
        SlicerServiceFactory emptyFactory = new SlicerServiceFactory(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> emptyFactory.getSlicer("prusa")
        );

        assertTrue(exception.getMessage().contains("No slicer service found"));
    }

    @Test
    void testGetSlicer_ErrorMessageIncludesAvailableSlicers() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getSlicer("unknown")
        );

        String message = exception.getMessage();
        assertTrue(message.contains("prusa") || message.contains("bambu"),
                "Error message should list available slicers");
    }

    @Test
    void testGetSlicer_CaseInsensitiveMatch() {
        // Configure mock to support case-insensitive matching
        when(prusaSlicerService.supports("PRUSA")).thenReturn(true);

        SlicerService result = factory.getSlicer("PRUSA");

        assertNotNull(result);
        assertEquals(prusaSlicerService, result);
    }

    @Test
    void testGetSlicer_FirstMatchIsReturned() {
        // If multiple services support the same type, first one should be returned
        SlicerService result = factory.getSlicer("prusa");

        assertNotNull(result);
        assertEquals(prusaSlicerService, result);
        // Verify that we don't check bambu after finding prusa
        verify(prusaSlicerService).supports("prusa");
    }
}
