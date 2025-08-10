#!/bin/bash

set -e

PLUGIN_DIR="connect-plugins/aiven-smts"
REPO="Aiven-Open/transforms-for-apache-kafka-connect"
LATEST_RELEASE_API="https://api.github.com/repos/${REPO}/releases/latest"
KOTLIN_VERSION="1.9.10"

echo "📥 Fetching latest Aiven SMT release..."
DOWNLOAD_URL=$(curl -s "$LATEST_RELEASE_API" | jq -r '.assets[] | select(.name | endswith(".zip")) | .browser_download_url')

if [[ -z "$DOWNLOAD_URL" ]]; then
  echo "❌ Could not find a .zip release asset from Aiven's latest release."
  echo "   Please check https://github.com/${REPO}/releases"
  exit 1
fi

mkdir -p "$PLUGIN_DIR"
cd "$PLUGIN_DIR"

echo "📦 Downloading SMT plugin from: $DOWNLOAD_URL"
curl -L -o aiven-smt.zip "$DOWNLOAD_URL"
unzip -o aiven-smt.zip
rm aiven-smt.zip

echo "📚 Downloading Kotlin dependencies..."

# List of required Kotlin dependencies
declare -a DEPENDENCIES=(
  "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"
  "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"
  "org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"
  "org.jetbrains.kotlin:kotlin-compiler:$KOTLIN_VERSION"
)

for dep in "${DEPENDENCIES[@]}"; do
  IFS=':' read -ra parts <<< "$dep"
  GROUP="${parts[0]//./\/}"
  ARTIFACT="${parts[1]}"
  VERSION="${parts[2]}"
  FILE_NAME="${ARTIFACT}-${VERSION}.jar"
  JAR_URL="https://repo1.maven.org/maven2/${GROUP}/${ARTIFACT}/${VERSION}/${FILE_NAME}"

  echo "⬇️  Downloading: $FILE_NAME"
  curl -s -L -O "$JAR_URL"
done

echo "✅ Aiven SMT plugin and dependencies ready in: $(pwd)"
