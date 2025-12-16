package com.threedfly.orderservice;

import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Factory class for creating test 3D model files
 */
public class TestFileFactory {

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
