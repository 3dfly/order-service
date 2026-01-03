# Print Quotation API - Extended Parameters

## Overview

The Print Quotation API now supports extended slicer parameters through dynamic INI file generation. This allows fine-grained control over print settings beyond the basic parameters.

## API Endpoint

```
POST /api/v1/quotations/print
Content-Type: multipart/form-data
```

## Request Parameters

### Required Parameters

| Parameter | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| `file` | File | STL, OBJ, or 3MF | 3D model file to be quoted |
| `technology` | String | FDM, SLS, or SLA | Printing technology |
| `material` | String | PLA, ABS, PETG, or TPU | Material type |
| `layerHeight` | Double | 0.05 - 0.4 mm | Layer height in millimeters |
| `shells` | Integer | 1 - 5 | Number of perimeter shells |
| `infill` | Integer | 5 - 20 % | Infill percentage |
| `supporters` | Boolean | true/false | Enable support structures |

### Optional Advanced Parameters

#### Shell Configuration

| Parameter | Type | Default | Constraints | Description |
|-----------|------|---------|-------------|-------------|
| `topShellLayers` | Integer | 5 | 0 - 10 | Number of solid layers on top |
| `bottomShellLayers` | Integer | 3 | 0 - 10 | Number of solid layers on bottom |

#### Infill Configuration

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `infillPattern` | Enum | GRID | Pattern for infill structure |

**Supported Infill Patterns:**
- `grid` - Standard grid pattern (default)
- `gyroid` - Gyroid pattern (strong and flexible)
- `honeycomb` - Honeycomb pattern
- `line` / `rectilinear` - Simple lines
- `cubic` - Cubic pattern
- `triangles` - Triangle pattern
- `concentric` - Concentric circles
- `hilbertcurve` - Hilbert curve pattern
- `archimedeanchords` - Archimedean chords
- `octagramspiral` - Octagram spiral
- `adaptivecubic` - Adaptive cubic (optimized)
- `supportcubic` - Support cubic pattern
- `lightning` - Lightning fast infill
- `crosshatch` - Crosshatch pattern
- `cross3d` - 3D cross pattern
- `honeycomb3d` - 3D honeycomb

#### Support Configuration

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `supportType` | Enum | NORMAL | Type of support structure (only if `supporters=true`) |

**Supported Support Types:**
- `normal` - Standard grid supports (default)
- `tree` / `tree-auto` - Tree-style supports (uses less material)
- `organic` - Organic supports (experimental)

#### Brim Configuration

| Parameter | Type | Default | Constraints | Description |
|-----------|------|---------|-------------|-------------|
| `brimType` | Enum | AUTO | - | Type of brim to add |
| `brimWidth` | Integer | - | 0 - 20 mm | Width of the brim in mm |

**Supported Brim Types:**
- `auto` - Automatic brim detection (default)
- `none` - No brim
- `custom` - Custom brim (requires `brimWidth`)

#### Seam Configuration

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `seam` | Enum | - | Position of the layer seam |

**Supported Seam Positions:**
- `random` - Random position per layer
- `aligned` - Aligned in a straight line
- `nearest` - Nearest to previous position
- `rear` - At the rear of the object
- `custom` - Custom position

#### Special Features

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `autoOrient` | Boolean | true | **NOT IMPLEMENTED** - See limitations below |
| `colorChange` | String | - | Comma-separated layer numbers for color changes (e.g., "10,20,30") |

## Response

```json
{
  "fileName": "model.stl",
  "materialUsedGrams": 25.5,
  "printingTimeMinutes": 120,
  "technology": "FDM",
  "material": "PLA",
  "layerHeight": 0.2,
  "shells": 3,
  "infill": 15,
  "supporters": true,
  "estimatedPrice": 12.50,
  "currency": "USD",
  "pricePerGram": 0.05,
  "pricePerMinute": 0.08,
  "materialCost": 1.28,
  "timeCost": 9.60,
  "brimType": "auto",
  "brimWidth": null,
  "supportType": "normal",
  "topShellLayers": 5,
  "bottomShellLayers": 3,
  "infillPattern": "GRID",
  "seam": null,
  "autoOrient": true,
  "colorChange": null
}
```

## Auto-Orientation Feature

### How It Works

The `autoOrient` parameter enables automatic model orientation for optimal printing. When enabled (default), the system:

1. **Analyzes** the 3D model geometry
2. **Identifies** the largest flat surfaces
3. **Rotates** the model to place the best surface on the build plate
4. **Minimizes** print height while maximizing bed adhesion
5. **Centers** the model on the XY plane

### Algorithm

The auto-orientation uses advanced mesh analysis:
- Examines the top 20 largest faces
- Scores each orientation based on:
  - **Bottom surface area** (higher = better bed adhesion)
  - **Print height** (lower = faster print time)
- Selects the optimal orientation automatically

### Requirements

⚠️ **Requires Python 3 with trimesh library**

If Python is not available:
- The feature will automatically fallback to using the original orientation
- A warning will be logged
- Processing continues normally

See [AUTO_ORIENT_SETUP.md](AUTO_ORIENT_SETUP.md) for installation instructions.

### Configuration

Enable/disable via configuration:
```properties
printing.orientation.enabled=true  # Enable auto-orientation
printing.orientation.timeout=60000  # Timeout in milliseconds
```

### Performance

Typical processing times:
- **Small models** (<10K faces): 1-2 seconds
- **Medium models** (10K-50K faces): 2-5 seconds
- **Large models** (50K-200K faces): 5-15 seconds

### Color Change Feature

The `colorChange` parameter is only applicable for **3MF files**. For STL and OBJ files, this parameter will be ignored.

Format: Comma-separated layer numbers (e.g., "10,20,30")
- The printer will pause at these layers
- Allows manual filament color change
- Layer numbers must be valid positive integers

## Example Request

```bash
curl -X POST "http://localhost:8080/api/v1/quotations/print" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@model.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=3" \
  -F "infill=15" \
  -F "supporters=true" \
  -F "supportType=tree" \
  -F "topShellLayers=5" \
  -F "bottomShellLayers=3" \
  -F "infillPattern=gyroid" \
  -F "brimType=auto" \
  -F "seam=rear"
```

## Implementation Details

### Dynamic INI Generation

The service now uses a hybrid approach:

1. **Base Configuration**: Selects appropriate base INI file based on technology, material, and basic parameters
2. **Dynamic Override**: Generates a temporary INI file with all custom parameters
3. **CLI Override**: Critical parameters (layerHeight, shells, infill, supporters) are also passed via CLI for validation and override guarantee
4. **Cleanup**: Temporary INI files are automatically cleaned up after processing

### Benefits

- ✅ Support for advanced slicer parameters not available via CLI
- ✅ Enum-based parameters with validation
- ✅ String-based parameters (infill patterns, seam positions)
- ✅ Maintains security through validation
- ✅ Backward compatible with existing API calls

### Parameter Validation

All parameters undergo validation:
- **Range checks** for numeric values
- **Enum validation** for string parameters
- **Pattern matching** for complex strings (colorChange)
- **Conditional defaults** (e.g., supportType defaults to NORMAL if not specified)

## Migration Guide

### From Old API

If you're using the basic API, no changes are required. All new parameters are optional with sensible defaults.

### Adding Advanced Parameters

Simply add the new optional parameters to your requests:

```javascript
// Before
const formData = new FormData();
formData.append('file', file);
formData.append('technology', 'FDM');
formData.append('material', 'PLA');
formData.append('layerHeight', '0.2');
formData.append('shells', '3');
formData.append('infill', '15');
formData.append('supporters', 'true');

// After (with advanced parameters)
formData.append('infillPattern', 'gyroid');
formData.append('supportType', 'tree');
formData.append('topShellLayers', '5');
formData.append('seam', 'rear');
```

## Best Practices

1. **Start with defaults**: Only specify advanced parameters when needed
2. **Test combinations**: Some parameter combinations may not work well together
3. **Orient models manually**: Don't rely on autoOrient - it's not implemented
4. **Use tree supports**: For complex geometries, tree supports use less material
5. **Gyroid infill**: For strength with flexibility, consider gyroid pattern
6. **Seam positioning**: Use `rear` seam position for aesthetic parts

## Support

For issues or questions:
- GitHub Issues: https://github.com/3dfly/order-service/issues
- API Documentation: https://api.3dfly.com/docs

## Version History

### v1.2.0 (Current)
- ✅ **Implemented auto-orientation** using Python/trimesh
- Added automatic geometry analysis for optimal printing
- Graceful fallback if Python not available
- Performance optimizations for large models

### v1.1.0
- Added dynamic INI generation
- Support for 13+ infill patterns
- Support types (tree, organic, normal)
- Shell layer configuration
- Brim and seam positioning
- Color change layers (3MF only)
- Added autoOrient parameter (implementation in v1.2.0)

### v1.0.0
- Basic quotation API
- Core parameters only
