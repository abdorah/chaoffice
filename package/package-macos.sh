#!/bin/bash
# ============================================================
# ChaOffice - macOS Package Builder
# Creates .dmg installer
# Requires: JDK 17+, Xcode command line tools
# ============================================================

set -e

echo "========================================"
echo " ChaOffice macOS Packaging"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

APP_VERSION="1.0.0"
INPUT_DIR="target/jpackage-input"
OUTPUT_DIR="target/installer"

# 1. Build the fat JAR
echo "[1/3] Building fat JAR..."
mvn clean package -DskipTests -q

# 2. Set up staging directory
echo "[2/3] Preparing staging directory..."
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"
cp "target/chaoffice-${APP_VERSION}-fat.jar" "$INPUT_DIR/chaoffice.jar"
mkdir -p "$OUTPUT_DIR"

# 3. Run jpackage
echo "[3/3] Running jpackage..."

jpackage \
    --type dmg \
    --name "ChaOffice" \
    --app-version "$APP_VERSION" \
    --vendor "Chaos Office" \
    --description "Parts Inventory Management System" \
    --icon "package/icon/chaoffice.icns" \
    --input "$INPUT_DIR" \
    --main-jar "chaoffice.jar" \
    --main-class "org.chaos.office.Launcher" \
    --dest "$OUTPUT_DIR" \
    --java-options "-Xmx512m" \
    --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/java.io=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/java.util=ALL-UNNAMED" \
    --java-options "-Dapple.awt.application.name=ChaOffice" \
    --mac-package-name "ChaOffice" \
    --mac-app-category "business"

echo "========================================"
echo " SUCCESS! DMG created in:"
echo " $OUTPUT_DIR"
ls -la "$OUTPUT_DIR"
echo "========================================"
