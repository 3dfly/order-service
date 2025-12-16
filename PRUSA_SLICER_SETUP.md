# Setting Up PrusaSlicer Locally

This guide shows you how to install and configure PrusaSlicer to work with the 3D Print Quotation API.

## Step 1: Install PrusaSlicer

### Download and Install

1. Go to: https://www.prusa3d.com/page/prusaslicer_424/
2. Download the macOS version
3. Install to `/Applications/PrusaSlicer.app`

### Verify Installation

```bash
ls -la /Applications/PrusaSlicer.app/Contents/MacOS/prusa-slicer
```

You should see the executable file.

## Step 2: Configure the Application

Update `src/main/resources/application.properties`:

```properties
# Comment out the mock slicer
# printing.bambu.slicer.path=/Users/sefica/Downloads/order-service/src/test/resources/mock-slicer/mock-slicer.sh

# Use real PrusaSlicer
printing.bambu.slicer.path=/Applications/PrusaSlicer.app/Contents/MacOS/prusa-slicer
```

## Step 3: Understanding INI Configuration Files

The API automatically selects INI files based on your parameters:

### Naming Convention

```
{technology}_{material}_{layerHeight}_{supporters}.ini
```

Examples:
- `fdm_pla_020_supports.ini` - FDM, PLA, 0.20mm layer height, with supports
- `fdm_pla_020.ini` - FDM, PLA, 0.20mm layer height, no supports
- `sls_abs_015.ini` - SLS, ABS, 0.15mm layer height
- `bambu_a1.ini` - Default fallback

### Fallback Order

When looking for a config file, the API tries:
1. Exact match: `fdm_pla_020_supports.ini`
2. Without supporters: `fdm_pla_020.ini`
3. Base material: `fdm_pla.ini`
4. Technology only: `fdm.ini`
5. Default: `bambu_a1.ini`

### Your Current Config Files

You already have:
- `bambu_a1.ini` - Default configuration for Bambu Lab A1 printer
- `bambu_a1_mini.ini` - Configuration for Bambu Lab A1 Mini

## Step 4: Create Custom INI Files (Optional)

### Option A: Export from PrusaSlicer GUI

1. Open PrusaSlicer
2. Configure your printer, filament, and print settings
3. Go to: `File → Export → Export Config...`
4. Save to: `order-service/slicer-configs/`
5. Rename following the naming convention above

### Option B: Use Existing INI as Template

Copy and modify the existing `bambu_a1.ini`:

```bash
cd slicer-configs

# Create FDM + PLA + 0.2mm with supports
cp bambu_a1.ini fdm_pla_020_supports.ini

# Create FDM + PLA + 0.2mm without supports
cp bambu_a1.ini fdm_pla_020.ini

# Edit the files to customize settings
```

### Key Settings to Modify

```ini
# Layer height (0.1, 0.15, 0.2, 0.3, 0.4)
layer_height = 0.2
first_layer_height = 0.2

# Infill percentage
fill_density = 15%

# Perimeter shells (walls)
perimeters = 2

# Support material
support_material = 0        # 0 = disabled, 1 = enabled
support_material_auto = 1   # Auto-generate supports

# Material-specific temperatures
# PLA
temperature = 215
bed_temperature = 60

# ABS
temperature = 245
bed_temperature = 100

# PETG
temperature = 240
bed_temperature = 80

# TPU
temperature = 230
bed_temperature = 50
```

## Step 5: Test with Real Slicer

### Restart the Application

```bash
# Stop current instance (Ctrl+C)
./gradlew bootRun
```

### Test the API

```bash
./test-single.sh
```

### Expected Output with Real Slicer

The response will contain actual calculated values from PrusaSlicer:

```json
{
  "fileName": "test_cube.stl",
  "technology": "FDM",
  "material": "PLA",
  "layerHeight": 0.2,
  "shells": 2,
  "infill": 15,
  "supporters": true,
  "estimatedPrice": 3.45,
  "currency": "USD",
  "materialUsedGrams": 18.7,
  "printingTimeMinutes": 127,
  "pricePerGram": 0.05,
  "pricePerMinute": 0.10,
  "materialCost": 0.94,
  "timeCost": 12.70
}
```

## Step 6: Create Comprehensive Config Set (Optional)

For production use, create configs for all your supported combinations:

```bash
cd slicer-configs

# FDM combinations
cp bambu_a1.ini fdm_pla_020_supports.ini
cp bambu_a1.ini fdm_pla_020.ini
cp bambu_a1.ini fdm_abs_020_supports.ini
cp bambu_a1.ini fdm_abs_020.ini
cp bambu_a1.ini fdm_petg_020_supports.ini
cp bambu_a1.ini fdm_petg_020.ini
cp bambu_a1.ini fdm_tpu_030_supports.ini
cp bambu_a1.ini fdm_tpu_030.ini

# SLS combinations (if you have SLS printer)
cp bambu_a1.ini sls_pla_020.ini
cp bambu_a1.ini sls_abs_020.ini
cp bambu_a1.ini sls_petg_020.ini

# SLA combinations (if you have SLA printer)
cp bambu_a1.ini sla_pla_010.ini
cp bambu_a1.ini sla_abs_010.ini
```

Then edit each file to customize:
- Material temperatures
- Print speeds
- Support settings
- Printer-specific settings

## Troubleshooting

### Error: "Cannot run program prusa-slicer"

**Solution**: Verify the path in `application.properties` matches your installation:

```bash
# Find PrusaSlicer
find /Applications -name "prusa-slicer" 2>/dev/null

# Update application.properties with the correct path
```

### Error: "Could not find configuration file"

**Solution**: Ensure your INI files are in the `slicer-configs/` directory:

```bash
ls -la slicer-configs/
```

### PrusaSlicer Returns Error

**Check the logs** for the slicer command being executed:

```
🔧 Executing slicer command: /Applications/PrusaSlicer.app/Contents/MacOS/prusa-slicer --load ...
```

**Test manually**:

```bash
/Applications/PrusaSlicer.app/Contents/MacOS/prusa-slicer \
  --load slicer-configs/bambu_a1.ini \
  --output /tmp/test_output.gcode \
  --export-gcode \
  test_cube.stl
```

### Invalid G-code Output

**Verify INI file syntax**:
- Use PrusaSlicer GUI to validate the config
- Check for missing required parameters
- Ensure temperatures are appropriate for the material

## Comparison: Mock vs Real Slicer

| Feature | Mock Slicer | Real PrusaSlicer |
|---------|------------|------------------|
| Speed | Very fast (~50ms) | Slower (1-10 seconds) |
| Accuracy | Fixed dummy values | Accurate calculations |
| Requirements | None | PrusaSlicer installed |
| Use Case | Development/Testing | Production |
| Material Usage | Always 12.34g | Calculated from model |
| Print Time | Always 83 min | Calculated from model |

## Recommended Setup

### Development
- Use **Mock Slicer** for fast testing
- No installation required
- Quick iteration

### Staging/Production
- Use **Real PrusaSlicer** for accurate quotes
- Create specific INI files for your printers
- Test with actual 3D models
- Validate pricing calculations

## Next Steps

1. Install PrusaSlicer
2. Update `application.properties`
3. Test with the provided `bambu_a1.ini`
4. Create custom INI files for your specific printers and materials
5. Validate pricing with real models

## Resources

- [PrusaSlicer Documentation](https://help.prusa3d.com/tag/prusaslicer)
- [PrusaSlicer Config Options](https://github.com/prusa3d/PrusaSlicer/blob/master/resources/profiles/)
- [G-code Reference](https://marlinfw.org/meta/gcode/)
