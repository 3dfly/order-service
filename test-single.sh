#!/bin/bash

# Simple test script for the Print Quotation API
# This tests a single valid request

echo "Testing Print Quotation API..."
echo "================================"
echo ""

# Make sure the test file exists
if [ ! -f "test_cube.stl" ]; then
  echo "ERROR: test_cube.stl not found!"
  echo "Please run this script from the project root directory."
  exit 1
fi

# Make the request
echo "Sending request with:"
echo "  - File: test_cube.stl"
echo "  - Technology: FDM"
echo "  - Material: PLA"
echo "  - Layer Height: 0.2mm"
echo "  - Shells: 2"
echo "  - Infill: 15%"
echo "  - Supporters: true"
echo ""
echo "Response:"
echo "----------"

curl -X POST http://localhost:8080/api/print/quotation \
  -F "file=@test_cube.stl" \
  -F "technology=FDM" \
  -F "material=PLA" \
  -F "layerHeight=0.2" \
  -F "shells=2" \
  -F "infill=15" \
  -F "supporters=true"

echo ""
echo ""
echo "================================"
echo "Test completed!"
