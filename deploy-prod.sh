#!/bin/bash
# Build and copy the plugin jar to a production server
set -e

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -z "$1" ]; then
    echo "Usage: ./deploy-prod.sh <user@host:/path/to/server/plugins/>"
    echo ""
    echo "Example: ./deploy-prod.sh mc@myserver.com:/opt/minecraft/plugins/"
    exit 1
fi

REMOTE_PATH="$1"

echo "Building plugin..."
cd "$SCRIPT_DIR"
./gradlew build

JAR_PATH=$(ls -t build/libs/minecraft-ai-builder-*.jar | head -1)
echo "Deploying $JAR_PATH to $REMOTE_PATH..."
scp "$JAR_PATH" "$REMOTE_PATH"
echo ""
echo "Deployed! Restart your Minecraft server or use a plugin reload tool."
