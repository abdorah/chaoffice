#!/bin/bash
# ============================================================
# ChaOffice - Linux Package Builder
# Creates .deb (Debian/Ubuntu) and .rpm (Fedora/RHEL)
# Requires: JDK 17+, dpkg (for .deb) or rpm-build (for .rpm)
# ============================================================

set -e

echo "========================================"
echo " ChaOffice Linux Packaging"
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

# 3. Detect available package type
PACKAGE_TYPE="deb"
if command -v rpmbuild &> /dev/null && ! command -v dpkg &> /dev/null; then
    PACKAGE_TYPE="rpm"
fi

echo "[3/3] Running jpackage (type: $PACKAGE_TYPE)..."

jpackage \
    --type "$PACKAGE_TYPE" \
    --name "chaoffice" \
    --app-version "$APP_VERSION" \
    --vendor "Chaos Office" \
    --description "Parts Inventory Management System" \
    --icon "package/icon/chaoffice.png" \
    --input "$INPUT_DIR" \
    --main-jar "chaoffice.jar" \
    --main-class "org.chaos.office.Launcher" \
    --dest "$OUTPUT_DIR" \
    --java-options "-Xmx512m" \
    --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/java.io=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/java.util=ALL-UNNAMED" \
    --linux-shortcut \
    --linux-menu-group "Office" \
    --linux-app-category "Office"

echo "========================================"
echo " SUCCESS! Package created in:"
echo " $OUTPUT_DIR"
ls -la "$OUTPUT_DIR"
echo "========================================"
