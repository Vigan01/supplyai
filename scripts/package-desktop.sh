#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_NAME="SupplyAI"
VERSION="0.0.1"
JAR_NAME="supplyai-0.0.1-SNAPSHOT.jar"

cd "$ROOT_DIR"

if ! command -v jpackage >/dev/null 2>&1; then
  echo "jpackage was not found. Install a full JDK 21+ and try again." >&2
  exit 1
fi

./mvnw --batch-mode --no-transfer-progress clean package
rm -rf dist
mkdir -p dist

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --input target \
  --main-jar "$JAR_NAME" \
  --main-class com.supplyai.desktop.SupplyAiDesktopApplication \
  --dest dist

echo "Desktop app image created in dist/$APP_NAME"
