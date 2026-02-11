#!/bin/bash
#
# Generate Python code from Wirespec contracts
#
# This script helps generate Python code from the Wirespec contract.
# You can run it after building the Wirespec CLI.
#

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Wirespec Python Code Generator${NC}"
echo ""

# Check if wirespec CLI exists
WIRESPEC_BIN=""

# Check common locations
if [ -f "../../src/plugin/cli/build/install/cli/bin/wirespec" ]; then
    WIRESPEC_BIN="../../src/plugin/cli/build/install/cli/bin/wirespec"
elif command -v wirespec &> /dev/null; then
    WIRESPEC_BIN="wirespec"
else
    echo -e "${YELLOW}Warning: Wirespec CLI not found.${NC}"
    echo ""
    echo "Please build the CLI first from the repository root:"
    echo "  ./gradlew :src:plugin:cli:installDist"
    echo ""
    echo "Or install wirespec globally and ensure it's in your PATH."
    exit 1
fi

echo "Using Wirespec CLI: $WIRESPEC_BIN"
echo ""

# Clean previous generated code
echo -e "${GREEN}Cleaning previous generated code...${NC}"
rm -rf src/generated/*
mkdir -p src/generated

# Generate Python code
echo -e "${GREEN}Generating Python code from Wirespec contracts...${NC}"
$WIRESPEC_BIN compile \
    -i ./wirespec \
    -o ./src/generated \
    -l Python \
    --shared

# Check if generation was successful
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Code generation successful!${NC}"
    echo ""
    echo "Generated files:"
    find src/generated -name "*.py" | sed 's/^/  - /'
    echo ""
    echo "Next steps:"
    echo "  1. Install dependencies: pip install -r requirements.txt"
    echo "  2. Run tests: pytest tests/"
    echo ""
else
    echo -e "${RED}✗ Code generation failed${NC}"
    exit 1
fi
