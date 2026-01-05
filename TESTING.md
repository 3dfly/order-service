# Testing Guide

This project has **two types of integration tests** for different purposes:

## 🚀 Fast Tests (Mock Slicer)

**File**: `PrintCalculationControllerIntegrationTest.java`

**Uses**: Mock slicer script that returns predictable values
**Speed**: ~2 seconds for 20+ test scenarios
**Purpose**: Quick feedback during development, CI/CD pipelines

```bash
# Run fast tests
./gradlew test --tests "*IntegrationTest" -x "*RealSlicerIntegrationTest"

# Or just run all tests (fast by default)
./gradlew test
```

**Pros:**
- ⚡ Very fast (seconds)
- 🔄 Works in any environment (no slicer required)
- 🎯 Deterministic results (always same values)
- ✅ Great for CI/CD

**Cons:**
- ❌ Doesn't test actual slicer integration
- ❌ Mock values may not match real behavior

---

## 🔬 Real Tests (Actual PrusaSlicer)

**File**: `PrintCalculationRealSlicerIntegrationTest.java`

**Uses**: Real PrusaSlicer binary installed on your machine
**Speed**: ~20-40 seconds for 9 test scenarios
**Purpose**: Validate actual slicing behavior before releases

```bash
# Run real slicer integration tests
./gradlew test --tests "*RealSlicerIntegrationTest"

# Or with tag-based filtering
./gradlew test -Dgroups=integration
```

**Requirements:**
- PrusaSlicer must be installed at: `/Applications/PrusaSlicer.app/Contents/MacOS/PrusaSlicer`
- Tests automatically skip if slicer not found

**Pros:**
- ✅ Tests actual end-to-end flow
- ✅ Real material/time calculations
- ✅ Catches slicer integration issues
- ✅ High confidence in production behavior

**Cons:**
- 🐌 Slower (20-40 seconds)
- 💻 Requires PrusaSlicer installation
- 🎲 May vary based on slicer version

---

## Test Scenarios

### Fast Tests (Mock)
- ✅ 20 parameterized tests covering all tech/material combinations
- ✅ Validation tests (missing params, invalid values)
- ✅ File type tests (STL, OBJ, 3MF, empty, invalid)
- ✅ Supporters impact verification

### Real Tests
- ✅ 5 key material scenarios (FDM: PLA, PETG, ABS, ASA, TPU)
- ✅ Supporters flag handling (geometry-dependent)
- ✅ Layer height impact (0.1mm vs 0.3mm)
- ✅ All FDM materials validation
- ✅ OBJ file support

---

## Running Tests

### During Development (Fast Feedback)
```bash
./gradlew test --tests "*IntegrationTest" -x "*RealSlicerIntegrationTest"
```

### Before Commit (All Unit + Fast Integration)
```bash
./gradlew test
```

### Before Release (Include Real Slicer Tests)
```bash
# Run ALL tests including real slicer
./gradlew test --tests "*IntegrationTest"

# Or run ONLY real slicer tests
./gradlew test --tests "*RealSlicerIntegrationTest"
```

### Continuous Integration (CI/CD)
```bash
# Use fast tests only (no slicer installation needed)
./gradlew test
```

---

## Test Output Examples

### Mock Slicer (Fast)
```
MockHttpServletResponse:
   Status = 200
   Body = {
     "materialUsedGrams": 12.34,    # Mock value
     "printingTimeMinutes": 83,      # Mock value
     "estimatedPrice": 1.23
   }
```

### Real Slicer
```
🔧 Testing with REAL slicer: FDM with PLA and supports
   ✅ Material: 0.76g, Time: 5min, Price: $1.04    # Real PrusaSlicer values
```

---

## Important Notes

### Support Material Behavior
Real PrusaSlicer only generates supports when **geometrically needed**. For simple flat geometries (like test cubes), supports may not be generated even when enabled. This is **correct behavior**!

Example:
```
WITHOUT supporters: 0.76g, 5min, $1.04
WITH supporters:    0.76g, 5min, $1.04
ℹ️  Supports not generated (geometry doesn't require them) - This is correct!
```

For complex models with overhangs, supports will add material:
```
WITHOUT supporters: 15.3g, 120min, $5.24
WITH supporters:    23.7g, 185min, $8.91
✅ Supports generated - Material increase: 54.9%
```

### Test File Locations
- **Real 3D models**: `src/test/resources/test-models/` (13 files, 236MB)
  - Model3 - Love.stl (57KB) - Primary test model
  - Model4 - Pineapple.stl (645KB) - Support testing
  - Plus 11 more models (see `test-models/README.md`)
- Mock slicer script: `src/test/resources/mock-slicer/mock-slicer.sh`
- Test file factory: `TestFileFactory.java` (loads from classpath)
- Slicer configs: `slicer-configs/*.ini`

---

## Troubleshooting

### "PrusaSlicer not found" - Tests Skipped
**Solution**: Install PrusaSlicer from https://www.prusa3d.com/page/prusaslicer_424/

### Real Tests Taking Too Long
**Solution**: Use fast tests during development, real tests only before releases

### Different Values from Mock
**Expected**: Real slicer uses actual algorithms, mock returns fixed values

### Tests Fail on CI
**Solution**: CI should use fast (mock) tests only, not real slicer tests

---

## Test Coverage

**Total Tests**: 184
- Unit tests: 166
- Fast integration tests: 9
- Real slicer integration tests: 9

```bash
# Check test count
./gradlew test --info | grep "tests completed"
```

---

## Best Practices

1. **During Development**: Use fast tests for quick iterations
2. **Before Commit**: Run all fast tests to catch regressions
3. **Before Release**: Run real slicer tests for confidence
4. **In CI/CD**: Use fast tests only (no slicer dependency)
5. **Manual QA**: Run both fast + real tests periodically

---

## Adding New Tests

### Adding to Fast Tests
1. Edit `PrintCalculationControllerIntegrationTest.java`
2. Add new test scenario to `validCombinations()` or create new `@Test`
3. Use mock slicer values: 12.34g (no supports), 15.50g (with supports)

### Adding to Real Tests
1. Edit `PrintCalculationRealSlicerIntegrationTest.java`
2. Add to `@Tag("integration")` test class
3. Use range assertions (not exact values)
4. Consider test duration (real slicer is slow)

---

## Summary

| Aspect | Fast Tests | Real Tests |
|--------|-----------|------------|
| **Speed** | ~2 seconds | ~20-40 seconds |
| **Slicer** | Mock script | Real PrusaSlicer |
| **Values** | Fixed (12.34g, 15.50g) | Real calculations |
| **CI/CD** | ✅ Yes | ❌ No (requires install) |
| **Confidence** | Medium | High |
| **Use Case** | Development, CI | Pre-release validation |
