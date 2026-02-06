#!/bin/bash
# Build the plugin and start the local dev server
set -e

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"

# Check if Paper jar exists
if [ ! -f "$SERVER_DIR/paper.jar" ]; then
    echo "Paper jar not found. Running setup first..."
    bash "$SCRIPT_DIR/setup-server.sh"
fi

# Build the plugin and deploy to server
echo "Building plugin..."
cd "$SCRIPT_DIR"
./gradlew deploy
echo "Plugin deployed to server/plugins/"

# Start the server
echo ""
echo "Starting Minecraft server..."
echo "Use '/ai build me a house' in-game to test."
echo "Press Ctrl+C to stop."
echo ""
cd "$SERVER_DIR"
java -Xmx2G -Xms1G -jar paper.jar --nogui
