#!/bin/bash
# Build the plugin and start the local dev server
set -e

if [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
    export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
elif [ -d "/usr/lib/jvm/msopenjdk-current" ]; then
    export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/msopenjdk-current}"
fi
export PATH="$JAVA_HOME/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"

# Load secrets from .env file if it exists
if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
    echo "Loaded environment from .env"
else
    echo "No .env file found. Copy .env.example to .env and add your API keys."
fi

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
java -Xmx512M -Xms512M -jar paper.jar --nogui
