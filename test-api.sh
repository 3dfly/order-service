#!/bin/bash

# 3D Print Quotation API Test Script
# This script contains curl commands to test the print quotation API locally

BASE_URL="http://localhost:8080"
ENDPOINT="/api/print/quotation"

# Create a test STL file if it doesn't exist
if [ ! -f "test_cube.stl" ]; then
  echo "Creating test STL file..."
  cat > test_cube.stl << 'EOF'
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
EOF
  echo "Test STL file created: test_cube.stl"
fi

echo "========================================="
echo "3D Print Quotation API Test Suite"
echo "========================================="
echo ""

# Test 1: Valid FDM + PLA + Supporters
echo "Test 1: FDM + PLA + Supporters (VALID)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
echo -e "\n\n"

# Test 2: Valid FDM + TPU + No Supporters
echo "Test 2: FDM + TPU + No Supporters (VALID)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=TPU" \
  -F "layerHeight=0.3" \
  -F "shells=3" \
  -F "infill=20" \
  -F "supporters=false"
echo -e "\n\n"

# Test 3: Valid SLS + PETG + Supporters
echo "Test 3: SLS + PETG + Supporters (VALID)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=PETG" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
echo -e "\n\n"

# Test 4: Valid SLA + ABS + No Supporters
echo "Test 4: SLA + ABS + No Supporters (VALID)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=SLA" \
  -F "material=ABS" \
  -F "layerHeight=0.1" \
  -F "shells=4" \
  -F "infill=10" \
  -F "supporters=false"
echo -e "\n\n"

# Test 5: Invalid SLS + TPU
echo "Test 5: SLS + TPU (INVALID COMBINATION)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=SLS" \
  -F "material=TPU" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
echo -e "\n\n"

# Test 6: Invalid SLA + PETG
echo "Test 6: SLA + PETG (INVALID COMBINATION)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=SLA" \
  -F "material=PETG" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
echo -e "\n\n"

# Test 7: Invalid SLA + TPU
echo "Test 7: SLA + TPU (INVALID COMBINATION)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=SLA" \
  -F "material=TPU" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
echo -e "\n\n"

# Test 8: Missing Supporters Parameter
echo "Test 8: Missing Supporters Parameter (VALIDATION ERROR)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15"
echo -e "\n\n"

# Test 9: Invalid Layer Height (Too Small)
echo "Test 9: Invalid Layer Height - 0.01 (VALIDATION ERROR)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.01" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"
echo -e "\n\n"

# Test 10: Invalid Shells Count (Too Many)
echo "Test 10: Invalid Shells Count - 10 (VALIDATION ERROR)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=10" \
  -F "infill=15" \
  -F "supporters=true"
echo -e "\n\n"

# Test 11: Invalid Infill (Not in Allowed List)
echo "Test 11: Invalid Infill - 25 (VALIDATION ERROR)"
echo "---------------------------------------"
curl -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=25" \
  -F "supporters=true"
echo -e "\n\n"

echo "========================================="
echo "Test suite completed!"
echo "========================================="
