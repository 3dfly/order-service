#!/bin/bash

echo "🧪 Testing Order Service 3D Printing API"
echo "=========================================="

# Configuration
API_BASE_URL="http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com"
TEST_STL_FILE="test-model.stl"

echo "🌐 API Base URL: $API_BASE_URL"
echo ""

# Test 1: Health Check
echo "📋 Test 1: Health Check"
echo "========================"
echo "GET $API_BASE_URL/health"
curl -s "$API_BASE_URL/health" | jq '.' 2>/dev/null || curl -s "$API_BASE_URL/health"
echo ""
echo ""

# Test 2: Basic Orders API
echo "📋 Test 2: Orders API"
echo "====================="
echo "GET $API_BASE_URL/orders"
curl -s "$API_BASE_URL/orders" | jq '.' 2>/dev/null || curl -s "$API_BASE_URL/orders"
echo ""
echo ""

# Test 3: Check if STL test file exists
echo "📋 Test 3: STL File Check"
echo "=========================="
if [ ! -f "$TEST_STL_FILE" ]; then
    echo "⚠️  Test STL file not found. Creating a minimal test STL file..."
    
    # Create a simple ASCII STL file for testing
    cat > "$TEST_STL_FILE" << 'EOF'
solid test_cube
  facet normal 0.0 0.0 1.0
    outer loop
      vertex 0.0 0.0 1.0
      vertex 1.0 0.0 1.0
      vertex 1.0 1.0 1.0
    endloop
  endfacet
  facet normal 0.0 0.0 1.0
    outer loop
      vertex 0.0 0.0 1.0
      vertex 1.0 1.0 1.0
      vertex 0.0 1.0 1.0
    endloop
  endfacet
  facet normal 0.0 0.0 -1.0
    outer loop
      vertex 0.0 0.0 0.0
      vertex 1.0 1.0 0.0
      vertex 1.0 0.0 0.0
    endloop
  endfacet
  facet normal 0.0 0.0 -1.0
    outer loop
      vertex 0.0 0.0 0.0
      vertex 0.0 1.0 0.0
      vertex 1.0 1.0 0.0
    endloop
  endfacet
  facet normal 0.0 -1.0 0.0
    outer loop
      vertex 0.0 0.0 0.0
      vertex 1.0 0.0 0.0
      vertex 1.0 0.0 1.0
    endloop
  endfacet
  facet normal 0.0 -1.0 0.0
    outer loop
      vertex 0.0 0.0 0.0
      vertex 1.0 0.0 1.0
      vertex 0.0 0.0 1.0
    endloop
  endfacet
  facet normal 1.0 0.0 0.0
    outer loop
      vertex 1.0 0.0 0.0
      vertex 1.0 1.0 0.0
      vertex 1.0 1.0 1.0
    endloop
  endfacet
  facet normal 1.0 0.0 0.0
    outer loop
      vertex 1.0 0.0 0.0
      vertex 1.0 1.0 1.0
      vertex 1.0 0.0 1.0
    endloop
  endfacet
  facet normal 0.0 1.0 0.0
    outer loop
      vertex 0.0 1.0 0.0
      vertex 1.0 1.0 1.0
      vertex 1.0 1.0 0.0
    endloop
  endfacet
  facet normal 0.0 1.0 0.0
    outer loop
      vertex 0.0 1.0 0.0
      vertex 0.0 1.0 1.0
      vertex 1.0 1.0 1.0
    endloop
  endfacet
  facet normal -1.0 0.0 0.0
    outer loop
      vertex 0.0 0.0 0.0
      vertex 0.0 1.0 1.0
      vertex 0.0 1.0 0.0
    endloop
  endfacet
  facet normal -1.0 0.0 0.0
    outer loop
      vertex 0.0 0.0 0.0
      vertex 0.0 0.0 1.0
      vertex 0.0 1.0 1.0
    endloop
  endfacet
endsolid test_cube
EOF
    
    echo "✅ Created test STL file: $TEST_STL_FILE"
else
    echo "✅ Test STL file found: $TEST_STL_FILE"
fi
echo ""

# Test 4: 3D Printing Price Calculation
echo "📋 Test 4: 3D Printing Price Calculation"
echo "========================================="
echo "POST $API_BASE_URL/orders/calculate"
echo "Uploading file: $TEST_STL_FILE"
echo ""

# Test the printing calculation endpoint
echo "📤 Sending request..."
RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
    -X POST \
    -F "stlFile=@$TEST_STL_FILE" \
    "$API_BASE_URL/orders/calculate")

HTTP_CODE=$(echo "$RESPONSE" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed 's/HTTP_CODE:[0-9]*$//')

echo "🔍 Response Code: $HTTP_CODE"
echo "📝 Response Body:"

if command -v jq &> /dev/null; then
    echo "$BODY" | jq '.'
else
    echo "$BODY"
fi

echo ""

# Analyze the response
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ SUCCESS: Printing calculation completed successfully!"
    
    # Try to extract price information if jq is available
    if command -v jq &> /dev/null; then
        TOTAL_PRICE=$(echo "$BODY" | jq -r '.totalPrice // empty')
        WEIGHT=$(echo "$BODY" | jq -r '.weightGrams // empty')
        TIME=$(echo "$BODY" | jq -r '.printingTimeMinutes // empty')
        STATUS=$(echo "$BODY" | jq -r '.status // empty')
        
        if [ ! -z "$TOTAL_PRICE" ]; then
            echo ""
            echo "💰 Pricing Details:"
            echo "   - Total Price: \$$TOTAL_PRICE"
            echo "   - Weight: ${WEIGHT}g"
            echo "   - Print Time: ${TIME} minutes"
            echo "   - Status: $STATUS"
        fi
    fi
elif [ "$HTTP_CODE" = "400" ]; then
    echo "⚠️  WARNING: Bad request - check the response for details"
elif [ "$HTTP_CODE" = "500" ]; then
    echo "❌ ERROR: Server error - the slicer might not be working properly"
else
    echo "❓ UNKNOWN: Unexpected response code: $HTTP_CODE"
fi

echo ""
echo "🔍 Troubleshooting Tips:"
echo "========================"
echo "1. If you get a 500 error, the slicer might not be installed properly"
echo "2. If you get a 400 error, check that the STL file is valid"
echo "3. Check the ECS logs in CloudWatch for detailed error messages:"
echo "   aws logs tail /ecs/order-service --follow --region eu-west-1"
echo ""
echo "📊 API Documentation:"
echo "===================="
echo "Endpoint: POST /orders/calculate"
echo "Content-Type: multipart/form-data"
echo "Parameter: stlFile (file upload)"
echo ""
echo "Expected Response Format:"
echo "{"
echo "  \"totalPrice\": 12.45,"
echo "  \"weightGrams\": 89.0,"
echo "  \"printingTimeMinutes\": 80,"
echo "  \"pricePerGram\": 0.05,"
echo "  \"pricePerMinute\": 0.10,"
echo "  \"weightCost\": 4.45,"
echo "  \"timeCost\": 8.00,"
echo "  \"filename\": \"model.stl\","
echo "  \"status\": \"SUCCESS\","
echo "  \"message\": \"Price calculated successfully\""
echo "}" 