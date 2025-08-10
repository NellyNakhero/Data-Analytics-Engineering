#!/bin/bash

set -e

PLUGIN_DIR="connect-plugins/aiven-smts"
REPO_URL="https://github.com/Aiven-Open/transforms-for-apache-kafka-connect"
LATEST_RELEASE_API="https://api.github.com/repos/Aiven-Open/transforms-for-apache-kafka-connect/releases/latest"

echo "📥 Fetching latest release info from Aiven..."
DOWNLOAD_URL=$(curl -s "$LATEST_RELEASE_API" | grep browser_download_url | grep ".zip" | cut -d '"' -f 4)

mkdir -p "$PLUGIN_DIR"
cd "$PLUGIN_DIR"

echo "📦 Downloading: $DOWNLOAD_URL"
curl -L -o aiven-smt.zip "$DOWNLOAD_URL"
unzip -o aiven-smt.zip

echo "✅ Aiven SMT downloaded to: $PLUGIN_DIR"
