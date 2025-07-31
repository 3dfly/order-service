#!/usr/bin/env python3
"""
Test Mock STL Analyzer
Returns consistent results for testing purposes
"""

import sys
import os

def main():
    if len(sys.argv) < 2:
        print("Error: No STL file provided")
        sys.exit(1)
    
    # Mock consistent results for testing
    print("Test Mock STL Analyzer")
    print("filament used = 25.5g")
    print("estimated printing time = 1h 35m")

if __name__ == "__main__":
    main() 