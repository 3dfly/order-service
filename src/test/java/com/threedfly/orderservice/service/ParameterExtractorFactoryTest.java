package com.threedfly.orderservice.service;

import com.threedfly.orderservice.entity.ModelFileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ParameterExtractorFactoryTest {

    @Mock
    private ThreeMFParameterExtractor threeMFExtractor;

    @Mock
    private ManualParameterExtractor manualExtractor;

    private ParameterExtractorFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ParameterExtractorFactory(threeMFExtractor, manualExtractor);
    }

    @Test
    void testGetExtractor_For3MF_ReturnsThreeMFExtractor() {
        // When
        ParameterExtractor result = factory.getExtractor(ModelFileType.THREE_MF);

        // Then
        assertNotNull(result);
        assertSame(threeMFExtractor, result, "Should return the 3MF extractor");
    }

    @Test
    void testGetExtractor_ForSTL_ReturnsManualExtractor() {
        // When
        ParameterExtractor result = factory.getExtractor(ModelFileType.STL);

        // Then
        assertNotNull(result);
        assertSame(manualExtractor, result, "Should return the manual extractor for STL");
    }

    @Test
    void testGetExtractor_ForOBJ_ReturnsManualExtractor() {
        // When
        ParameterExtractor result = factory.getExtractor(ModelFileType.OBJ);

        // Then
        assertNotNull(result);
        assertSame(manualExtractor, result, "Should return the manual extractor for OBJ");
    }

    @Test
    void testGetExtractor_MultipleCalls_ReturnsSameInstance() {
        // When
        ParameterExtractor result1 = factory.getExtractor(ModelFileType.THREE_MF);
        ParameterExtractor result2 = factory.getExtractor(ModelFileType.THREE_MF);

        // Then
        assertSame(result1, result2, "Should return the same instance on multiple calls");
    }

    @Test
    void testGetExtractor_DifferentFileTypes_ReturnsDifferentExtractors() {
        // When
        ParameterExtractor threeMfResult = factory.getExtractor(ModelFileType.THREE_MF);
        ParameterExtractor stlResult = factory.getExtractor(ModelFileType.STL);

        // Then
        assertNotSame(threeMfResult, stlResult, "Different file types should return different extractors");
    }

    @Test
    void testGetExtractor_STLAndOBJ_ReturnSameExtractorInstance() {
        // When
        ParameterExtractor stlResult = factory.getExtractor(ModelFileType.STL);
        ParameterExtractor objResult = factory.getExtractor(ModelFileType.OBJ);

        // Then
        assertSame(stlResult, objResult, "STL and OBJ should share the same manual extractor");
    }
}
