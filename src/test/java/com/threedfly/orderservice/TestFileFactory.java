package com.threedfly.orderservice;

import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Factory class for creating test 3D model files.
 * Uses REAL STL/3MF files from src/test/resources/test-models/ for realistic testing.
 */
public class TestFileFactory {

    private static final String TEST_MODELS_PATH = "test-models";

    /**
     * Creates a test file from a REAL STL file stored in test resources.
     * Falls back to synthetic cube if real file not found.
     */
    public static MockMultipartFile createRealStlFile(String modelName) {
        try {
            // Load from classpath resources
            byte[] fileContent = TestFileFactory.class.getClassLoader()
                    .getResourceAsStream(TEST_MODELS_PATH + "/" + modelName)
                    .readAllBytes();

            System.out.println("✅ Using REAL file from resources: " + modelName + " (" + fileContent.length + " bytes)");
            return new MockMultipartFile(
                    "file",
                    modelName,
                    "application/octet-stream",
                    fileContent
            );
        } catch (IOException | NullPointerException e) {
            System.out.println("⚠️  Real file not found in resources: " + modelName + ", using synthetic cube");
            return createTestStlFile();
        }
    }

    /**
     * Creates a test file from a REAL 3MF file stored in test resources.
     */
    public static MockMultipartFile createReal3MFFile(String modelName) {
        try {
            // Load from classpath resources
            byte[] fileContent = TestFileFactory.class.getClassLoader()
                    .getResourceAsStream(TEST_MODELS_PATH + "/" + modelName)
                    .readAllBytes();

            System.out.println("✅ Using REAL 3MF file from resources: " + modelName + " (" + fileContent.length + " bytes)");
            return new MockMultipartFile(
                    "file",
                    modelName,
                    "application/octet-stream",
                    fileContent
            );
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("3MF file not found in resources: " + modelName, e);
        }
    }

    /**
     * Creates a simple ASCII STL file representing a cube
     */
    public static MockMultipartFile createTestStlFile() {
        String stlContent = """
                solid test_cube
                  facet normal 0.0 0.0 1.0
                    outer loop
                      vertex 0.0 0.0 10.0
                      vertex 10.0 0.0 10.0
                      vertex 10.0 10.0 10.0
                    endloop
                  endfacet
                  facet normal 0.0 0.0 1.0
                    outer loop
                      vertex 0.0 0.0 10.0
                      vertex 10.0 10.0 10.0
                      vertex 0.0 10.0 10.0
                    endloop
                  endfacet
                  facet normal 0.0 0.0 -1.0
                    outer loop
                      vertex 0.0 0.0 0.0
                      vertex 10.0 10.0 0.0
                      vertex 10.0 0.0 0.0
                    endloop
                  endfacet
                  facet normal 0.0 0.0 -1.0
                    outer loop
                      vertex 0.0 0.0 0.0
                      vertex 0.0 10.0 0.0
                      vertex 10.0 10.0 0.0
                    endloop
                  endfacet
                  facet normal 1.0 0.0 0.0
                    outer loop
                      vertex 10.0 0.0 0.0
                      vertex 10.0 10.0 0.0
                      vertex 10.0 10.0 10.0
                    endloop
                  endfacet
                  facet normal 1.0 0.0 0.0
                    outer loop
                      vertex 10.0 0.0 0.0
                      vertex 10.0 10.0 10.0
                      vertex 10.0 0.0 10.0
                    endloop
                  endfacet
                  facet normal -1.0 0.0 0.0
                    outer loop
                      vertex 0.0 0.0 0.0
                      vertex 0.0 10.0 10.0
                      vertex 0.0 10.0 0.0
                    endloop
                  endfacet
                  facet normal -1.0 0.0 0.0
                    outer loop
                      vertex 0.0 0.0 0.0
                      vertex 0.0 0.0 10.0
                      vertex 0.0 10.0 10.0
                    endloop
                  endfacet
                  facet normal 0.0 1.0 0.0
                    outer loop
                      vertex 0.0 10.0 0.0
                      vertex 0.0 10.0 10.0
                      vertex 10.0 10.0 10.0
                    endloop
                  endfacet
                  facet normal 0.0 1.0 0.0
                    outer loop
                      vertex 0.0 10.0 0.0
                      vertex 10.0 10.0 10.0
                      vertex 10.0 10.0 0.0
                    endloop
                  endfacet
                  facet normal 0.0 -1.0 0.0
                    outer loop
                      vertex 0.0 0.0 0.0
                      vertex 10.0 0.0 10.0
                      vertex 0.0 0.0 10.0
                    endloop
                  endfacet
                  facet normal 0.0 -1.0 0.0
                    outer loop
                      vertex 0.0 0.0 0.0
                      vertex 10.0 0.0 0.0
                      vertex 10.0 0.0 10.0
                    endloop
                  endfacet
                endsolid test_cube
                """;

        return new MockMultipartFile(
                "file",
                "test_cube.stl",
                "application/octet-stream",
                stlContent.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Creates a simple OBJ file representing a cube
     */
    public static MockMultipartFile createTestObjFile() {
        String objContent = """
                # Simple cube
                v 0.0 0.0 0.0
                v 10.0 0.0 0.0
                v 10.0 10.0 0.0
                v 0.0 10.0 0.0
                v 0.0 0.0 10.0
                v 10.0 0.0 10.0
                v 10.0 10.0 10.0
                v 0.0 10.0 10.0

                f 1 2 3
                f 1 3 4
                f 5 6 7
                f 5 7 8
                f 1 2 6
                f 1 6 5
                f 3 4 8
                f 3 8 7
                f 2 3 7
                f 2 7 6
                f 1 4 8
                f 1 8 5
                """;

        return new MockMultipartFile(
                "file",
                "test_cube.obj",
                "application/octet-stream",
                objContent.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Creates an empty/invalid file for testing
     */
    public static MockMultipartFile createEmptyFile() {
        return new MockMultipartFile(
                "file",
                "empty.stl",
                "application/octet-stream",
                new byte[0]
        );
    }

    /**
     * Creates an invalid file type
     */
    public static MockMultipartFile createInvalidFileType() {
        return new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is not a 3D model file".getBytes(StandardCharsets.UTF_8)
        );
    }
}
