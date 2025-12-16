# 3D Print Quotation API - Testing Guide

This guide explains how to test the 3D Print Quotation API locally using Postman or curl commands.

## API Endpoint

```
POST /api/print/quotation
```

## Prerequisites

1. **Start the application**:
   ```bash
   ./gradlew bootRun
   ```
   The API will be available at `http://localhost:8080`

2. **Ensure you have a 3D model file** (STL, OBJ, or 3MF format)

## Request Parameters

| Parameter | Type | Required | Description | Valid Values |
|-----------|------|----------|-------------|--------------|
| file | File | Yes | 3D model file | STL, OBJ, 3MF formats |
| technology | String | Yes | Printing technology | FDM, SLS, SLA |
| material | String | Yes | Printing material | PLA, ABS, PETG, TPU |
| layerHeight | Double | Yes | Layer height in mm | 0.05 - 0.4 |
| shells | Integer | Yes | Number of perimeter shells | 1 - 5 |
| infill | Integer | Yes | Infill percentage | 0, 10, 15, 20, 100 |
| supporters | Boolean | Yes | Use support structures | true, false |

## Valid Technology-Material Combinations

| Technology | Supported Materials |
|------------|-------------------|
| FDM | PLA, ABS, PETG, TPU |
| SLS | PLA, ABS, PETG |
| SLA | PLA, ABS |

## Response Format

### Success Response (200 OK)

```json
{
  "fileName": "model.stl",
  "technology": "FDM",
  "material": "PLA",
  "layerHeight": 0.2,
  "shells": 2,
  "infill": 15,
  "supporters": true,
  "estimatedPrice": 12.50,
  "currency": "USD",
  "materialUsedGrams": 25.5,
  "printingTimeMinutes": 120,
  "pricePerGram": 0.05,
  "pricePerMinute": 0.08,
  "materialCost": 1.28,
  "timeCost": 9.60
}
```

### Error Responses

#### Invalid Material Combination (400 Bad Request)
```json
{
  "timestamp": "2025-12-09T11:00:00",
  "status": 400,
  "error": "Invalid Parameter Combination",
  "message": "Material TPU is not compatible with technology SLS"
}
```

#### Validation Error (400 Bad Request)
```json
{
  "timestamp": "2025-12-09T11:00:00",
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "supporters": "Supporters parameter is required",
    "layerHeight": "Layer height must be between 0.05 and 0.4"
  }
}
```

#### Invalid File Type (400 Bad Request)
```json
{
  "timestamp": "2025-12-09T11:00:00",
  "status": 400,
  "error": "Invalid File Type",
  "message": "Unsupported file type: txt. Supported formats: STL, OBJ, 3MF"
}
```

## Testing Options

### Option 1: Using the Test Script (Recommended)

Run the automated test script that includes all test cases:

```bash
./test-api.sh
```

This will:
- Create a test STL file if it doesn't exist
- Run 11 different test cases covering valid/invalid scenarios
- Display results for each test

### Option 2: Using Postman

1. **Import the collection**:
   - Open Postman
   - Click "Import"
   - Select the `postman_collection.json` file
   - The collection will appear in your workspace

2. **Update file paths**:
   - In each request, update the `file` field to point to your STL file
   - Default path is `/path/to/your/model.stl`

3. **Run requests**:
   - Expand the collection folders
   - Select a request and click "Send"

### Option 3: Manual curl Commands

#### Example 1: Valid FDM + PLA + Supporters

```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
```

#### Example 2: Valid SLS + PETG + No Supporters

```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=PETG" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=false"
```

#### Example 3: Invalid Combination (SLS + TPU)

```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=TPU" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
```

Expected response: 400 Bad Request with "Invalid Parameter Combination" error

#### Example 4: Invalid Layer Height

```bash
curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.01" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
```

Expected response: 400 Bad Request with "Validation Failed" error

## Test Cases Included

The test script and Postman collection include:

### Valid Combinations (Should return 200 OK)
1. FDM + PLA + Supporters
2. FDM + TPU + No Supporters
3. SLS + PETG + Supporters
4. SLA + ABS + No Supporters

### Invalid Combinations (Should return 400 Bad Request)
5. SLS + TPU (Material not supported for technology)
6. SLA + PETG (Material not supported for technology)
7. SLA + TPU (Material not supported for technology)

### Validation Errors (Should return 400 Bad Request)
8. Missing Supporters Parameter
9. Invalid Layer Height (0.01 - too small)
10. Invalid Shells Count (10 - too many)
11. Invalid Infill (25 - not in allowed list)

## Configuration

The API uses the following configuration (from `application.properties`):

### Pricing Configuration
- **Technology multipliers**:
  - FDM: 1.0
  - SLS: 1.5
  - SLA: 2.0

- **Material pricing** (example for PLA):
  - Density: 1.24 g/cm³
  - Price per gram: $0.05

### Slicer Configuration
- Slicer path: `/usr/local/bin/prusa-slicer` (or BambuStudio)
- Configuration files directory: `slicer-configs/`
- Temporary files directory: `/tmp/printing`

## Troubleshooting

### Error: "Cannot run program prusa-slicer"
- Make sure PrusaSlicer or BambuStudio is installed
- Update `printing.bambu.slicer.path` in `application.properties` to the correct path

### Error: "File is empty"
- Ensure the STL file is not corrupted
- File must be at least 1 byte

### Error: "Unsupported file type"
- Only STL, OBJ, and 3MF formats are supported
- Check file extension

### Connection Refused
- Ensure the application is running: `./gradlew bootRun`
- Check if port 8080 is available
- Verify the base URL is correct

## Additional Notes

- Maximum file size: 50MB (configurable in `application.properties`)
- All prices are returned in USD
- Material usage and printing time are estimated based on slicer output
- Support structures are optional and affect pricing
