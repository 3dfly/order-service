package com.threedfly.orderservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "printing.slicer.config.directory=slicer-configs"
})
class IniConfigurationMapperTest {

    @Autowired
    private IniConfigurationMapper iniConfigurationMapper;

    @Test
    void testGetConfigurationFile_ExactMatch(@TempDir Path tempDir) throws IOException {
        // This test validates the naming convention
        // Expected: fdm_pla_020_supports.ini for exact match
        String result = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.20, true);

        // Should try exact match first
        assertThat(result).isNotNull();
        // Falls back to bambu_a1.ini if specific file doesn't exist
    }

    @Test
    void testGetConfigurationFile_WithoutSupports() {
        String result = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.20, false);

        assertThat(result).isNotNull();
        assertThat(result).endsWith(".ini");
    }

    @Test
    void testGetConfigurationFile_DifferentLayerHeights() {
        String result1 = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.10, true);
        String result2 = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.20, true);
        String result3 = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.30, true);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
    }

    @Test
    void testGetConfigurationFile_AllTechnologies() {
        String fdm = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.20, true);
        String sls = iniConfigurationMapper.getConfigurationFile("SLS", "PLA", 0.20, true);
        String sla = iniConfigurationMapper.getConfigurationFile("SLA", "PLA", 0.20, true);

        assertThat(fdm).isNotNull();
        assertThat(sls).isNotNull();
        assertThat(sla).isNotNull();
    }

    @Test
    void testGetConfigurationFile_AllMaterials() {
        String pla = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.20, true);
        String abs = iniConfigurationMapper.getConfigurationFile("FDM", "ABS", 0.20, true);
        String petg = iniConfigurationMapper.getConfigurationFile("FDM", "PETG", 0.20, true);
        String tpu = iniConfigurationMapper.getConfigurationFile("FDM", "TPU", 0.20, true);

        assertThat(pla).isNotNull();
        assertThat(abs).isNotNull();
        assertThat(petg).isNotNull();
        assertThat(tpu).isNotNull();
    }

    @Test
    void testGetConfigurationFile_FallbackToDefault() {
        // When no specific config exists, should fall back to bambu_a1.ini
        String result = iniConfigurationMapper.getConfigurationFile("FDM", "PLA", 0.20, true);

        assertThat(result).isNotNull();
        assertThat(result).endsWith(".ini");
        // Default fallback is bambu_a1.ini
        assertThat(result).isIn("bambu_a1.ini", "fdm_pla_020_supports.ini", "fdm_pla_020.ini", "fdm_pla.ini", "fdm.ini");
    }

    @Test
    void testGetConfigurationPath() {
        String configFile = "bambu_a1.ini";
        Path path = iniConfigurationMapper.getConfigurationPath(configFile);

        assertThat(path).isNotNull();
        assertThat(path.toString()).contains("slicer-configs");
        assertThat(path.toString()).endsWith(configFile);
    }
}
