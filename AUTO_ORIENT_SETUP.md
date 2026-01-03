# Auto-Orientation Setup Guide

This guide explains how to set up the auto-orientation feature for 3D models.

## Overview

The auto-orientation feature automatically rotates uploaded 3D models to the optimal orientation for printing:
- **Largest flat surface** placed on the build plate
- **Minimized height** to reduce print time and improve stability
- **Maximized bottom surface area** for better bed adhesion

## Prerequisites

### 1. Python 3 Installation

**macOS:**
```bash
# Using Homebrew
brew install python3

# Verify installation
python3 --version
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install python3 python3-pip

# Verify installation
python3 --version
```

**Windows:**
- Download from [python.org](https://www.python.org/downloads/)
- Ensure "Add Python to PATH" is checked during installation

### 2. Install Python Dependencies

Navigate to your project directory and install the required packages:

```bash
cd /path/to/order-service
pip3 install -r scripts/requirements.txt
```

Or install manually:
```bash
pip3 install trimesh numpy scipy
```

### 3. Verify Installation

Test the auto-orientation script:

```bash
# Make the script executable (Unix/macOS)
chmod +x scripts/auto_orient_model.py

# Test with a sample STL file
python3 scripts/auto_orient_model.py test_model.stl test_model_oriented.stl
```

## Configuration

### Application Properties

Add these properties to your `application.properties` or `application.yml`:

```properties
# Auto-orientation configuration
printing.orientation.enabled=true
printing.orientation.script.path=scripts/auto_orient_model.py
printing.orientation.timeout=60000
```

**Configuration Options:**

| Property | Default | Description |
|----------|---------|-------------|
| `printing.orientation.enabled` | `true` | Enable/disable auto-orientation globally |
| `printing.orientation.script.path` | `scripts/auto_orient_model.py` | Path to Python orientation script |
| `printing.orientation.timeout` | `60000` | Timeout in milliseconds (60 seconds) |

### Disable Auto-Orientation

If you want to disable the feature (Python not available, testing, etc.):

```properties
printing.orientation.enabled=false
```

## How It Works

### 1. User Upload
User uploads a 3D model (STL/OBJ/3MF) with `autoOrient=true` (default)

### 2. Geometry Analysis
The system analyzes the model:
- Identifies all large flat surfaces
- Calculates surface areas and normals
- Scores each potential orientation

### 3. Optimal Rotation
The model is rotated to maximize:
- Bottom surface area (weight: 2x)
- Minimize height (weight: 1x)

### 4. Centering
The model is:
- Centered on XY plane
- Bottom placed at Z=0 (build plate)

### 5. Slicing
The oriented model is sent to the slicer

## Algorithm Details

The auto-orientation algorithm:

1. **Face Analysis**: Examines the top 20 largest faces
2. **Orientation Scoring**: For each candidate face:
   ```
   score = (bottom_area / total_area) * 2.0 + (1 - height/max_height) * 1.0
   ```
3. **Best Selection**: Chooses the orientation with the highest score
4. **Transformation**: Applies rotation and translation matrices
5. **Export**: Saves as STL for slicing

### Scoring Weights

- **Bottom Surface Area** (2.0x): Prioritizes large, flat surfaces for bed adhesion
- **Height Reduction** (1.0x): Reduces print time and material usage

You can adjust these weights in the Python script if needed.

## API Usage

### Enable Auto-Orientation (Default)

```bash
curl -X POST "http://localhost:8080/api/v1/quotations/print" \
  -F "file=@model.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=3" \
  -F "infill=15" \
  -F "supporters=true" \
  -F "autoOrient=true"
```

### Disable Auto-Orientation

```bash
curl -X POST "http://localhost:8080/api/v1/quotations/print" \
  -F "file=@model.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=3" \
  -F "infill=15" \
  -F "supporters=true" \
  -F "autoOrient=false"
```

## Logs

When auto-orientation is active, you'll see logs like:

```
🔄 Auto-orienting model: model.stl
✅ Model auto-oriented successfully
   Original height: 45.23 mm
   Final height: 32.15 mm
   Height reduction: 13.08 mm
   Bottom surface area: 234.56 mm²
   Bottom area: 23.4% of total
   Rotation applied: true
```

If orientation fails (timeout, error), the original model is used:

```
⚠️ Auto-orientation failed, using original model
```

## Troubleshooting

### Python Not Found

**Error:** `Python 3 not available, auto-orientation disabled`

**Solution:**
- Install Python 3
- Ensure `python3` command is in PATH
- Restart the application

### Script Not Found

**Error:** `Orientation script not found at: scripts/auto_orient_model.py`

**Solution:**
- Verify the script exists at the specified path
- Update `printing.orientation.script.path` if script is in a different location
- Ensure file permissions (make executable on Unix)

### Trimesh Import Error

**Error:** `ModuleNotFoundError: No module named 'trimesh'`

**Solution:**
```bash
pip3 install trimesh numpy scipy
```

### Timeout Issues

**Error:** `Auto-orientation timed out`

**Solution:**
- Increase timeout: `printing.orientation.timeout=120000` (2 minutes)
- For very large models, consider:
  - Simplifying the mesh before upload
  - Disabling auto-orient for large files

### Memory Issues

For very large STL files (>100MB):

**Solution:**
- Increase Python memory limits
- Pre-process/simplify models before upload
- Consider disabling auto-orient for large files

## Performance

### Typical Processing Times

| Model Size | Faces | Time |
|------------|-------|------|
| Small | <10K | 1-2s |
| Medium | 10K-50K | 2-5s |
| Large | 50K-200K | 5-15s |
| Very Large | >200K | 15-60s |

### Optimization Tips

1. **Reduce face count**: Only checks top 20 largest faces
2. **Early exit**: Skips small faces (<5% of surface)
3. **Timeout protection**: Configurable timeout prevents hangs

## Alternative Implementations

If Python isn't available, you have other options:

### Option 2: Java-Only with Java3D

Use pure Java libraries (no external dependencies):
- More complex to implement
- Slower performance
- No external dependencies

### Option 3: Pre-Orient Client-Side

Require users to orient models before upload:
- No server-side processing
- User has full control
- Requires 3D knowledge

### Option 4: Disable Feature

Simply disable and document:
```properties
printing.orientation.enabled=false
```

## Testing

### Unit Tests

The `ModelOrientationService` includes:
- Availability checks
- Timeout handling
- Fallback to original model
- Cleanup verification

### Integration Test

Test the full flow:

```bash
# 1. Start the application
./gradlew bootRun

# 2. Upload a test model
curl -X POST "http://localhost:8080/api/v1/quotations/print" \
  -F "file=@test/resources/test_model.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=3" \
  -F "infill=15" \
  -F "supporters=true" \
  -F "autoOrient=true"

# 3. Check logs for orientation success
```

## Production Considerations

### Docker Deployment

Add to your `Dockerfile`:

```dockerfile
FROM openjdk:17-slim

# Install Python and dependencies
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    pip3 install trimesh numpy scipy && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy application
COPY build/libs/*.jar app.jar
COPY scripts/ scripts/

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes

For Kubernetes deployments:
- Include Python in container image
- Mount scripts as ConfigMap (for easy updates)
- Set appropriate resource limits

### Performance Monitoring

Monitor:
- Orientation processing time
- Success/failure rates
- Timeout occurrences
- Memory usage

Add metrics in `ModelOrientationService` using Spring Actuator.

## Support

For issues:
- Check logs first
- Verify Python installation: `python3 --version`
- Test script directly: `python3 scripts/auto_orient_model.py model.stl output.stl`
- Report issues with full error logs

## Further Reading

- [Trimesh Documentation](https://trimsh.org/)
- [3D Printing Orientation Optimization](https://www.sciencedirect.com/topics/engineering/build-orientation)
- [STL File Format](https://en.wikipedia.org/wiki/STL_(file_format))
