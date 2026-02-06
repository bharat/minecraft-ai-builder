#!/bin/bash
# Downloads Paper MC and sets up the local dev server
set -e

SERVER_DIR="$(dirname "$0")/server"
PAPER_VERSION="1.21.4"
PAPER_BUILD="192"
PAPER_JAR="paper-${PAPER_VERSION}-${PAPER_BUILD}.jar"
PAPER_URL="https://api.papermc.io/v2/projects/paper/versions/${PAPER_VERSION}/builds/${PAPER_BUILD}/downloads/${PAPER_JAR}"

mkdir -p "$SERVER_DIR/plugins"

# Download Paper if not present
if [ ! -f "$SERVER_DIR/paper.jar" ]; then
    echo "Downloading Paper MC ${PAPER_VERSION} build ${PAPER_BUILD}..."
    curl -L -o "$SERVER_DIR/paper.jar" "$PAPER_URL"
    echo "Download complete!"
else
    echo "Paper jar already exists, skipping download."
fi

# Accept EULA
echo "eula=true" > "$SERVER_DIR/eula.txt"

# Create server.properties for local dev (small world, creative mode, etc.)
if [ ! -f "$SERVER_DIR/server.properties" ]; then
    cat > "$SERVER_DIR/server.properties" << 'EOF'
# Local dev server settings
server-port=25565
gamemode=creative
difficulty=peaceful
max-players=5
level-name=dev-world
motd=AI Builder Dev Server
online-mode=false
spawn-protection=0
view-distance=10
simulation-distance=5
level-type=minecraft\:flat
generate-structures=false
allow-flight=true
EOF
    echo "Created server.properties for local dev."
fi

echo ""
echo "Server setup complete!"
echo "Run './dev.sh' to build the plugin and start the server."
