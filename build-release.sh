#!/bin/bash
# ChaOffice Release Build Script for Linux/Mac
# This script creates a distributable package of the application

echo "========================================"
echo "ChaOffice Release Build Script"
echo "========================================"
echo ""

# Step 1: Clean previous builds
echo "[1/5] Cleaning previous builds..."
mvn clean
if [ $? -ne 0 ]; then
    echo "ERROR: Clean failed!"
    exit 1
fi
echo ""

# Step 2: Run tests
echo "[2/5] Running tests..."
mvn test
if [ $? -ne 0 ]; then
    echo "ERROR: Tests failed!"
    exit 1
fi
echo ""

# Step 3: Package application
echo "[3/5] Packaging application (Fat JAR)..."
mvn package -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: Packaging failed!"
    exit 1
fi
echo ""

# Step 4: Create jpackage runtime image (alternative to jlink)
echo "[4/5] Creating portable application with jpackage..."
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type app-image --app-version 1.0.0 --vendor "ChaOffice" --description "ChaOffice Parts Inventory Management System"
if [ $? -ne 0 ]; then
    echo "WARNING: jpackage failed. Continuing with JAR only..."
    echo "You can still use the JAR file: target/chaoffice-1.0.0.jar"
fi
echo ""

# Step 5: Create distribution zip
echo "[5/5] Creating distribution package..."
cd target
rm -f ChaOffice-1.0.0-linux.zip

# Check if jpackage created the ChaOffice folder
if [ -d "../ChaOffice" ]; then
    echo "Packaging jpackage output..."
    cd ..
    zip -r target/ChaOffice-1.0.0-linux.zip ChaOffice
    cd target
else
    echo "jpackage folder not found, packaging JAR only..."
    zip ChaOffice-1.0.0-linux.zip chaoffice-1.0.0.jar
fi
cd ..
echo ""

echo "========================================"
echo "Build completed successfully!"
echo "========================================"
echo ""
echo "Output files:"
echo "  - Fat JAR: target/chaoffice-1.0.0.jar"
if [ -d "ChaOffice" ]; then
    echo "  - Portable App: ChaOffice/bin/ChaOffice"
    echo "  - Distribution: target/ChaOffice-1.0.0-linux.zip"
else
    echo "  - Distribution: target/ChaOffice-1.0.0-linux.zip (JAR only)"
fi
echo ""
echo "To run the application:"
if [ -d "ChaOffice" ]; then
    echo "  ./ChaOffice/bin/ChaOffice"
    echo "  OR"
fi
echo "  java -jar target/chaoffice-1.0.0.jar"
echo ""
