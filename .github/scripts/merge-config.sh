#!/bin/bash
set -e

BASE_CONFIG=$1
PLUGIN_CONFIG=$2
MERGED_OUTPUT=$3

if [[ ! -f "$BASE_CONFIG" || ! -f "$PLUGIN_CONFIG" ]]; then
  echo "Missing base or plugin config!"
  exit 1
fi

# Extract connectorName and connectorConfiguration from plugin config
CONNECTOR_NAME=$(jq -r '.connectorName' "$PLUGIN_CONFIG")
CONNECTOR_CONFIGURATION=$(jq -c '.connectorConfiguration' "$PLUGIN_CONFIG")

# Merge using jq
jq \
  --arg name "$CONNECTOR_NAME" \
  --argjson config "$CONNECTOR_CONFIGURATION" \
  '. + {connectorName: $name, connectorConfiguration: $config}' \
  "$BASE_CONFIG" > "$MERGED_OUTPUT"

echo " Merged config saved to $MERGED_OUTPUT"
