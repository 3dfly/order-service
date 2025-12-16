#!/bin/bash

# Slicer Configuration Script
# Helps you switch between mock slicer (development) and real PrusaSlicer (production)

CONFIG_FILE="src/main/resources/application.properties"
MOCK_PATH="/Users/sefica/Downloads/order-service/src/test/resources/mock-slicer/mock-slicer.sh"
PRUSA_PATH="/Applications/PrusaSlicer.app/Contents/MacOS/prusa-slicer"
BAMBU_PATH="/Applications/BambuStudio.app/Contents/MacOS/bambu-studio"

echo "========================================="
echo "3D Print Quotation API - Slicer Setup"
echo "========================================="
echo ""

# Check what slicers are available
echo "Checking available slicers..."
echo ""

AVAILABLE_SLICERS=()

# Check mock slicer
if [ -f "$MOCK_PATH" ]; then
    echo "✅ Mock Slicer: Available (for development/testing)"
    AVAILABLE_SLICERS+=("mock")
else
    echo "❌ Mock Slicer: Not found"
fi

# Check PrusaSlicer
if [ -f "$PRUSA_PATH" ]; then
    echo "✅ PrusaSlicer: Installed at $PRUSA_PATH"
    AVAILABLE_SLICERS+=("prusa")
else
    echo "❌ PrusaSlicer: Not installed"
    echo "   Download from: https://www.prusa3d.com/page/prusaslicer_424/"
fi

# Check BambuStudio
if [ -f "$BAMBU_PATH" ]; then
    echo "✅ BambuStudio: Installed at $BAMBU_PATH"
    AVAILABLE_SLICERS+=("bambu")
else
    echo "❌ BambuStudio: Not installed"
    echo "   Download from: https://bambulab.com/en/download/studio"
fi

echo ""
echo "========================================="
echo ""

# If no slicers available, exit
if [ ${#AVAILABLE_SLICERS[@]} -eq 0 ]; then
    echo "❌ No slicers available!"
    echo "Please install PrusaSlicer or BambuStudio to continue."
    exit 1
fi

# Prompt user to select slicer
echo "Select which slicer to use:"
echo ""

MENU_OPTIONS=()
MENU_PATHS=()

for slicer in "${AVAILABLE_SLICERS[@]}"; do
    case $slicer in
        mock)
            MENU_OPTIONS+=("Mock Slicer (Development/Testing)")
            MENU_PATHS+=("$MOCK_PATH")
            ;;
        prusa)
            MENU_OPTIONS+=("PrusaSlicer (Production)")
            MENU_PATHS+=("$PRUSA_PATH")
            ;;
        bambu)
            MENU_OPTIONS+=("BambuStudio (Production)")
            MENU_PATHS+=("$BAMBU_PATH")
            ;;
    esac
done

# Display menu
for i in "${!MENU_OPTIONS[@]}"; do
    echo "$((i+1)). ${MENU_OPTIONS[$i]}"
done

echo ""
read -p "Enter your choice (1-${#MENU_OPTIONS[@]}): " choice

# Validate choice
if ! [[ "$choice" =~ ^[0-9]+$ ]] || [ "$choice" -lt 1 ] || [ "$choice" -gt ${#MENU_OPTIONS[@]} ]; then
    echo "❌ Invalid choice"
    exit 1
fi

# Get selected path
SELECTED_PATH="${MENU_PATHS[$((choice-1))]}"
SELECTED_NAME="${MENU_OPTIONS[$((choice-1))]}"

echo ""
echo "Configuring $SELECTED_NAME..."

# Update application.properties
if grep -q "printing.bambu.slicer.path=" "$CONFIG_FILE"; then
    # Update existing line
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s|^printing.bambu.slicer.path=.*|printing.bambu.slicer.path=$SELECTED_PATH|" "$CONFIG_FILE"
        sed -i '' "s|^# printing.bambu.slicer.path=.*|printing.bambu.slicer.path=$SELECTED_PATH|" "$CONFIG_FILE"
    else
        # Linux
        sed -i "s|^printing.bambu.slicer.path=.*|printing.bambu.slicer.path=$SELECTED_PATH|" "$CONFIG_FILE"
        sed -i "s|^# printing.bambu.slicer.path=.*|printing.bambu.slicer.path=$SELECTED_PATH|" "$CONFIG_FILE"
    fi
    echo "✅ Configuration updated successfully!"
else
    echo "❌ Could not find slicer path configuration in $CONFIG_FILE"
    exit 1
fi

echo ""
echo "========================================="
echo "Current Configuration:"
echo "========================================="
grep "printing.bambu.slicer.path=" "$CONFIG_FILE" | grep -v "^#"
echo ""

echo "========================================="
echo "Next Steps:"
echo "========================================="
echo "1. Restart your application if it's running"
echo "2. Test the API with: ./test-single.sh"
echo ""

if [[ "$SELECTED_PATH" == *"mock-slicer"* ]]; then
    echo "ℹ️  Using Mock Slicer:"
    echo "   - Returns dummy values (12.34g, 83 minutes)"
    echo "   - Fast for development and testing"
    echo "   - No real slicer calculation"
else
    echo "ℹ️  Using Real Slicer:"
    echo "   - Returns accurate calculations"
    echo "   - Slower (1-10 seconds per request)"
    echo "   - Requires INI configuration files in slicer-configs/"
    echo ""
    echo "📚 See PRUSA_SLICER_SETUP.md for detailed setup instructions"
fi

echo ""
