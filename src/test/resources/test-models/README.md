# Test 3D Models

This directory contains **real 3D model files** used for integration testing.

## Models Overview

| Model | STL Size | 3MF Size | Description | Primary Use |
|-------|----------|----------|-------------|-------------|
| **Model1 - Easy** | - | 61KB | Simple model | 3MF parameter extraction |
| **Model2 - Heracross** | 26MB | 7.9MB | Large Pokemon model | Large file testing |
| **Model3 - Love** | 57KB | 80KB | Heart shape | **Primary test model** (fast) |
| **Model4 - Pineapple** | 645KB | 262KB | Pineapple with overhangs | **Support testing** |
| **Model5 - Baby Turtle** | 15MB | 4.2MB | Detailed turtle | Medium complexity |
| **Model6 - Charizard** | 50MB | 15MB | Large Pokemon | Large file testing |
| **Model7 - Umbreon** | 89MB | 25MB | Very large Pokemon | Stress testing |

## Usage in Tests

### Fast Tests (Mock Slicer)
Uses synthetic simple cubes for speed.

### Real Slicer Integration Tests
Uses actual models for realistic validation:

**Primary Models**:
- **Model3 - Love.stl** (57KB) - Used for most tests due to fast slicing time
- **Model4 - Pineapple.stl** (645KB) - Used for supporter testing (has overhangs)

**Why These Models?**

**Model3 - Love**:
- ✅ Small file size (fast slicing: ~3-5 seconds)
- ✅ Realistic geometry
- ✅ Perfect for parameterized tests (5 materials × 2 supporter options)

**Model4 - Pineapple**:
- ✅ Has overhangs that **actually need supports**
- ✅ Shows real support material impact (+32% material, +53% time)
- ✅ Realistic for validation testing

## Test Results with Real Models

### Model3 - Love (various materials)
```
PLA:  ~3.5g,  ~15min
ABS:  ~3.4g,  ~15min
PETG: ~3.6g,  ~15min
TPU:  ~3.3g,  ~15min
ASA:  ~3.4g,  ~15min
```

### Model4 - Pineapple (supporters impact)
```
WITHOUT supporters: 22.64g, 71min,  $15.33
WITH supporters:    29.84g, 109min, $23.29
Material increase:  +31.8%
Time increase:      +53.5%
Price increase:     +51.9%
```

## Adding New Models

To add new test models:

1. Copy STL/3MF file to this directory
2. Update `TestFileFactory.createRealStlFile("YourModel.stl")`
3. Add test case in integration tests
4. Document in this README

## File Sizes

**Total**: ~236MB (13 files)

**Considerations**:
- Git handles these fine for testing
- For very large repos, consider Git LFS
- CI/CD environments should have sufficient space
- Tests automatically load from classpath

## Notes

- All models are version-controlled as test fixtures
- Models are loaded from classpath at test runtime
- No external file dependencies
- Tests work on any machine that clones the repo
- Models represent real-world use cases
