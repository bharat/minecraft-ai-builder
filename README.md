# Minecraft AI Builder

A Paper MC plugin that uses AI (OpenAI GPT-4o) to build structures in Minecraft from natural language commands.

Say **"AI, build me a house"** in chat and watch it appear block by block.

## Features

- **Natural language building** - describe what you want and AI generates the structure
- **Multi-turn conversations** - AI asks clarifying questions (size, material, style) before building
- **Chat trigger** - just say `AI, <request>` in chat, no command needed
- **Command interface** - `/ai build me a cozy wooden cottage`
- **Animated building** - blocks are placed gradually for visual effect
- **Configurable** - API key, model, build speed, max blocks, all adjustable in-game

## Quick Start

### Prerequisites

- Java 21 (`brew install openjdk@21` on macOS)
- An [OpenAI API key](https://platform.openai.com/api-keys)
- Minecraft Java Edition

### Setup

```bash
# Clone the repo
git clone https://github.com/YOUR_USERNAME/minecraft-ai-builder.git
cd minecraft-ai-builder

# Download Paper MC and set up local server
./setup-server.sh

# Build the plugin and start the server
./dev.sh
```

### Configure the API Key

Once the server is running, connect with Minecraft and run:
```
/aiconfig apikey sk-your-openai-api-key-here
```

Or edit `server/plugins/AIBuilder/config.yml` directly.

### Usage

In-game, you can use either:
- **Chat:** `AI, build me a small stone house with a garden`
- **Command:** `/ai build me a wooden cabin`

The AI may ask clarifying questions before building:
```
You: AI, build me a house
AI: What style would you like? (wooden cottage, stone castle, modern, etc.)
    And what size - small (7x7), medium (11x11), or large (15x15)?
You: AI, medium wooden cottage please
AI: Building: Medium wooden cottage with oak planks... (847 blocks)
```

Other commands:
- `/ai cancel` - stop an in-progress build
- `/ai clear` - reset the conversation
- `/aiconfig apikey <key>` - set OpenAI API key
- `/aiconfig model <model>` - change AI model (default: gpt-4o)
- `/aiconfig speed <blocks-per-tick>` - adjust build speed
- `/aiconfig maxblocks <n>` - set maximum blocks per build

## Development

### Project Structure

```
minecraft-ai-builder/
├── src/main/java/com/aibuilder/
│   ├── AIBuilderPlugin.java      # Main plugin class
│   ├── AIService.java            # OpenAI API integration
│   ├── AIResponse.java           # Response model
│   ├── AICommand.java            # /ai command handler
│   ├── AIConfigCommand.java      # /aiconfig command handler
│   ├── ChatListener.java         # "AI," chat trigger
│   ├── BuildEngine.java          # Block placement engine
│   ├── ConversationManager.java  # Multi-turn conversation tracking
│   ├── BlockPlacement.java       # Block data model
│   └── ConversationMessage.java  # Chat message model
├── src/main/resources/
│   ├── plugin.yml                # Plugin metadata
│   └── config.yml                # Default configuration
├── server/                       # Local dev server (gitignored)
├── build.gradle.kts              # Gradle build config
├── setup-server.sh               # One-time server setup
├── dev.sh                        # Build + run dev server
└── deploy-prod.sh                # Deploy to production
```

### Dev Workflow

```bash
# Make code changes, then:
./dev.sh                    # Rebuild + restart server

# Or just rebuild without restarting:
./gradlew deploy            # Builds jar and copies to server/plugins/
                            # Then use /reload confirm in-game (or restart)
```

### Deploy to Production

```bash
./deploy-prod.sh user@myserver.com:/opt/minecraft/plugins/
```

This builds the plugin and `scp`s the jar to your production server. Restart the server or use a plugin manager to reload.

## Configuration

All settings are in `server/plugins/AIBuilder/config.yml`:

| Setting | Default | Description |
|---------|---------|-------------|
| `openai-api-key` | (none) | Your OpenAI API key |
| `openai-model` | `gpt-4o` | AI model to use |
| `max-blocks` | `10000` | Max blocks per build |
| `blocks-per-tick` | `50` | Build animation speed |
| `chat-trigger-enabled` | `true` | Enable "AI," chat trigger |
| `chat-trigger-prefix` | `AI,` | Chat trigger prefix |

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `aibuilder.use` | op | Use /ai and chat trigger |
| `aibuilder.admin` | op | Use /aiconfig |

## License

MIT
